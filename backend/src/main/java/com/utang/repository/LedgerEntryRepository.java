package com.utang.repository;

import com.utang.domain.LedgerEntry;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<LedgerEntry> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    void deleteByCustomerId(Long customerId);
}
