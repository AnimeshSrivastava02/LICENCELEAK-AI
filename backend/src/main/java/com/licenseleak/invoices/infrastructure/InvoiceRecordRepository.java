package com.licenseleak.invoices.infrastructure;

import com.licenseleak.invoices.domain.InvoiceRecordEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRecordRepository extends JpaRepository<InvoiceRecordEntity, String> {
    Optional<InvoiceRecordEntity> findByCaseId(String caseId);
    Optional<InvoiceRecordEntity> findByProviderInvoiceId(String providerInvoiceId);
}
