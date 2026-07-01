package com.utang.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "external_event_id", nullable = false)
    private String externalEventId;

    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean processed = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WebhookEvent() {
    }

    public WebhookEvent(String provider, String externalEventId, String payload) {
        this.provider = provider;
        this.externalEventId = externalEventId;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void markProcessed() {
        this.processed = true;
    }
}
