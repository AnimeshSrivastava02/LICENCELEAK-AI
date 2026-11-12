package com.licenseleak.invoices.infrastructure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.licenseleak.invoices.application.WebhookVerifier;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "licenseleak.invoice.provider", havingValue = "paypal")
public class PaypalWebhookVerifier implements WebhookVerifier {

    private final PaypalRestClient paypal;
    private final ObjectMapper objectMapper;
    private final String webhookId;

    public PaypalWebhookVerifier(PaypalRestClient paypal, ObjectMapper objectMapper,
            @Value("${PAYPAL_WEBHOOK_ID:}") String webhookId) {
        this.paypal = paypal;
        this.objectMapper = objectMapper;
        this.webhookId = webhookId;
    }

    @Override
    public boolean verify(HttpHeaders headers, String rawBody) {
        if (webhookId.isBlank()) {
            return false;
        }
        try {
            JsonNode event = objectMapper.readTree(rawBody);
            Map<String, Object> verificationRequest = Map.of(
                    "transmission_id", requiredHeader(headers, "paypal-transmission-id"),
                    "transmission_time", requiredHeader(headers, "paypal-transmission-time"),
                    "cert_url", requiredHeader(headers, "paypal-cert-url"),
                    "auth_algo", requiredHeader(headers, "paypal-auth-algo"),
                    "transmission_sig", requiredHeader(headers, "paypal-transmission-sig"),
                    "webhook_id", webhookId,
                    "webhook_event", event);
            JsonNode response = paypal.restClient().post()
                    .uri("/v1/notifications/verify-webhook-signature")
                    .headers(requestHeaders -> requestHeaders.setBearerAuth(paypal.accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(verificationRequest)
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && "SUCCESS".equals(response.path("verification_status").asText());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String requiredHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing PayPal verification header");
        }
        return value;
    }
}
