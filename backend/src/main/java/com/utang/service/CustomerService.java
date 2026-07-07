package com.utang.service;

import com.utang.domain.Customer;
import com.utang.error.NotFoundException;
import com.utang.repository.CustomerRepository;
import com.utang.repository.LedgerEntryRepository;
import com.utang.support.PhoneNumbers;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final LedgerEntryRepository ledgerRepository;

    public CustomerService(CustomerRepository customerRepository,
                           LedgerEntryRepository ledgerRepository) {
        this.customerRepository = customerRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public Customer create(Long storeId, String name, String phoneNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("mobile number is required");
        }
        String payToken = UUID.randomUUID().toString().replace("-", "");
        String phone = PhoneNumbers.toE164(phoneNumber);
        return customerRepository.save(new Customer(storeId, name.trim(), phone, payToken));
    }

    @Transactional(readOnly = true)
    public List<Customer> list(Long storeId) {
        return customerRepository.findByStoreIdOrderByNameAsc(storeId);
    }
    @Transactional(readOnly = true)
    public Customer get(Long storeId, Long customerId) {
        return customerRepository.findByIdAndStoreId(customerId, storeId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
    }

    @Transactional
    public Customer update(Long storeId, Long customerId, String name, String phoneNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("mobile number is required");
        }
        Customer customer = get(storeId, customerId);
        customer.setName(name.trim());
        customer.setPhoneNumber(PhoneNumbers.toE164(phoneNumber));
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long storeId, Long customerId) {
        Customer customer = get(storeId, customerId);
        ledgerRepository.deleteByCustomerId(customer.getId());
        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public Customer getByPayToken(String payToken) {
        return customerRepository.findByPayToken(payToken)
                .orElseThrow(() -> new NotFoundException("Payment page not found"));
    }
}
