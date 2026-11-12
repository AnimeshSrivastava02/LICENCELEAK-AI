package com.licenseleak.invoices.api;

import com.licenseleak.fees.domain.FeeQuoteEntity;
import com.licenseleak.invoices.domain.InvoiceRecordEntity;
import java.time.OffsetDateTime;

public record InvoiceResponse(
        String invoiceRecordId,
        String caseId,
        String provider,
        String providerInvoiceId,
        String status,
        String amount,
        String currency,
        String approvalDecisionId,
        String invoiceUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static InvoiceResponse from(InvoiceRecordEntity invoice, FeeQuoteEntity quote) {
        return new InvoiceResponse(invoice.getId(), invoice.getCaseId(), invoice.getProviderName(),
                invoice.getProviderInvoiceId(), invoice.getStatus().name(),
                quote.getAmount() == null ? null : quote.getAmount().toPlainString(), quote.getCurrency(),
                invoice.getDecisionId(), invoice.getInvoiceUrl(), invoice.getCreatedAt(), invoice.getUpdatedAt());
    }
}
