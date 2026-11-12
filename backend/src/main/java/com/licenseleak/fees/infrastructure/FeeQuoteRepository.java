package com.licenseleak.fees.infrastructure;

import com.licenseleak.fees.domain.FeeQuoteEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeQuoteRepository extends JpaRepository<FeeQuoteEntity, String> {
    Optional<FeeQuoteEntity> findByAnalysisId(String analysisId);
    Optional<FeeQuoteEntity> findByIdAndAnalysisId(String id, String analysisId);
}
