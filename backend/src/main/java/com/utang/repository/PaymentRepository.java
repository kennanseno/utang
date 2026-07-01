package com.utang.repository;

import com.utang.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderAndProviderRefId(String provider, String providerRefId);
}
