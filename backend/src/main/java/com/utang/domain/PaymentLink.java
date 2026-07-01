package com.utang.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Links a PayMongo reference id to the customer so the webhook can resolve it. */
@Entity
@Table(name = "payment_links")
public class PaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String provider = "paymongo";

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "checkout_url", nullable = false)
    private String checkoutUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PaymentLink() {
    }

    public PaymentLink(Long customerId, String referenceId, BigDecimal amount, String checkoutUrl) {
        this.customerId = customerId;
        this.referenceId = referenceId;
        this.amount = amount;
        this.checkoutUrl = checkoutUrl;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getProvider() {
        return provider;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }
}
