package com.licenseleak.cases.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "demo_case")
public class DemoCaseEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(length = 160, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", length = 40, nullable = false)
    private CaseScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private CaseStatus status;

    @Column(nullable = false)
    private boolean fictional;

    protected DemoCaseEntity() {
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public CaseScenarioType getScenarioType() {
        return scenarioType;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public boolean isFictional() {
        return fictional;
    }
}
