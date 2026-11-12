package com.licenseleak.analysis.application;

import com.licenseleak.cases.domain.CaseScenarioType;
import java.time.LocalDate;
import java.util.List;

public record AnalysisInput(
        String caseId,
        String caseTitle,
        CaseScenarioType demoScenario,
        int contractVersion,
        String contractTitle,
        String contractText,
        LocalDate contractValidThrough,
        List<Evidence> evidence) {

    public record Evidence(String evidenceId, String type, String label, LocalDate receivedOn, boolean outOfContract) {
    }
}
