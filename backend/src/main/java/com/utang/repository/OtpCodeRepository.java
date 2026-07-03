package com.utang.repository;

import com.utang.domain.OtpCode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByPhoneNumberAndCodeAndConsumedFalseAndExpiresAtAfterOrderByIdDesc(
            String phoneNumber, String code, Instant now);

    boolean existsByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant threshold);

    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant threshold);
}
