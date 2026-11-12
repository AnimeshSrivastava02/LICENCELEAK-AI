package com.licenseleak.invoices.application;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.licenseleak.audit.application.AuditService;
import com.licenseleak.invoices.domain.InvoiceRecordEntity;
import com.licenseleak.invoices.domain.InvoiceStatus;
import com.licenseleak.invoices.domain.PaypalWebhookEventEntity;
import com.licenseleak.invoices.infrastructure.InvoiceRecordRepository;
import com.licenseleak.invoices.infrastructure.PaypalWebhookEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpHeaders;

@Service
public class PaypalWebhookService {

    private final WebhookVerifier verifier;
    private final ObjectMapper objectMapper;
    private final PaypalWebhookEventRepository eventRepository;
    private final InvoiceRecordRepository invoiceRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public PaypalWebhookService(WebhookVerifier verifier, ObjectMapper objectMapper,
            PaypalWebhookEventRepository eventRepository, InvoiceRecordRepository invoiceRepository,
            AuditService auditService, PlatformTransactionManager transactionManager) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
        this.eventRepository = eventRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public WebhookResult receive(HttpHeaders headers, String rawBody) {
        if (!verifier.verify(headers, rawBody)) {
            throw new InvalidWebhookException("PayPal webhook verification failed.");
        }
        JsonNode event;
        try {
            event = objectMapper.readTree(rawBody);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new InvalidWebhookException("PayPal webhook payload is not valid JSON.");
        }
        String eventId = event.path("id").asText();
        String eventType = event.path("event_type").asText();
        JsonNode resource = event.path("resource");
        String providerInvoiceId = resource.path("id").asText();
        String providerStatus = resource.path("status").asText();
        if (resource.has("invoice")) {
            if (providerInvoiceId.isBlank()) {
                providerInvoiceId = resource.path("invoice").path("id").asText();
            }
            if (providerStatus.isBlank()) {
                providerStatus = resource.path("invoice").path("status").asText();
            }
        }
        if (eventId.isBlank() || eventType.isBlank()) {
            throw new InvalidWebhookException("PayPal webhook is missing its event identifier or type.");
        }
        String finalProviderInvoiceId = providerInvoiceId;
        String finalProviderStatus = providerStatus;
        return transactionTemplate.execute(status -> processVerifiedEvent(eventId, eventType,
                finalProviderInvoiceId, finalProviderStatus));
    }

    private WebhookResult processVerifiedEvent(String eventId, String eventType, String providerInvoiceId,
            String providerStatus) {
        var existingEvent = eventRepository.findById(eventId);
        if (existingEvent.isPresent()) {
            PaypalWebhookEventEntity savedEvent = existingEvent.get();
            String savedInvoiceId = savedEvent.getProviderInvoiceId();
            if (savedInvoiceId == null || savedInvoiceId.isBlank()) {
                savedInvoiceId = providerInvoiceId;
            }
            String savedEventType = savedEvent.getEventType();
            String savedProviderStatus = savedEvent.getProviderStatus();
            if (savedProviderStatus == null || savedProviderStatus.isBlank()) {
                savedProviderStatus = providerStatus;
            }
            InvoiceRecordEntity invoice = savedInvoiceId == null || savedInvoiceId.isBlank() ? null
                    : invoiceRepository.findByProviderInvoiceId(savedInvoiceId).orElse(null);
            InvoiceStatus nextStatus = mapStatus(savedEventType, savedProviderStatus);
            if (invoice == null || nextStatus == null) {
                return new WebhookResult(true, true, "DUPLICATE_IGNORED");
            }
            if (invoice.getStatus() != nextStatus && canTransition(invoice.getStatus(), nextStatus)) {
                invoice.updateStatus(nextStatus, OffsetDateTime.now(ZoneOffset.UTC));
                invoiceRepository.save(invoice);
                auditService.record(invoice.getCaseId(), "PAYPAL_INVOICE_STATUS", eventId,
                        Map.of("eventType", savedEventType, "status", nextStatus.name(),
                                "providerInvoiceId", savedInvoiceId));
            }
            savedEvent.markProcessed();
            eventRepository.save(savedEvent);
            return new WebhookResult(true, true, invoice.getStatus().name());
        }

        InvoiceRecordEntity invoice = providerInvoiceId.isBlank() ? null
                : invoiceRepository.findByProviderInvoiceId(providerInvoiceId).orElse(null);
        InvoiceStatus nextStatus = mapStatus(eventType, providerStatus);
        boolean processed = invoice == null ? nextStatus == null : true;
        eventRepository.save(new PaypalWebhookEventEntity(
                eventId, eventType, providerInvoiceId.isBlank() ? null : providerInvoiceId, providerStatus,
                processed, OffsetDateTime.now(ZoneOffset.UTC)));
        if (invoice != null && nextStatus != null && canTransition(invoice.getStatus(), nextStatus)) {
            invoice.updateStatus(nextStatus, OffsetDateTime.now(ZoneOffset.UTC));
            invoiceRepository.save(invoice);
            auditService.record(invoice.getCaseId(), "PAYPAL_INVOICE_STATUS", eventId,
                    Map.of("eventType", eventType, "status", nextStatus.name(), "providerInvoiceId", providerInvoiceId));
        }
        return new WebhookResult(true, false, nextStatus == null ? "EVENT_RECORDED" : nextStatus.name());
    }

    @Transactional
    public void replayPending(String providerInvoiceId) {
        InvoiceRecordEntity invoice = invoiceRepository.findByProviderInvoiceId(providerInvoiceId).orElse(null);
        if (invoice == null) return;
        for (PaypalWebhookEventEntity event : eventRepository
                .findAllByProviderInvoiceIdAndProcessedFalseOrderByReceivedAtAsc(providerInvoiceId)) {
            InvoiceStatus nextStatus = mapStatus(event.getEventType(), event.getProviderStatus());
            if (nextStatus != null && canTransition(invoice.getStatus(), nextStatus)) {
                invoice.updateStatus(nextStatus, OffsetDateTime.now(ZoneOffset.UTC));
                invoiceRepository.save(invoice);
                auditService.record(invoice.getCaseId(), "PAYPAL_INVOICE_STATUS", event.getEventId(),
                        Map.of("eventType", event.getEventType(), "status", nextStatus.name(),
                                "providerInvoiceId", providerInvoiceId));
            }
            event.markProcessed();
            eventRepository.save(event);
        }
    }

    private InvoiceStatus mapStatus(String eventType, String providerStatus) {
        return switch (eventType) {
            case "INVOICING.INVOICE.SENT" -> InvoiceStatus.SENT;
            case "INVOICING.INVOICE.PAID" -> providerStatus == null || providerStatus.isBlank()
                    || "PAID".equalsIgnoreCase(providerStatus)
                    || "MARKED_AS_PAID".equalsIgnoreCase(providerStatus)
                    ? InvoiceStatus.PAID : null;
            case "INVOICING.INVOICE.CANCELLED" -> InvoiceStatus.CANCELLED;
            default -> null;
        };
    }

    private boolean canTransition(InvoiceStatus current, InvoiceStatus next) {
        if (current == InvoiceStatus.PAID) {
            return false;
        }
        if (next == InvoiceStatus.PAID) {
            return true;
        }
        return current != InvoiceStatus.CANCELLED && current != InvoiceStatus.FAILED;
    }

    public record WebhookResult(boolean accepted, boolean duplicate, String result) {
    }
}
