package com.licenseleak.invoices.application;

import com.licenseleak.analysis.domain.AnalysisRunEntity;
import com.licenseleak.analysis.infrastructure.AnalysisRunRepository;
import com.licenseleak.approvals.domain.CreatorDecisionEntity;
import com.licenseleak.approvals.domain.DecisionType;
import com.licenseleak.approvals.infrastructure.CreatorDecisionRepository;
import com.licenseleak.audit.application.AuditService;
import com.licenseleak.cases.application.CaseNotFoundException;
import com.licenseleak.cases.infrastructure.DemoCaseRepository;
import com.licenseleak.common.errors.ConflictException;
import com.licenseleak.fees.domain.FeeQuoteEntity;
import com.licenseleak.fees.infrastructure.FeeQuoteRepository;
import com.licenseleak.invoices.api.InvoiceResponse;
import com.licenseleak.invoices.domain.InvoiceRecordEntity;
import com.licenseleak.invoices.domain.InvoiceStatus;
import com.licenseleak.invoices.infrastructure.InvoiceRecordRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final DemoCaseRepository caseRepository;
    private final AnalysisRunRepository analysisRepository;
    private final CreatorDecisionRepository decisionRepository;
    private final FeeQuoteRepository quoteRepository;
    private final InvoiceRecordRepository invoiceRepository;
    private final InvoiceProvider provider;
    private final PaypalWebhookService webhookService;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public InvoiceService(DemoCaseRepository caseRepository, AnalysisRunRepository analysisRepository,
            CreatorDecisionRepository decisionRepository, FeeQuoteRepository quoteRepository,
            InvoiceRecordRepository invoiceRepository, InvoiceProvider provider, AuditService auditService,
            PaypalWebhookService webhookService, PlatformTransactionManager transactionManager) {
        this.caseRepository = caseRepository;
        this.analysisRepository = analysisRepository;
        this.decisionRepository = decisionRepository;
        this.quoteRepository = quoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.provider = provider;
        this.auditService = auditService;
        this.webhookService = webhookService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public InvoiceResponse create(String caseId) {
        InvoiceRecordEntity pending = transactionTemplate.execute(status -> prepare(caseId));
        if (pending == null) {
            throw new IllegalStateException("Could not prepare invoice creation");
        }
        boolean reconcilePayPal = "PAYPAL".equals(pending.getProviderName())
                && pending.getProviderInvoiceId() != null
                && (pending.getStatus() == InvoiceStatus.UNKNOWN || pending.getStatus() == InvoiceStatus.FAILED);
        if (pending.getStatus() != InvoiceStatus.CREATING && !reconcilePayPal) {
            return InvoiceResponse.from(pending, quoteRepository.findById(pending.getFeeQuoteId()).orElseThrow());
        }

        try {
            InvoiceProviderResult providerResult = reconcilePayPal
                    ? provider.reconcileAndSend(caseId, pending.getProviderInvoiceId())
                    : provider.createAndSend(caseId,
                            quoteRepository.findById(pending.getFeeQuoteId()).orElseThrow().getAmount(),
                            quoteRepository.findById(pending.getFeeQuoteId()).orElseThrow().getCurrency(),
                            quoteRepository.findById(pending.getFeeQuoteId()).orElseThrow().getTotalBillableDays(),
                            pending.getIdempotencyKey());
            InvoiceRecordEntity saved = updateProviderResult(pending.getId(), providerResult.providerInvoiceId(),
                    providerResult.status(), providerResult.invoiceUrl());
            webhookService.replayPending(providerResult.providerInvoiceId());
            saved = invoiceRepository.findById(saved.getId()).orElse(saved);
            return InvoiceResponse.from(saved, quoteRepository.findById(saved.getFeeQuoteId()).orElseThrow());
        } catch (InvoiceProviderException exception) {
            InvoiceStatus status = exception.isUncertain() ? InvoiceStatus.UNKNOWN : InvoiceStatus.FAILED;
            if (exception.isUncertain()) {
                log.warn("Invoice result is uncertain for case {} using provider {} (provider invoice ID known: {}).",
                        caseId, provider.providerName(), exception.getKnownProviderInvoiceId() != null);
            }
            InvoiceRecordEntity saved = updateProviderResult(pending.getId(), exception.getKnownProviderInvoiceId(),
                    status, null);
            if (exception.getKnownProviderInvoiceId() != null) {
                webhookService.replayPending(exception.getKnownProviderInvoiceId());
                saved = invoiceRepository.findById(saved.getId()).orElse(saved);
            }
            auditService.record(caseId, "INVOICE_PROVIDER_RESULT", saved.getId(),
                    Map.of("status", status.name(), "provider", provider.providerName()));
            if (!exception.isUncertain()) {
                throw new InvoiceProviderException("Invoice creation failed. Check the stored invoice status before retrying.",
                        exception.getKnownProviderInvoiceId(), false);
            }
            return InvoiceResponse.from(saved, quoteRepository.findById(saved.getFeeQuoteId()).orElseThrow());
        } catch (RuntimeException exception) {
            log.error("Unexpected invoice provider failure for case {} using provider {} ({}).",
                    caseId, provider.providerName(), exception.getClass().getSimpleName());
            InvoiceRecordEntity saved = updateProviderResult(pending.getId(), null, InvoiceStatus.UNKNOWN, null);
            auditService.record(caseId, "INVOICE_PROVIDER_RESULT", saved.getId(),
                    Map.of("status", "UNKNOWN", "provider", provider.providerName()));
            return InvoiceResponse.from(saved, quoteRepository.findById(saved.getFeeQuoteId()).orElseThrow());
        }
    }

    public InvoiceResponse get(String caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new CaseNotFoundException(caseId);
        }
        InvoiceRecordEntity invoice = invoiceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new InvoiceNotFoundException(caseId));
        FeeQuoteEntity quote = quoteRepository.findById(invoice.getFeeQuoteId()).orElseThrow();
        return InvoiceResponse.from(invoice, quote);
    }

    private InvoiceRecordEntity prepare(String caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new CaseNotFoundException(caseId);
        }
        var existing = invoiceRepository.findByCaseId(caseId);
        if (existing.isPresent()) {
            return existing.get();
        }
        CreatorDecisionEntity decision = decisionRepository.findTopByCaseIdOrderByDecidedAtDesc(caseId)
                .orElseThrow(() -> new ConflictException("APPROVAL_REQUIRED", "A creator decision is required before invoicing."));
        if (decision.getDecision() != DecisionType.APPROVE) {
            throw new ConflictException("APPROVAL_REQUIRED", "The latest creator decision is not an approval." );
        }
        AnalysisRunEntity latest = analysisRepository.findFirstByCaseIdOrderByVersionDesc(caseId)
                .orElseThrow(() -> new ConflictException("ANALYSIS_REQUIRED", "A current analysis is required before invoicing."));
        if (!latest.getId().equals(decision.getAnalysisId())) {
            throw new ConflictException("STALE_APPROVAL", "The approval does not reference the latest analysis." );
        }
        if (decision.getFeeQuoteId() == null) {
            throw new ConflictException("FEE_QUOTE_REQUIRED", "Approval must reference an actionable fee quote." );
        }
        FeeQuoteEntity quote = quoteRepository.findByIdAndAnalysisId(decision.getFeeQuoteId(), latest.getId())
                .orElseThrow(() -> new ConflictException("FEE_QUOTE_REQUIRED", "The approved fee quote is no longer available."));
        if (!quote.isActionable() || quote.getAmount() == null || quote.getAmount().signum() <= 0) {
            throw new ConflictException("FEE_QUOTE_NOT_ACTIONABLE", "A current, positive, actionable fee quote is required." );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String invoiceId = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();
        InvoiceRecordEntity pending = invoiceRepository.save(new InvoiceRecordEntity(
                invoiceId, caseId, decision.getId(), quote.getId(), provider.providerName(), null,
                InvoiceStatus.CREATING, idempotencyKey, null, now, now));
        auditService.record(caseId, "INVOICE_CREATE_REQUESTED", invoiceId,
                Map.of("decisionId", decision.getId(), "feeQuoteId", quote.getId(), "provider", provider.providerName()));
        return pending;
    }

    private InvoiceRecordEntity updateProviderResult(String id, String providerInvoiceId, InvoiceStatus status,
            String invoiceUrl) {
        return transactionTemplate.execute(transactionStatus -> {
            InvoiceRecordEntity invoice = invoiceRepository.findById(id).orElseThrow();
            invoice.updateProviderResult(providerInvoiceId, status, invoiceUrl, OffsetDateTime.now(ZoneOffset.UTC));
            return invoiceRepository.save(invoice);
        });
    }
}
