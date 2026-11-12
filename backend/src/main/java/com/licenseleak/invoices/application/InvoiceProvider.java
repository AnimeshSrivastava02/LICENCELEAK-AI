package com.licenseleak.invoices.application;

import java.math.BigDecimal;

public interface InvoiceProvider {
    InvoiceProviderResult createAndSend(String caseId, BigDecimal amount, String currency, int billableDays,
            String idempotencyKey);

    default InvoiceProviderResult reconcileAndSend(String caseId, String providerInvoiceId) {
        throw new UnsupportedOperationException("This invoice provider cannot reconcile an existing invoice");
    }

    String providerName();
}
