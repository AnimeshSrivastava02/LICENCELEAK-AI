package com.licenseleak.invoices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.licenseleak.analysis.infrastructure.AnalysisRunRepository;
import com.licenseleak.approvals.infrastructure.CreatorDecisionRepository;
import com.licenseleak.audit.application.AuditService;
import com.licenseleak.cases.infrastructure.DemoCaseRepository;
import com.licenseleak.common.errors.ConflictException;
import com.licenseleak.fees.infrastructure.FeeQuoteRepository;
import com.licenseleak.invoices.application.InvoiceProvider;
import com.licenseleak.invoices.application.InvoiceService;
import com.licenseleak.invoices.application.PaypalWebhookService;
import com.licenseleak.invoices.domain.InvoiceRecordEntity;
import com.licenseleak.invoices.domain.InvoiceStatus;
import com.licenseleak.invoices.infrastructure.InvoiceRecordRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class InvoiceServiceTest {

    @Test
    void requiresApprovalBeforeCallingProvider() {
        var cases = mock(DemoCaseRepository.class);
        var analyses = mock(AnalysisRunRepository.class);
        var decisions = mock(CreatorDecisionRepository.class);
        var quotes = mock(FeeQuoteRepository.class);
        var invoices = mock(InvoiceRecordRepository.class);
        var provider = mock(InvoiceProvider.class);
        var webhooks = mock(PaypalWebhookService.class);
        var audit = mock(AuditService.class);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(mock(org.springframework.transaction.TransactionStatus.class));
        when(cases.existsById("case-demo-001")).thenReturn(true);
        when(invoices.findByCaseId("case-demo-001")).thenReturn(Optional.empty());

        var service = new InvoiceService(cases, analyses, decisions, quotes, invoices, provider, audit, webhooks, transactions);

        var error = assertThrows(ConflictException.class, () -> service.create("case-demo-001"));

        assertEquals("APPROVAL_REQUIRED", error.getCode());
        verify(provider, never()).createAndSend(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void returnsExistingInvoiceWithoutCreatingADuplicate() {
        var cases = mock(DemoCaseRepository.class);
        var analyses = mock(AnalysisRunRepository.class);
        var decisions = mock(CreatorDecisionRepository.class);
        var quotes = mock(FeeQuoteRepository.class);
        var invoices = mock(InvoiceRecordRepository.class);
        var provider = mock(InvoiceProvider.class);
        var webhooks = mock(PaypalWebhookService.class);
        var audit = mock(AuditService.class);
        var transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(mock(org.springframework.transaction.TransactionStatus.class));
        var now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);
        var invoice = new InvoiceRecordEntity("record-id", "case-demo-001", "decision-id", "quote-id",
                "MOCK", "MOCK-EXISTING", InvoiceStatus.DRAFT, "idempotency-key", null, now, now);
        when(cases.existsById("case-demo-001")).thenReturn(true);
        when(invoices.findByCaseId("case-demo-001")).thenReturn(Optional.of(invoice));
        var quote = new com.licenseleak.fees.domain.FeeQuoteEntity("quote-id", "analysis-id",
                new java.math.BigDecimal("30.00"), "USD", new java.math.BigDecimal("10.00"), 3, true,
                "DAILY_RATE_PER_BILLABLE_DAY", now, java.util.List.of());
        when(quotes.findById("quote-id")).thenReturn(Optional.of(quote));

        var service = new InvoiceService(cases, analyses, decisions, quotes, invoices, provider, audit, webhooks, transactions);
        var result = service.create("case-demo-001");

        assertEquals("MOCK-EXISTING", result.providerInvoiceId());
        verify(provider, never()).createAndSend(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }
}
