package com.utang.service;

import com.utang.domain.Customer;
import com.utang.error.NotFoundException;
import com.utang.repository.CustomerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer create(Long storeId, String name, String phoneNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        String payToken = UUID.randomUUID().toString().replace("-", "");
        String phone = (phoneNumber == null || phoneNumber.isBlank()) ? null : phoneNumber.trim();
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

    @Transactional(readOnly = true)
    public Customer getByPayToken(String payToken) {
        return customerRepository.findByPayToken(payToken)
                .orElseThrow(() -> new NotFoundException("Payment link not found"));
    }
}
