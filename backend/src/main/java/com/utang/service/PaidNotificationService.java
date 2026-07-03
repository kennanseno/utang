package com.utang.service;

import com.utang.domain.Customer;
import com.utang.domain.Store;
import com.utang.error.TooManyRequestsException;
import com.utang.sms.SmsSender;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Sends an SMS to the store owner when a customer (from the public pay page)
 * indicates they have paid their utang.
 *
 * <p>The pay page is public and unauthenticated, so this is rate limited per
 * customer pay token to avoid spamming the store owner and running up SMS costs.
 * The cooldown is kept in memory, which is sufficient for a single-instance
 * deployment.
 */
@Service
public class PaidNotificationService {

    /** Minimum time between "I've paid" texts for the same customer. */
    private static final Duration COOLDOWN = Duration.ofMinutes(10);

    private final SmsSender smsSender;
    private final Map<String, Instant> lastNotifiedByToken = new ConcurrentHashMap<>();

    public PaidNotificationService(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    /**
     * Texts the store owner that the customer says they have paid.
     *
     * @throws TooManyRequestsException if a notification was sent for this customer recently
     */
    public void notifyPaid(Store store, Customer customer) {
        Instant now = Instant.now();
        Instant last = lastNotifiedByToken.get(customer.getPayToken());
        if (last != null && Duration.between(last, now).compareTo(COOLDOWN) < 0) {
            throw new TooManyRequestsException(
                    "You already told the store you paid. Please wait a few minutes.");
        }

        String message = "Utang: Sinabi ni " + customer.getName()
                + " na nakabayad na siya. Pakisuri po ang inyong Utang app.";
        smsSender.send(store.getPhoneNumber(), message);
        lastNotifiedByToken.put(customer.getPayToken(), now);
    }
}
