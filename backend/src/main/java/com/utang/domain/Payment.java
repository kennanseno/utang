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
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Column
    private String provider;

    @Column(name = "provider_ref_id")
    private String providerRefId;

    @Column(nullable = false)
    private String status = "PAID";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Payment() {
    }

    public Payment(Long customerId, BigDecimal amount, PaymentMethod method,
                   String provider, String providerRefId) {
        this.customerId = customerId;
        this.amount = amount;
        this.method = method;
        this.provider = provider;
        this.providerRefId = providerRefId;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderRefId() {
        return providerRefId;
    }

    public String getStatus() {
        return status;
    }
}
