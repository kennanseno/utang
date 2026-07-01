package com.utang.service;

import com.utang.config.AppProperties;
import com.utang.domain.Customer;
import com.utang.domain.ReminderLog;
import com.utang.domain.Store;
import com.utang.error.ConflictException;
import com.utang.repository.ReminderLogRepository;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds copyable reminder messages and enforces the "max 1 manual reminder per
 * customer per day" lock. The copy action counts as a sent reminder.
 */
@Service
public class ReminderService {

    /** Philippine time is used to determine the reminder "day". */
    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");

    private final ReminderLogRepository reminderRepository;
    private final AppProperties appProperties;

    public ReminderService(ReminderLogRepository reminderRepository, AppProperties appProperties) {
        this.reminderRepository = reminderRepository;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public boolean canSendToday(Long customerId) {
        return !reminderRepository.existsByCustomerIdAndSentOn(customerId, today());
    }

    public String buildMessage(Store store, Customer customer) {
        String link = appProperties.getPublicBaseUrl() + "/pay/" + customer.getPayToken();
        String amount = AMOUNT_FORMAT.format(customer.getCurrentBalance().max(BigDecimal.ZERO));
        return "Hi " + customer.getName() + "! May utang ka na \u20b1" + amount
                + " sa " + store.getName() + ".\n"
                + "Pwede ka magbayad dito: " + link;
    }

    /**
     * Records that a reminder was sent (copied). Enforces the one-per-day lock.
     *
     * @throws ConflictException if a reminder was already sent to this customer today
     */
    @Transactional
    public void logReminder(Long customerId, String channel) {
        LocalDate today = today();
        if (reminderRepository.existsByCustomerIdAndSentOn(customerId, today)) {
            throw new ConflictException("A reminder was already sent to this suki today");
        }
        reminderRepository.save(new ReminderLog(customerId, channel, today));
    }

    private LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
