package com.licenseleak.fees.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fee_quote_item", uniqueConstraints = {
        @UniqueConstraint(name = "fee_quote_item_evidence_unique", columnNames = {"fee_quote_id", "evidence_id"})
})
public class FeeQuoteItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_quote_id", nullable = false)
    private FeeQuoteEntity feeQuote;

    @Column(name = "evidence_id", length = 64, nullable = false)
    private String evidenceId;

    @Column(name = "contract_valid_through")
    private LocalDate contractValidThrough;

    @Column(name = "ad_received_on")
    private LocalDate adReceivedOn;

    @Column(name = "billable_days", nullable = false)
    private int billableDays;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private boolean actionable;

    protected FeeQuoteItemEntity() {
    }

    public FeeQuoteItemEntity(
            String evidenceId, LocalDate contractValidThrough, LocalDate adReceivedOn,
            int billableDays, BigDecimal amount, boolean actionable) {
        this.evidenceId = evidenceId;
        this.contractValidThrough = contractValidThrough;
        this.adReceivedOn = adReceivedOn;
        this.billableDays = billableDays;
        this.amount = amount;
        this.actionable = actionable;
    }

    void setFeeQuote(FeeQuoteEntity feeQuote) { this.feeQuote = feeQuote; }
    public String getEvidenceId() { return evidenceId; }
    public LocalDate getContractValidThrough() { return contractValidThrough; }
    public LocalDate getAdReceivedOn() { return adReceivedOn; }
    public int getBillableDays() { return billableDays; }
    public BigDecimal getAmount() { return amount; }
    public boolean isActionable() { return actionable; }
}
