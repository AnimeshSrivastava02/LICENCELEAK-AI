package com.licenseleak.invoices.infrastructure;

import com.licenseleak.invoices.application.InvoiceProvider;
import com.licenseleak.invoices.application.InvoiceProviderResult;
import com.licenseleak.invoices.domain.InvoiceStatus;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "licenseleak.invoice.provider", havingValue = "mock", matchIfMissing = true)
public class MockInvoiceProvider implements InvoiceProvider {
    @Override
    public InvoiceProviderResult createAndSend(String caseId, BigDecimal amount, String currency, int billableDays,
            String idempotencyKey) {
        return new InvoiceProviderResult("MOCK-" + idempotencyKey.substring(0, 12), InvoiceStatus.DRAFT, null);
    }

    @Override
    public String providerName() { return "MOCK"; }
}
