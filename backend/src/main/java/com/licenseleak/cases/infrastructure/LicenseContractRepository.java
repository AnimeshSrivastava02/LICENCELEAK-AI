package com.licenseleak.cases.infrastructure;

import com.licenseleak.cases.domain.LicenseContractEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseContractRepository extends JpaRepository<LicenseContractEntity, String> {

    Optional<LicenseContractEntity> findByCaseId(String caseId);
}
