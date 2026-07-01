package com.utang.repository;

import com.utang.domain.LedgerEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
