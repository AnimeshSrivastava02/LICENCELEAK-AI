package com.licenseleak.invoices.application;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(String caseId) { super("No invoice exists for case: " + caseId); }
}
