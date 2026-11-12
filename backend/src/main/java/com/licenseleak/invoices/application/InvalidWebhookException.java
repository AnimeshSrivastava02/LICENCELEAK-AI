package com.licenseleak.invoices.application;

public class InvalidWebhookException extends RuntimeException {
    public InvalidWebhookException(String message) { super(message); }
}
