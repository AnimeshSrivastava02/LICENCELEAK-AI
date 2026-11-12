package com.licenseleak.cases.api;

import com.licenseleak.cases.domain.CaseScenarioType;
import com.licenseleak.cases.domain.CaseStatus;

public record CaseSummaryResponse(
        String caseId,
        String title,
        CaseScenarioType scenarioType,
        CaseStatus status,
        boolean fictional) {
}
