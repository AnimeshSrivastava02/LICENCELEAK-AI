package com.licenseleak.invoices.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "paypal_webhook_event")
public class PaypalWebhookEventEntity {
    @Id
    @Column(name = "event_id", length = 120, nullable = false)
    private String eventId;

    @Column(name = "event_type", length = 120, nullable = false)
    private String eventType;

    @Column(name = "provider_invoice_id", length = 120)
    private String providerInvoiceId;

    @Column(name = "provider_status", length = 40)
    private String providerStatus;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    protected PaypalWebhookEventEntity() {
    }

    public PaypalWebhookEventEntity(String eventId, String eventType, String providerInvoiceId,
            String providerStatus, boolean processed, OffsetDateTime receivedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.providerInvoiceId = providerInvoiceId;
        this.providerStatus = providerStatus;
        this.processed = processed;
        this.receivedAt = receivedAt;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getProviderInvoiceId() { return providerInvoiceId; }
    public String getProviderStatus() { return providerStatus; }
    public boolean isProcessed() { return processed; }
    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public void markProcessed() { this.processed = true; }
}
