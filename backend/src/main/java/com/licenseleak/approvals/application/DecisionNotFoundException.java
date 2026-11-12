package com.licenseleak.approvals.application;

public class DecisionNotFoundException extends RuntimeException {
    public DecisionNotFoundException(String caseId) { super("No creator decision exists for case: " + caseId); }
}
