package com.licenseleak.cases.infrastructure;

import com.licenseleak.cases.domain.DemoCaseEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoCaseRepository extends JpaRepository<DemoCaseEntity, String> {

    List<DemoCaseEntity> findAllByOrderByTitleAsc();
}
