package com.licenseleak.invoices.infrastructure;

import com.licenseleak.invoices.domain.PaypalWebhookEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaypalWebhookEventRepository extends JpaRepository<PaypalWebhookEventEntity, String> {
    List<PaypalWebhookEventEntity> findAllByProviderInvoiceIdAndProcessedFalseOrderByReceivedAtAsc(String providerInvoiceId);
}
