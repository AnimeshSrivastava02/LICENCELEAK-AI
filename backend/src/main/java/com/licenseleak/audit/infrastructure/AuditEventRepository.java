package com.licenseleak.audit.infrastructure;

import com.licenseleak.audit.domain.AuditEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {
    List<AuditEventEntity> findAllByCaseIdOrderByOccurredAtDesc(String caseId);
}
