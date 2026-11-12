package com.licenseleak.analysis.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analysis_run", uniqueConstraints = {
        @UniqueConstraint(name = "analysis_run_case_version_unique", columnNames = {"case_id", "version"})
})
public class AnalysisRunEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "case_id", length = 64, nullable = false)
    private String caseId;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AnalysisOutcome outcome;

    @Column(columnDefinition = "text", nullable = false)
    private String summary;

    @Column(nullable = false, length = 16)
    private String source;

    @Column(name = "model_name", length = 120)
    private String modelName;

    @Column(name = "contract_version", nullable = false)
    private int contractVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_clause_reference", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "reference_order")
    @Column(name = "clause_reference", nullable = false, length = 120)
    private List<String> clauseReferences = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_evidence_reference", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "reference_order")
    @Column(name = "evidence_reference", nullable = false, length = 64)
    private List<String> evidenceReferences = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_uncertainty", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "uncertainty_order")
    @Column(name = "uncertainty", nullable = false, columnDefinition = "text")
    private List<String> uncertainties = new ArrayList<>();

    protected AnalysisRunEntity() {
    }

    public AnalysisRunEntity(
            String id, String caseId, int version, AnalysisOutcome outcome, String summary,
            String source, String modelName, int contractVersion, OffsetDateTime createdAt,
            List<String> clauseReferences, List<String> evidenceReferences, List<String> uncertainties) {
        this.id = id;
        this.caseId = caseId;
        this.version = version;
        this.outcome = outcome;
        this.summary = summary;
        this.source = source;
        this.modelName = modelName;
        this.contractVersion = contractVersion;
        this.createdAt = createdAt;
        this.clauseReferences = new ArrayList<>(clauseReferences);
        this.evidenceReferences = new ArrayList<>(evidenceReferences);
        this.uncertainties = new ArrayList<>(uncertainties);
    }

    public String getId() { return id; }
    public String getCaseId() { return caseId; }
    public int getVersion() { return version; }
    public AnalysisOutcome getOutcome() { return outcome; }
    public String getSummary() { return summary; }
    public String getSource() { return source; }
    public String getModelName() { return modelName; }
    public int getContractVersion() { return contractVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<String> getClauseReferences() { return List.copyOf(clauseReferences); }
    public List<String> getEvidenceReferences() { return List.copyOf(evidenceReferences); }
    public List<String> getUncertainties() { return List.copyOf(uncertainties); }
}
