package com.utang.repository;

import com.utang.domain.Store;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByPhoneNumber(String phoneNumber);
}
