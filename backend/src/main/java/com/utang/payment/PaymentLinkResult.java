package com.utang.payment;

import java.math.BigDecimal;

/** Result of creating a hosted payment link with a provider. */
public record PaymentLinkResult(String referenceId, String checkoutUrl) {

    public static PaymentLinkResult of(String referenceId, String checkoutUrl) {
        return new PaymentLinkResult(referenceId, checkoutUrl);
    }
}
