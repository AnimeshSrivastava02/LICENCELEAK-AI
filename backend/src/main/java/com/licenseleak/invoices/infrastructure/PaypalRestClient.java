package com.licenseleak.invoices.infrastructure;

import tools.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Component
public class PaypalRestClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public PaypalRestClient(@Value("${PAYPAL_BASE_URL:https://api-m.sandbox.paypal.com}") String baseUrl,
            @Value("${PAYPAL_CLIENT_ID:}") String clientId,
            @Value("${PAYPAL_CLIENT_SECRET:}") String clientSecret) {
        if (!"api-m.sandbox.paypal.com".equalsIgnoreCase(URI.create(baseUrl).getHost())) {
            throw new IllegalArgumentException("This demo only supports the PayPal Sandbox API host");
        }
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public RestClient restClient() { return restClient; }

    public String accessToken() {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("PayPal client credentials are not configured");
        }
        JsonNode token = restClient.post()
                .uri("/v1/oauth2/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(JsonNode.class);
        String accessToken = token == null ? "" : token.path("access_token").asText();
        if (accessToken.isBlank()) {
            throw new IllegalStateException("PayPal did not return an access token");
        }
        return accessToken;
    }
}
