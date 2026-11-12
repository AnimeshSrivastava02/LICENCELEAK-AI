package com.licenseleak.approvals.infrastructure;

import com.licenseleak.approvals.domain.CreatorDecisionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorDecisionRepository extends JpaRepository<CreatorDecisionEntity, String> {
    Optional<CreatorDecisionEntity> findTopByCaseIdOrderByDecidedAtDesc(String caseId);
}
