package com.licenseleak.analysis.application;

import com.licenseleak.analysis.domain.AnalysisOutcome;
import com.licenseleak.cases.domain.CaseScenarioType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "licenseleak.analysis.provider", havingValue = "mock", matchIfMissing = true)
public class MockAnalysisProvider implements AnalysisProvider {

    @Override
    public AnalysisDraft analyze(AnalysisInput input) {
        List<String> evidenceIds = input.evidence().stream().map(AnalysisInput.Evidence::evidenceId).toList();
        return switch (input.demoScenario()) {
            case POSSIBLE_MISMATCH -> new AnalysisDraft(
                    AnalysisOutcome.POSSIBLE_MISMATCH,
                    "The recorded ad date falls after the license term shown in this case. Confirm the term, source record, and cited material before making a decision.",
                    List.of("clause-4"), evidenceIds, List.of(), "MOCK", "fictional-scenario-mock");
            case PERMITTED -> new AnalysisDraft(
                    AnalysisOutcome.PERMITTED,
                    "The recorded ad date falls within the license term shown in this case. Confirm the source record and permitted channels before closing the review.",
                    List.of("usage-condition"), evidenceIds, List.of(), "MOCK", "fictional-scenario-mock");
            case AMBIGUOUS -> new AnalysisDraft(
                    AnalysisOutcome.AMBIGUOUS,
                    "The agreement end date or evidence timing is incomplete, so this case needs human review.",
                    List.of("clause-4"), evidenceIds,
                    List.of("A reliable contract end date and ad receipt date are not both available."),
                    "MOCK", "fictional-scenario-mock");
        };
    }
}
