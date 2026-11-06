package com.licenseleak.cases.infrastructure;

import com.licenseleak.cases.domain.UsageEvidenceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageEvidenceRepository extends JpaRepository<UsageEvidenceEntity, String> {

    List<UsageEvidenceEntity> findAllByCaseIdOrderByIdAsc(String caseId);
}
