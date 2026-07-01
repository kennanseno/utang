package com.utang.payment;

import java.math.BigDecimal;

/**
 * Payment provider abstraction (adapter pattern). MVP ships a PayMongo adapter
 * and a mock adapter; additional PSPs can be added without touching call sites.
 */
public interface PaymentGateway {

    /** Provider identifier stored on payments/webhooks (e.g. "paymongo"). */
    String provider();

    /**
     * Create a hosted checkout link for the given amount.
     *
     * @param amount      amount in PHP
     * @param description human-readable description shown at checkout
     * @return reference id + hosted checkout URL
     */
    PaymentLinkResult createPaymentLink(BigDecimal amount, String description);

    /** Verify a webhook signature header against the raw request body. */
    boolean verifySignature(String rawBody, String signatureHeader);
}
