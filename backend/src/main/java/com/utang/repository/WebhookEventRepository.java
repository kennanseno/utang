package com.utang.repository;

import com.utang.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByProviderAndExternalEventId(String provider, String externalEventId);
}
