package com.licenseleak.invoices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;
import com.licenseleak.audit.application.AuditService;
import com.licenseleak.invoices.application.PaypalWebhookService;
import com.licenseleak.invoices.application.WebhookVerifier;
import com.licenseleak.invoices.domain.InvoiceRecordEntity;
import com.licenseleak.invoices.domain.InvoiceStatus;
import com.licenseleak.invoices.infrastructure.InvoiceRecordRepository;
import com.licenseleak.invoices.infrastructure.PaypalWebhookEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PaypalWebhookServiceTest {

    @Test
    void onlyVerifiedPaidEventMarksInvoicePaid() {
        var verifier = mock(WebhookVerifier.class);
        var events = mock(PaypalWebhookEventRepository.class);
        var invoices = mock(InvoiceRecordRepository.class);
        var audit = mock(AuditService.class);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(new SimpleTransactionStatus());
        when(verifier.verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        when(events.existsById("event-1")).thenReturn(false);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var invoice = new InvoiceRecordEntity("record", "case-demo-001", "decision", "quote", "PAYPAL",
                "INV-1", InvoiceStatus.SENT, "key", null, now, now);
        when(invoices.findByProviderInvoiceId("INV-1")).thenReturn(Optional.of(invoice));
        var service = new PaypalWebhookService(verifier, new ObjectMapper(), events, invoices, audit, transactions);

        var result = service.receive(new HttpHeaders(), "{\"id\":\"event-1\",\"event_type\":\"INVOICING.INVOICE.PAID\",\"resource\":{\"id\":\"INV-1\"}}");

        assertEquals("PAID", result.result());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        verify(invoices).save(invoice);
    }

    @Test
    void createdEventDoesNotMarkInvoiceAsPaid() {
        var verifier = mock(WebhookVerifier.class);
        var events = mock(PaypalWebhookEventRepository.class);
        var invoices = mock(InvoiceRecordRepository.class);
        var audit = mock(AuditService.class);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(new SimpleTransactionStatus());
        when(verifier.verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        when(events.existsById("event-2")).thenReturn(false);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var invoice = new InvoiceRecordEntity("record", "case-demo-001", "decision", "quote", "PAYPAL",
                "INV-2", InvoiceStatus.CREATING, "key", null, now, now);
        when(invoices.findByProviderInvoiceId("INV-2")).thenReturn(Optional.of(invoice));
        var service = new PaypalWebhookService(verifier, new ObjectMapper(), events, invoices, audit, transactions);

        var result = service.receive(new HttpHeaders(), "{\"id\":\"event-2\",\"event_type\":\"INVOICING.INVOICE.CREATED\",\"resource\":{\"id\":\"INV-2\"}}");

        assertEquals("EVENT_RECORDED", result.result());
        assertEquals(InvoiceStatus.CREATING, invoice.getStatus());
        verify(invoices, never()).save(invoice);
    }
}
