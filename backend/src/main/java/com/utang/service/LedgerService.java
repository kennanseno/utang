package com.utang.service;

import com.utang.domain.Customer;
import com.utang.domain.EntryType;
import com.utang.domain.LedgerEntry;
import com.utang.error.NotFoundException;
import com.utang.repository.CustomerRepository;
import com.utang.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core ledger operations. The ledger is the source of truth; the customer's
 * {@code current_balance} is a denormalized cache kept in sync atomically.
 *
 * <pre>balance = sum(debits) - sum(credits)</pre>
 */
@Service
public class LedgerService {

    private final CustomerRepository customerRepository;
    private final LedgerEntryRepository ledgerRepository;

    public LedgerService(CustomerRepository customerRepository, LedgerEntryRepository ledgerRepository) {
        this.customerRepository = customerRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Append a ledger entry and update the customer's balance atomically.
     * DEBIT increases the balance; CREDIT decreases it. The customer row is
     * locked for update so concurrent writes are serialized.
     *
     * @return the customer's new balance
     */
    @Transactional
    public BigDecimal applyEntry(Long customerId, EntryType type, BigDecimal amount, String note) {
        validateAmount(amount);
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));

        ledgerRepository.save(new LedgerEntry(customerId, type, amount, note));

        BigDecimal delta = type == EntryType.DEBIT ? amount : amount.negate();
        customer.setCurrentBalance(customer.getCurrentBalance().add(delta));
        customerRepository.save(customer);
        return customer.getCurrentBalance();
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> history(Long customerId) {
        return ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }
}
