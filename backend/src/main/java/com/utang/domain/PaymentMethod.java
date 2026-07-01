package com.utang.domain;

/** How a payment was collected. CASH is manual; LINK is via a PayMongo hosted checkout. */
public enum PaymentMethod {
    CASH,
    LINK
}
