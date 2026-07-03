package com.utang.repository;

import com.utang.domain.OtpCode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByEmailAndCodeAndConsumedFalseAndExpiresAtAfterOrderByIdDesc(
            String email, String code, Instant now);

    boolean existsByEmailAndCreatedAtAfter(String email, Instant threshold);

    long countByEmailAndCreatedAtAfter(String email, Instant threshold);
}
