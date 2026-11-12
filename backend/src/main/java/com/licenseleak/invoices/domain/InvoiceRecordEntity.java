package com.licenseleak.invoices.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "invoice_record")
public class InvoiceRecordEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "case_id", length = 64, nullable = false, unique = true)
    private String caseId;

    @Column(name = "decision_id", length = 36, nullable = false)
    private String decisionId;

    @Column(name = "fee_quote_id", length = 36, nullable = false)
    private String feeQuoteId;

    @Column(name = "provider_name", length = 16, nullable = false)
    private String providerName;

    @Column(name = "provider_invoice_id", length = 120, unique = true)
    private String providerInvoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private InvoiceStatus status;

    @Column(name = "idempotency_key", length = 100, nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "invoice_url", columnDefinition = "text")
    private String invoiceUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected InvoiceRecordEntity() {
    }

    public InvoiceRecordEntity(String id, String caseId, String decisionId, String feeQuoteId,
            String providerName, String providerInvoiceId, InvoiceStatus status, String idempotencyKey,
            String invoiceUrl, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.caseId = caseId;
        this.decisionId = decisionId;
        this.feeQuoteId = feeQuoteId;
        this.providerName = providerName;
        this.providerInvoiceId = providerInvoiceId;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.invoiceUrl = invoiceUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateProviderResult(String providerInvoiceId, InvoiceStatus status, String invoiceUrl,
            OffsetDateTime updatedAt) {
        this.providerInvoiceId = providerInvoiceId;
        this.status = status;
        this.invoiceUrl = invoiceUrl;
        this.updatedAt = updatedAt;
    }

    public void updateStatus(InvoiceStatus status, OffsetDateTime updatedAt) {
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getCaseId() { return caseId; }
    public String getDecisionId() { return decisionId; }
    public String getFeeQuoteId() { return feeQuoteId; }
    public String getProviderName() { return providerName; }
    public String getProviderInvoiceId() { return providerInvoiceId; }
    public InvoiceStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getInvoiceUrl() { return invoiceUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
