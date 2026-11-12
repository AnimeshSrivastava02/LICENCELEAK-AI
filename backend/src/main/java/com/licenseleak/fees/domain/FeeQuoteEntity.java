package com.licenseleak.fees.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fee_quote")
public class FeeQuoteEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "analysis_id", length = 36, nullable = false, unique = true)
    private String analysisId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "daily_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal dailyRate;

    @Column(name = "total_billable_days", nullable = false)
    private int totalBillableDays;

    @Column(nullable = false)
    private boolean actionable;

    @Column(name = "rule_reference", length = 120, nullable = false)
    private String ruleReference;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "feeQuote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeeQuoteItemEntity> items = new ArrayList<>();

    protected FeeQuoteEntity() {
    }

    public FeeQuoteEntity(
            String id, String analysisId, BigDecimal amount, String currency, BigDecimal dailyRate,
            int totalBillableDays, boolean actionable, String ruleReference, OffsetDateTime createdAt,
            List<FeeQuoteItemEntity> items) {
        this.id = id;
        this.analysisId = analysisId;
        this.amount = amount;
        this.currency = currency;
        this.dailyRate = dailyRate;
        this.totalBillableDays = totalBillableDays;
        this.actionable = actionable;
        this.ruleReference = ruleReference;
        this.createdAt = createdAt;
        this.items = new ArrayList<>(items);
        this.items.forEach(item -> item.setFeeQuote(this));
    }

    public String getId() { return id; }
    public String getAnalysisId() { return analysisId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public int getTotalBillableDays() { return totalBillableDays; }
    public boolean isActionable() { return actionable; }
    public String getRuleReference() { return ruleReference; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<FeeQuoteItemEntity> getItems() { return List.copyOf(items); }
}
