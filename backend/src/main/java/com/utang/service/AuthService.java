package com.utang.service;

import com.utang.domain.OtpCode;
import com.utang.domain.Store;
import com.utang.error.UnauthorizedException;
import com.utang.repository.OtpCodeRepository;
import com.utang.repository.StoreRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phone + OTP authentication. In MVP the OTP is generated and returned to the
 * client (no SMS gateway). Verifying an OTP creates the store account on first login.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_TTL_MINUTES = 5;

    private final OtpCodeRepository otpRepository;
    private final StoreRepository storeRepository;

    public AuthService(OtpCodeRepository otpRepository, StoreRepository storeRepository) {
        this.otpRepository = otpRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional
    public String requestOtp(String phoneNumber) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES);
        otpRepository.save(new OtpCode(normalize(phoneNumber), code, expiresAt));
        return code;
    }

    /** Verifies the OTP and returns the store (creating it on first successful login). */
    @Transactional
    public Store verifyOtp(String phoneNumber, String code) {
        String phone = normalize(phoneNumber);
        OtpCode otp = otpRepository
                .findFirstByPhoneNumberAndCodeAndConsumedFalseAndExpiresAtAfterOrderByIdDesc(
                        phone, code, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP"));
        otp.markConsumed();

        return storeRepository.findByPhoneNumber(phone)
                .orElseGet(() -> storeRepository.save(new Store(phone, "My Store")));
    }

    private static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        return phoneNumber.trim();
    }
}
