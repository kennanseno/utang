package com.utang.web;

import com.utang.domain.Customer;
import com.utang.domain.EntryType;
import com.utang.domain.Store;
import com.utang.dto.Dtos.CreditRequest;
import com.utang.dto.Dtos.CustomerResponse;
import com.utang.dto.Dtos.DebitRequest;
import com.utang.dto.Dtos.LedgerResponse;
import com.utang.security.CurrentStore;
import com.utang.service.CustomerService;
import com.utang.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LedgerController {

    private final LedgerService ledgerService;
    private final CustomerService customerService;

    public LedgerController(LedgerService ledgerService, CustomerService customerService) {
        this.ledgerService = ledgerService;
        this.customerService = customerService;
    }

    /** Add utang (DEBIT) — increases the customer's balance. */
    @PostMapping("/ledger/debit")
    public CustomerResponse debit(@CurrentStore Store store, @Valid @RequestBody DebitRequest request) {
        // Ownership check before mutating the ledger.
        Customer customer = customerService.get(store.getId(), request.customerId());
        ledgerService.applyEntry(customer.getId(), EntryType.DEBIT, request.amount(), request.note());
        return ApiMapper.toCustomer(customerService.get(store.getId(), customer.getId()));
    }

    /** Record bayad (CREDIT) — decreases the customer's balance. Used for CASH payments. */
    @PostMapping("/ledger/credit")
    public CustomerResponse credit(@CurrentStore Store store, @RequestBody CreditRequest request) {
        Customer customer = customerService.get(store.getId(), request.customerId());
        ledgerService.applyEntry(customer.getId(), EntryType.CREDIT, request.amount(), request.note());
        return ApiMapper.toCustomer(customerService.get(store.getId(), customer.getId()));
    }

    /** A page of the customer's ledger entries, newest first. */
    @GetMapping("/customers/{id}/ledger")
    public LedgerResponse ledger(@CurrentStore Store store, @PathVariable Long id,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        Customer customer = customerService.get(store.getId(), id);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<com.utang.domain.LedgerEntry> pageResult = ledgerService.history(customer.getId(), pageable);
        var entries = pageResult.getContent().stream()
                .map(ApiMapper::toLedgerEntry)
                .toList();
        return new LedgerResponse(
                customer.getId(),
                customer.getCurrentBalance(),
                entries,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.hasNext());
    }
}
