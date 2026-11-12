package com.licenseleak.analysis.application;

public class AnalysisNotFoundException extends RuntimeException {
    public AnalysisNotFoundException(String caseId) {
        super("No analysis exists for case: " + caseId);
    }
}
