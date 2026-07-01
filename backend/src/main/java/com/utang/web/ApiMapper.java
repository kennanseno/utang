package com.utang.web;

import com.utang.domain.Customer;
import com.utang.domain.LedgerEntry;
import com.utang.dto.Dtos.CustomerResponse;
import com.utang.dto.Dtos.LedgerEntryResponse;

/** Maps domain entities to API response DTOs. */
final class ApiMapper {

    private ApiMapper() {
    }

    static CustomerResponse toCustomer(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getName(), c.getPhoneNumber(), c.getCurrentBalance(), c.getPayToken());
    }

    static LedgerEntryResponse toLedgerEntry(LedgerEntry e) {
        return new LedgerEntryResponse(
                e.getId(), e.getType().name(), e.getAmount(), e.getNote(), e.getCreatedAt());
    }
}
