package com.utang.service;

import com.utang.config.AppProperties;
import com.utang.domain.OtpCode;
import com.utang.domain.Store;
import com.utang.error.TooManyRequestsException;
import com.utang.error.UnauthorizedException;
import com.utang.repository.OtpCodeRepository;
import com.utang.repository.StoreRepository;
import com.utang.sms.SmsSender;
import com.utang.support.PhoneNumbers;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phone + OTP authentication. The OTP is delivered by SMS (Twilio) when configured;
 * otherwise it is logged and echoed back for testing. Verifying an OTP creates the
 * store account on first login, after which the owner completes onboarding.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpRepository;
    private final StoreRepository storeRepository;
    private final SmsSender smsSender;
    private final AppProperties.Otp otpProperties;

    public AuthService(
            OtpCodeRepository otpRepository,
            StoreRepository storeRepository,
            SmsSender smsSender,
            AppProperties appProperties) {
        this.otpRepository = otpRepository;
        this.storeRepository = storeRepository;
        this.smsSender = smsSender;
        this.otpProperties = appProperties.getOtp();
    }

    @Transactional
    public String requestOtp(String phoneNumber) {
        String phone = normalize(phoneNumber);
        enforceRateLimit(phone);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(otpProperties.getTtlMinutes(), ChronoUnit.MINUTES);
        otpRepository.save(new OtpCode(phone, code, expiresAt));
        smsSender.send(phone, "Your Utang verification code is " + code
                + ". It expires in " + otpProperties.getTtlMinutes() + " minutes.");
        return code;
    }

    private void enforceRateLimit(String phone) {
        Instant now = Instant.now();
        Instant cooldownThreshold = now.minusSeconds(otpProperties.getResendCooldownSeconds());
        if (otpRepository.existsByPhoneNumberAndCreatedAtAfter(phone, cooldownThreshold)) {
            throw new TooManyRequestsException("Please wait a moment before requesting another code");
        }
        Instant hourAgo = now.minus(1, ChronoUnit.HOURS);
        if (otpRepository.countByPhoneNumberAndCreatedAtAfter(phone, hourAgo) >= otpProperties.getMaxPerHour()) {
            throw new TooManyRequestsException("Too many code requests. Please try again later");
        }
    }

    /** Whether OTPs are delivered by a real SMS gateway (true) or only logged (false). */
    public boolean isLiveSms() {
        return smsSender.isLive();
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

    /** Completes store owner onboarding by setting the store profile. */
    @Transactional
    public Store onboard(Store store, String storeName, String ownerName) {
        if (storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("storeName is required");
        }
        store.setName(storeName.trim());
        store.setOwnerName(ownerName == null || ownerName.isBlank() ? null : ownerName.trim());
        store.markOnboarded();
        return storeRepository.save(store);
    }

    private static String normalize(String phoneNumber) {
        return PhoneNumbers.toE164(phoneNumber);
    }
}
