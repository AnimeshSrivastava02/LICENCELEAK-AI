package com.licenseleak.invoices.application;

import org.springframework.http.HttpHeaders;

public interface WebhookVerifier {
    boolean verify(HttpHeaders headers, String rawBody);
}
