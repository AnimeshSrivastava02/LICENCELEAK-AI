package com.licenseleak.analysis.application;

import com.licenseleak.analysis.domain.AnalysisOutcome;
import java.util.List;

public record AnalysisDraft(
        AnalysisOutcome outcome,
        String summary,
        List<String> clauseReferences,
        List<String> evidenceReferences,
        List<String> uncertainties,
        String source,
        String modelName) {
}
