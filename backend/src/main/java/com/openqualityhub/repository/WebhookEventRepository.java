package com.openqualityhub.repository;

import com.openqualityhub.entity.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Page<WebhookEvent> findByIntegrationIdOrderByReceivedAtDesc(UUID integrationId, Pageable pageable);

    long countByIntegrationId(UUID integrationId);

    long countByStatus(WebhookEvent.EventStatus status);

    long countByIntegrationIdAndStatus(UUID integrationId, WebhookEvent.EventStatus status);
}
