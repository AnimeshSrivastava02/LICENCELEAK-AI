package com.licenseleak.cases.api;

import java.util.List;

public record CaseDetailResponse(
        String caseId,
        String title,
        boolean fictional,
        ContractSummary contract,
        List<EvidenceSummary> evidence) {

    public record ContractSummary(String contractId, int version, String title, String summary) {
    }

    public record EvidenceSummary(String evidenceId, String type, String label) {
    }
}
