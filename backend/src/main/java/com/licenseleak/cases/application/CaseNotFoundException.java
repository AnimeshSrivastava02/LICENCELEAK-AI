package com.licenseleak.cases.application;

public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(String caseId) {
        super("No fictional case exists with id: " + caseId);
    }
}
