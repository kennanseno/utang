package com.utang.repository;

import com.utang.domain.Customer;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByStoreIdOrderByNameAsc(Long storeId);

    Optional<Customer> findByIdAndStoreId(Long id, Long storeId);

    Optional<Customer> findByPayToken(String payToken);

    /** Pessimistic lock so concurrent balance updates are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);
}
