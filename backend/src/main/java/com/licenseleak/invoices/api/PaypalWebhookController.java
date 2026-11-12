package com.licenseleak.invoices.api;

import com.licenseleak.invoices.application.PaypalWebhookService;
import com.licenseleak.invoices.application.PaypalWebhookService.WebhookResult;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/paypal")
public class PaypalWebhookController {

    private final PaypalWebhookService webhookService;

    public PaypalWebhookController(PaypalWebhookService webhookService) { this.webhookService = webhookService; }

    @PostMapping
    public WebhookResult receive(@RequestHeader HttpHeaders headers, @RequestBody String rawBody) {
        return webhookService.receive(headers, rawBody);
    }
}
