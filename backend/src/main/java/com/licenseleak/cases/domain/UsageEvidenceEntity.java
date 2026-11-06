package com.licenseleak.cases.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_evidence")
public class UsageEvidenceEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "case_id", length = 64, nullable = false)
    private String caseId;

    @Column(name = "evidence_type", length = 40, nullable = false)
    private String type;

    @Column(length = 160, nullable = false)
    private String label;

    protected UsageEvidenceEntity() {
    }

    public String getId() {
        return id;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }
}
