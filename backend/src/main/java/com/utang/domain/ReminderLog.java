package com.utang.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reminder_logs")
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String method = "manual";

    @Column(nullable = false)
    private String channel = "copy";

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Column(name = "sent_on", nullable = false)
    private LocalDate sentOn;

    protected ReminderLog() {
    }

    public ReminderLog(Long customerId, String channel, LocalDate sentOn) {
        this.customerId = customerId;
        this.channel = channel;
        this.sentOn = sentOn;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getMethod() {
        return method;
    }

    public String getChannel() {
        return channel;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public LocalDate getSentOn() {
        return sentOn;
    }
}
