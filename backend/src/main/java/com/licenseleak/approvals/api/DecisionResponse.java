package com.licenseleak.approvals.api;

import com.licenseleak.approvals.domain.CreatorDecisionEntity;
import java.time.OffsetDateTime;

public record DecisionResponse(
        String decisionId,
        String caseId,
        String decision,
        String analysisId,
        String feeQuoteId,
        OffsetDateTime decidedAt) {
    public static DecisionResponse from(CreatorDecisionEntity entity) {
        return new DecisionResponse(entity.getId(), entity.getCaseId(), entity.getDecision().name(),
                entity.getAnalysisId(), entity.getFeeQuoteId(), entity.getDecidedAt());
    }
}
