package com.utang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.utang.domain.Customer;
import com.utang.domain.EntryType;
import com.utang.domain.Store;
import com.utang.error.NotFoundException;
import com.utang.repository.CustomerRepository;
import com.utang.repository.LedgerEntryRepository;
import com.utang.repository.StoreRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Covers customer CRUD, store-scoped ownership, and delete cascade behaviour. */
@SpringBootTest
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;
    @Autowired
    private LedgerService ledgerService;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private LedgerEntryRepository ledgerRepository;

    private Store newStore() {
        long unique = System.nanoTime();
        return storeRepository.save(new Store("0917" + unique, "Store " + unique));
    }

    @Test
    void create_thenGet_returnsCustomerWithNormalizedPhone() {
        Store store = newStore();

        Customer created = customerService.create(store.getId(), "  Juan  ", "09170000001");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Juan");
        assertThat(created.getPhoneNumber()).isEqualTo("+639170000001");
        assertThat(created.getPayToken()).isNotBlank();

        Customer fetched = customerService.get(store.getId(), created.getId());
        assertThat(fetched.getId()).isEqualTo(created.getId());
    }

    @Test
    void get_fromAnotherStore_throwsNotFound() {
        Store owner = newStore();
        Store intruder = newStore();
        Customer customer = customerService.create(owner.getId(), "Pedro", "09170000002");

        assertThatThrownBy(() -> customerService.get(intruder.getId(), customer.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_changesNameAndPhone() {
        Store store = newStore();
        Customer customer = customerService.create(store.getId(), "Maria", "09170000003");

        Customer updated = customerService.update(
                store.getId(), customer.getId(), "Maria Clara", "09170000009");

        assertThat(updated.getName()).isEqualTo("Maria Clara");
        assertThat(updated.getPhoneNumber()).isEqualTo("+639170000009");
    }

    @Test
    void delete_removesCustomerAndCascadesLedgerEntries() {
        Store store = newStore();
        Customer customer = customerService.create(store.getId(), "Ana", "09170000004");
        ledgerService.applyEntry(customer.getId(), EntryType.DEBIT, new BigDecimal("100.00"), "utang");
        ledgerService.applyEntry(customer.getId(), EntryType.CREDIT, new BigDecimal("40.00"), "bayad");
        Long customerId = customer.getId();
        assertThat(ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).hasSize(2);

        customerService.delete(store.getId(), customerId);

        assertThat(customerRepository.findById(customerId)).isEmpty();
        assertThat(ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).isEmpty();
    }

    @Test
    void delete_stillWorksWhenCustomerHasOutstandingBalance() {
        Store store = newStore();
        Customer customer = customerService.create(store.getId(), "Ben", "09170000005");
        ledgerService.applyEntry(customer.getId(), EntryType.DEBIT, new BigDecimal("250.00"), "utang");
        Long customerId = customer.getId();

        customerService.delete(store.getId(), customerId);

        assertThat(customerRepository.findById(customerId)).isEmpty();
        assertThat(ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).isEmpty();
    }

    @Test
    void delete_fromAnotherStore_throwsNotFound() {
        Store owner = newStore();
        Store intruder = newStore();
        Customer customer = customerService.create(owner.getId(), "Cardo", "09170000006");

        assertThatThrownBy(() -> customerService.delete(intruder.getId(), customer.getId()))
                .isInstanceOf(NotFoundException.class);

        // The customer must survive an unauthorized delete attempt.
        assertThat(customerRepository.findById(customer.getId())).isPresent();
    }
}
