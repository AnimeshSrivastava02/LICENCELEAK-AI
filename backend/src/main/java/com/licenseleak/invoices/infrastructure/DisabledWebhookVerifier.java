package com.licenseleak.invoices.infrastructure;

import com.licenseleak.invoices.application.WebhookVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(name = "licenseleak.invoice.provider", havingValue = "mock", matchIfMissing = true)
public class DisabledWebhookVerifier implements WebhookVerifier {
    @Override
    public boolean verify(HttpHeaders headers, String rawBody) { return false; }
}
