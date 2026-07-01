package com.utang.repository;

import com.utang.domain.PaymentLink;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {

    Optional<PaymentLink> findByProviderAndReferenceId(String provider, String referenceId);
}
