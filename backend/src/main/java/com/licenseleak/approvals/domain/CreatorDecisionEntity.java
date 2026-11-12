package com.licenseleak.approvals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "creator_decision")
public class CreatorDecisionEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "case_id", length = 64, nullable = false)
    private String caseId;

    @Column(name = "analysis_id", length = 36, nullable = false)
    private String analysisId;

    @Column(name = "fee_quote_id", length = 36)
    private String feeQuoteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DecisionType decision;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    protected CreatorDecisionEntity() {
    }

    public CreatorDecisionEntity(String id, String caseId, String analysisId, String feeQuoteId,
            DecisionType decision, OffsetDateTime decidedAt) {
        this.id = id;
        this.caseId = caseId;
        this.analysisId = analysisId;
        this.feeQuoteId = feeQuoteId;
        this.decision = decision;
        this.decidedAt = decidedAt;
    }

    public String getId() { return id; }
    public String getCaseId() { return caseId; }
    public String getAnalysisId() { return analysisId; }
    public String getFeeQuoteId() { return feeQuoteId; }
    public DecisionType getDecision() { return decision; }
    public OffsetDateTime getDecidedAt() { return decidedAt; }
}
