package com.utang.service;

import com.utang.config.AppProperties;
import com.utang.domain.Customer;
import com.utang.domain.Store;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import org.springframework.stereotype.Service;

/**
 * Builds copyable reminder messages. Reminders are delivered by the store owner
 * from their own phone (via an SMS deep link or by copying the message); the
 * backend does not send or track them.
 */
@Service
public class ReminderService {

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");

    private final AppProperties appProperties;

    public ReminderService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String buildMessage(Store store, Customer customer) {
        String link = appProperties.getPublicBaseUrl() + "/pay/" + customer.getPayToken();
        String amount = AMOUNT_FORMAT.format(customer.getCurrentBalance().max(BigDecimal.ZERO));
        return "Hi " + customer.getName() + "! May utang ka na \u20b1" + amount
                + " sa " + store.getName() + ".\n"
                + "Pwede ka magbayad dito: " + link;
    }
}
