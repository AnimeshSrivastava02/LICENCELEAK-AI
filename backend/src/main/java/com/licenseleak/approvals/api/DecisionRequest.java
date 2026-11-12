package com.licenseleak.approvals.api;

import com.licenseleak.approvals.domain.DecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecisionRequest(
        @NotNull DecisionType decision,
        @NotBlank String analysisId,
        String feeQuoteId) {
}
