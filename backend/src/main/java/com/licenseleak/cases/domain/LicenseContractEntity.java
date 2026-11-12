package com.licenseleak.cases.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "license_contract")
public class LicenseContractEntity {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "case_id", length = 64, nullable = false, unique = true)
    private String caseId;

    @Column(length = 160, nullable = false)
    private String title;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "text", nullable = false)
    private String summary;

    @Column(name = "contract_valid_through")
    private LocalDate contractValidThrough;

    protected LicenseContractEntity() {
    }

    public String getId() {
        return id;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getTitle() {
        return title;
    }

    public int getVersion() {
        return version;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDate getContractValidThrough() {
        return contractValidThrough;
    }
}
