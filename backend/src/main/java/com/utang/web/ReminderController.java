package com.utang.web;

import com.utang.domain.Customer;
import com.utang.domain.Store;
import com.utang.dto.Dtos.RemindResponse;
import com.utang.dto.Dtos.ReminderPreviewResponse;
import com.utang.security.CurrentStore;
import com.utang.service.CustomerService;
import com.utang.service.ReminderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReminderController {

    private final ReminderService reminderService;
    private final CustomerService customerService;

    ReminderController(ReminderService reminderService, CustomerService customerService) {
        this.reminderService = reminderService;
        this.customerService = customerService;
    }

    @GetMapping("/customers/{id}/reminder-preview")
    ReminderPreviewResponse preview(@CurrentStore Store store, @PathVariable Long id) {
        Customer customer = customerService.get(store.getId(), id);
        String message = reminderService.buildMessage(store, customer);
        boolean canSend = reminderService.canSendToday(customer.getId());
        return new ReminderPreviewResponse(message, canSend);
    }

    /** Logs the reminder (copy counts as sent). Enforces the once-per-day lock. */
    @PostMapping("/customers/{id}/remind")
    RemindResponse remind(@CurrentStore Store store, @PathVariable Long id) {
        Customer customer = customerService.get(store.getId(), id);
        String message = reminderService.buildMessage(store, customer);
        reminderService.logReminder(customer.getId(), "copy");
        return new RemindResponse(message, true);
    }
}
