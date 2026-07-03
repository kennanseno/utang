package com.utang.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.utang.domain.Customer;
import com.utang.domain.EntryType;
import com.utang.domain.Store;
import com.utang.repository.StoreRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Covers the PRD test scenarios for ledger balance and reminder locking. */
@SpringBootTest
class LedgerAndReminderTest {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private LedgerService ledgerService;
    @Autowired
    private StoreRepository storeRepository;

    private Store newStore() {
        long unique = System.nanoTime();
        return storeRepository.save(new Store("0917" + unique, "Aling Nena Store " + unique));
    }

    @Test
    void scenario1_addUtang_balanceIsHundred() {
        Store store = newStore();
        Customer customer = customerService.create(store.getId(), "Juan", null);

        BigDecimal balance = ledgerService.applyEntry(
                customer.getId(), EntryType.DEBIT, new BigDecimal("100.00"), "sardinas");

        assertThat(balance).isEqualByComparingTo("100.00");
    }

    @Test
    void scenario2_utangThenCash_balanceIsFifty() {
        Store store = newStore();
        Customer customer = customerService.create(store.getId(), "Pedro", null);

        ledgerService.applyEntry(customer.getId(), EntryType.DEBIT, new BigDecimal("100.00"), null);
        BigDecimal balance = ledgerService.applyEntry(
                customer.getId(), EntryType.CREDIT, new BigDecimal("50.00"), "bayad");

        assertThat(balance).isEqualByComparingTo("50.00");
    }
}
