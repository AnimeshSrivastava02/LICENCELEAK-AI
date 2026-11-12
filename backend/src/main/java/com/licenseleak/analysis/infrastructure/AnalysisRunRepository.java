package com.licenseleak.analysis.infrastructure;

import com.licenseleak.analysis.domain.AnalysisRunEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRunEntity, String> {
    Optional<AnalysisRunEntity> findFirstByCaseIdOrderByVersionDesc(String caseId);
    Optional<AnalysisRunEntity> findByIdAndCaseId(String id, String caseId);
    int countByCaseId(String caseId);
}
