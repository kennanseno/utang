package com.utang.web;

import com.utang.domain.Customer;
import com.utang.domain.Store;
import com.utang.dto.Dtos.CreateCustomerRequest;
import com.utang.dto.Dtos.CustomerResponse;
import com.utang.dto.Dtos.UpdateCustomerRequest;
import com.utang.security.CurrentStore;
import com.utang.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public CustomerResponse create(@CurrentStore Store store,
                                   @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.create(store.getId(), request.name(), request.phoneNumber());
        return ApiMapper.toCustomer(customer);
    }

    @GetMapping
    public List<CustomerResponse> list(@CurrentStore Store store) {
        return customerService.list(store.getId()).stream()
                .map(ApiMapper::toCustomer)
                .toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@CurrentStore Store store, @PathVariable Long id) {
        return ApiMapper.toCustomer(customerService.get(store.getId(), id));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@CurrentStore Store store,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateCustomerRequest request) {
        Customer customer = customerService.update(
                store.getId(), id, request.name(), request.phoneNumber());
        return ApiMapper.toCustomer(customer);
    }
}
