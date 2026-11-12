package com.licenseleak.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_event")
public class AuditEventEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "case_id", length = 64, nullable = false)
    private String caseId;

    @Column(name = "event_type", length = 48, nullable = false)
    private String eventType;

    @Column(name = "record_id", length = 120)
    private String recordId;

    @Column(columnDefinition = "text", nullable = false)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(String id, String caseId, String eventType, String recordId,
            String details, OffsetDateTime occurredAt) {
        this.id = id;
        this.caseId = caseId;
        this.eventType = eventType;
        this.recordId = recordId;
        this.details = details;
        this.occurredAt = occurredAt;
    }

    public String getId() { return id; }
    public String getCaseId() { return caseId; }
    public String getEventType() { return eventType; }
    public String getRecordId() { return recordId; }
    public String getDetails() { return details; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
