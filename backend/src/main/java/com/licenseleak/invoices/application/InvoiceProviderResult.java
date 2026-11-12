package com.licenseleak.invoices.application;

import com.licenseleak.invoices.domain.InvoiceStatus;

public record InvoiceProviderResult(String providerInvoiceId, InvoiceStatus status, String invoiceUrl) {
}
