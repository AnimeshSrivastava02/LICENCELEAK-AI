package com.licenseleak.invoices.infrastructure;

import tools.jackson.databind.JsonNode;
import com.licenseleak.invoices.application.InvoiceProvider;
import com.licenseleak.invoices.application.InvoiceProviderException;
import com.licenseleak.invoices.application.InvoiceProviderResult;
import com.licenseleak.invoices.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "licenseleak.invoice.provider", havingValue = "paypal")
public class PaypalInvoiceProvider implements InvoiceProvider {

    private static final Logger log = LoggerFactory.getLogger(PaypalInvoiceProvider.class);
    private static final Pattern INVOICE_ID_IN_LINK = Pattern.compile(
            "/v2/invoicing/invoices/([^/?#]+)");

    private final PaypalRestClient paypal;
    private final String recipientEmail;
    private final String invoicerEmail;

    public PaypalInvoiceProvider(PaypalRestClient paypal,
            @Value("${PAYPAL_INVOICE_RECIPIENT_EMAIL:}") String recipientEmail,
            @Value("${PAYPAL_INVOICER_EMAIL:}") String invoicerEmail) {
        this.paypal = paypal;
        this.recipientEmail = recipientEmail;
        this.invoicerEmail = invoicerEmail;
    }

    @Override
    public InvoiceProviderResult createAndSend(String caseId, BigDecimal amount, String currency, int billableDays,
            String idempotencyKey) {
        if (recipientEmail.isBlank()) {
            throw new InvoiceProviderException("Sandbox invoice recipient email is not configured", null, false);
        }

        String token;
        try {
            token = paypal.accessToken();
        } catch (RuntimeException exception) {
            throw new InvoiceProviderException("PayPal authentication failed", null, false);
        }

        Map<String, Object> request = Map.of(
                "detail", Map.of("currency_code", currency,
                        "note", "Fictional LicenseLeak AI demo. Fee calculated at the agreed daily rate."),
                "invoicer", invoicerEmail.isBlank()
                        ? Map.of("name", Map.of("given_name", "Fictional", "surname", "Creator"))
                        : Map.of("email_address", invoicerEmail),
                "primary_recipients", List.of(Map.of("billing_info", Map.of("email_address", recipientEmail))),
                "items", List.of(Map.of(
                        "name", "Fictional ad licence fee",
                        "description", "Case " + caseId + ": " + billableDays + " out-of-contract calendar day(s)",
                        "quantity", "1",
                        "unit_amount", Map.of("currency_code", currency, "value", amount.toPlainString()))));

        JsonNode created;
        try {
            created = paypal.restClient().post()
                    .uri("/v2/invoicing/invoices")
                    .headers(headers -> {
                        headers.setBearerAuth(token);
                        headers.set("PayPal-Request-Id", idempotencyKey);
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            boolean uncertain = exception.getStatusCode().is5xxServerError();
            throw new InvoiceProviderException("PayPal could not confirm invoice creation", null, uncertain);
        } catch (ResourceAccessException exception) {
            throw new InvoiceProviderException("PayPal invoice creation timed out; result is uncertain", null, true);
        }

        String invoiceId = extractInvoiceId(created);
        if (invoiceId.isBlank()) {
            log.error("PayPal created an invoice response without an invoice ID or invoice link for case {}.", caseId);
            throw new InvoiceProviderException("PayPal returned no invoice identifier", null, true);
        }
        log.info("PayPal draft created for case {}; proceeding to send after approval.", caseId);
        String invoiceUrl = null;
        for (JsonNode link : responseLinks(created)) {
            if ("payer-view".equals(link.path("rel").asText())) {
                invoiceUrl = link.path("href").asText(null);
                break;
            }
        }

        return sendDraft(caseId, invoiceId, invoiceUrl, token);
    }

    @Override
    public InvoiceProviderResult reconcileAndSend(String caseId, String invoiceId) {
        String token;
        try {
            token = paypal.accessToken();
        } catch (RuntimeException exception) {
            throw new InvoiceProviderException("PayPal authentication failed", invoiceId, false);
        }

        JsonNode current;
        try {
            current = paypal.restClient().get()
                    .uri("/v2/invoicing/invoices/{invoiceId}", invoiceId)
                    .headers(headers -> {
                        headers.setBearerAuth(token);
                        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    })
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            logPayPalError("Could not check the existing PayPal draft", caseId, exception);
            throw new InvoiceProviderException("PayPal could not confirm the existing invoice status",
                    invoiceId, exception.getStatusCode().is5xxServerError());
        } catch (ResourceAccessException exception) {
            throw new InvoiceProviderException("Checking the existing PayPal invoice timed out",
                    invoiceId, true);
        }

        String status = current == null ? "" : current.path("status").asText("");
        if ("SENT".equalsIgnoreCase(status) || "UNPAID".equalsIgnoreCase(status)) {
            return new InvoiceProviderResult(invoiceId, InvoiceStatus.SENT, payerViewUrl(current));
        }
        if ("PAID".equalsIgnoreCase(status)) {
            return new InvoiceProviderResult(invoiceId, InvoiceStatus.PAID, payerViewUrl(current));
        }
        if (!"DRAFT".equalsIgnoreCase(status)) {
            throw new InvoiceProviderException("PayPal invoice is not in a sendable draft state",
                    invoiceId, false);
        }
        return sendDraft(caseId, invoiceId, payerViewUrl(current), token);
    }

    private InvoiceProviderResult sendDraft(String caseId, String invoiceId, String invoiceUrl, String token) {
        try {
            paypal.restClient().post()
                    .uri("/v2/invoicing/invoices/{invoiceId}/send", invoiceId)
                    .headers(headers -> {
                        headers.setBearerAuth(token);
                        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("send_to_recipient", true, "send_to_invoicer", false))
                    .retrieve()
                    .toBodilessEntity();
            log.info("PayPal confirmed the invoice send for case {}.", caseId);
            return new InvoiceProviderResult(invoiceId, InvoiceStatus.SENT, invoiceUrl);
        } catch (RestClientResponseException exception) {
            logPayPalError("PayPal rejected the invoice send", caseId, exception);
            throw new InvoiceProviderException("PayPal rejected the invoice send", invoiceId,
                    exception.getStatusCode().is5xxServerError());
        } catch (ResourceAccessException exception) {
            log.error("PayPal send result is uncertain for case {} (network timeout).", caseId);
            throw new InvoiceProviderException("PayPal could not confirm invoice sending", invoiceId, true);
        } catch (RuntimeException exception) {
            log.error("PayPal send failed unexpectedly for case {} ({}).",
                    caseId, exception.getClass().getSimpleName());
            throw new InvoiceProviderException("PayPal could not confirm invoice sending", invoiceId, true);
        }
    }

    private void logPayPalError(String operation, String caseId, RestClientResponseException exception) {
        JsonNode error = null;
        try {
            error = new tools.jackson.databind.ObjectMapper().readTree(exception.getResponseBodyAsString());
        } catch (Exception ignored) {
            // The status code is still useful when PayPal returns a non-JSON response.
        }
        String name = error == null ? "" : error.path("name").asText("");
        String field = "";
        String issue = "";
        if (error != null && error.path("details").isArray() && error.path("details").size() > 0) {
            JsonNode firstDetail = error.path("details").get(0);
            field = firstDetail.path("field").asText("");
            issue = firstDetail.path("issue").asText("");
        }
        log.error("{} for case {}: HTTP {}, PayPal error {}, field {}, issue {}.",
                operation, caseId, exception.getStatusCode().value(), name, field, issue);
    }

    private String payerViewUrl(JsonNode invoice) {
        if (invoice == null) {
            return null;
        }
        String recipientViewUrl = invoice.path("metadata").path("recipient_view_url").asText("");
        if (!recipientViewUrl.isBlank()) {
            return recipientViewUrl;
        }
        for (JsonNode link : responseLinks(invoice)) {
            if ("payer-view".equals(link.path("rel").asText())) {
                return link.path("href").asText(null);
            }
        }
        return null;
    }

    /**
     * PayPal normally returns an invoice object's top-level `id`. Some responses
     * expose the resource only through a HATEOAS link, so use the invoice URL as
     * a safe fallback rather than treating a successfully-created draft as lost.
     */
    private String extractInvoiceId(JsonNode response) {
        if (response == null) {
            return "";
        }
        String id = response.path("id").asText("");
        if (!id.isBlank()) {
            return id;
        }
        for (JsonNode link : responseLinks(response)) {
            String href = link.path("href").asText("");
            Matcher matcher = INVOICE_ID_IN_LINK.matcher(href);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    private List<JsonNode> responseLinks(JsonNode response) {
        List<JsonNode> links = new ArrayList<>();
        if (response == null) {
            return links;
        }
        JsonNode nestedLinks = response.path("links");
        if (nestedLinks.isArray()) {
            nestedLinks.forEach(links::add);
        }
        // The developer dashboard may expose a single HATEOAS link as the
        // response object itself instead of showing the enclosing invoice.
        if (response.has("href") && response.has("rel")) {
            links.add(response);
        }
        return links;
    }

    @Override
    public String providerName() { return "PAYPAL"; }
}
