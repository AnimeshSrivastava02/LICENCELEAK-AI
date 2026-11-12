package com.licenseleak.invoices.application;

public class InvoiceProviderException extends RuntimeException {
    private final String knownProviderInvoiceId;
    private final boolean uncertain;

    public InvoiceProviderException(String message, String knownProviderInvoiceId, boolean uncertain) {
        super(message);
        this.knownProviderInvoiceId = knownProviderInvoiceId;
        this.uncertain = uncertain;
    }

    public String getKnownProviderInvoiceId() { return knownProviderInvoiceId; }
    public boolean isUncertain() { return uncertain; }
}
