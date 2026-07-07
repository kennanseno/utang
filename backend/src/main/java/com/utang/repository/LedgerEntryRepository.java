package com.utang.repository;

import com.utang.domain.EntryType;
import com.utang.domain.LedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<LedgerEntry> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    void deleteByCustomerId(Long customerId);

    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.type = :type")
    BigDecimal sumAmountByType(@Param("type") EntryType type);
}
