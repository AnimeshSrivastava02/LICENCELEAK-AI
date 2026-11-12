package com.licenseleak.audit.application;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.licenseleak.audit.domain.AuditEventEntity;
import com.licenseleak.audit.infrastructure.AuditEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(String caseId, String eventType, String recordId, Map<String, ?> safeDetails) {
        try {
            String details = objectMapper.writeValueAsString(safeDetails);
            repository.save(new AuditEventEntity(UUID.randomUUID().toString(), caseId, eventType,
                    recordId, details, OffsetDateTime.now(ZoneOffset.UTC)));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize safe audit details", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditItem> listForCase(String caseId) {
        return repository.findAllByCaseIdOrderByOccurredAtDesc(caseId).stream()
                .map(event -> new AuditItem(event.getId(), event.getEventType(), event.getRecordId(),
                        parseDetails(event.getDetails()), event.getOccurredAt()))
                .toList();
    }

    private JsonNode parseDetails(String details) {
        try {
            return objectMapper.readTree(details);
        } catch (JacksonException exception) {
            return objectMapper.createObjectNode();
        }
    }

    public record AuditItem(String eventId, String eventType, String recordId, JsonNode details,
            OffsetDateTime occurredAt) {
    }
}
