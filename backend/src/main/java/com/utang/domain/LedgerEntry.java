package com.utang.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected LedgerEntry() {
    }

    public LedgerEntry(Long customerId, EntryType type, BigDecimal amount, String note) {
        this.customerId = customerId;
        this.type = type;
        this.amount = amount;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public EntryType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
