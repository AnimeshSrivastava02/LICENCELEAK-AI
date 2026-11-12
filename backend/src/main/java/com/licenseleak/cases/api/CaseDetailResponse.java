package com.licenseleak.cases.api;

import java.util.List;
import java.time.LocalDate;

public record CaseDetailResponse(
        String caseId,
        String title,
        boolean fictional,
        ContractSummary contract,
        List<EvidenceSummary> evidence) {

    public record ContractSummary(
            String contractId, int version, String title, String summary, LocalDate validThrough) {
    }

    public record EvidenceSummary(
            String evidenceId, String type, String label, LocalDate adReceivedOn, boolean outOfContract) {
    }
}
