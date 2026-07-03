package com.utang.service;

import com.utang.config.AppProperties;
import com.utang.domain.OtpCode;
import com.utang.domain.Store;
import com.utang.domain.TrustedDevice;
import com.utang.error.ConflictException;
import com.utang.error.TooManyRequestsException;
import com.utang.error.UnauthorizedException;
import com.utang.repository.OtpCodeRepository;
import com.utang.repository.StoreRepository;
import com.utang.repository.TrustedDeviceRepository;
import com.utang.sms.SmsSender;
import com.utang.support.PhoneNumbers;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Store owner authentication.
 *
 * <p>Owners register with a username, password and mobile number. Login uses
 * username + password. Logging in from an unrecognized device additionally
 * requires an OTP sent to the owner's mobile number; once verified, the device
 * is trusted and future logins from it skip the OTP step.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Deliberately vague message for any registration uniqueness collision so we
     * don't disclose whether a username, store name or mobile number exists.
     */
    private static final String GENERIC_TAKEN_MESSAGE =
            "Some of those details are already in use. Please try a different "
                    + "username, store name, or mobile number.";

    private final OtpCodeRepository otpRepository;
    private final StoreRepository storeRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final SmsSender smsSender;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties.Otp otpProperties;

    public AuthService(
            OtpCodeRepository otpRepository,
            StoreRepository storeRepository,
            TrustedDeviceRepository trustedDeviceRepository,
            SmsSender smsSender,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.otpRepository = otpRepository;
        this.storeRepository = storeRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.smsSender = smsSender;
        this.passwordEncoder = passwordEncoder;
        this.otpProperties = appProperties.getOtp();
    }

    /** Registers a new store owner and trusts the registering device. */
    @Transactional
    public Store register(String username, String rawPassword, String phoneNumber,
                          String storeName, String ownerName, String deviceId) {
        String uname = normalizeUsername(username);
        if (storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("storeName is required");
        }
        String name = storeName.trim();
        String phone = normalizePhone(phoneNumber);

        // For security, never reveal which detail is already in use — that would let
        // an attacker enumerate existing usernames, store names or mobile numbers.
        // Any collision returns the same generic message.
        if (storeRepository.existsByUsername(uname)
                || storeRepository.existsByNameIgnoreCase(name)
                || storeRepository.existsByPhoneNumber(phone)) {
            throw new ConflictException(GENERIC_TAKEN_MESSAGE);
        }

        Store store = new Store(uname, passwordEncoder.encode(rawPassword), phone,
                name, blankToNull(ownerName));
        store.markOnboarded();
        store = storeRepository.save(store);
        trustDevice(store.getId(), deviceId);
        return store;
    }

    /** Outcome of a login attempt: either authenticated, or an OTP challenge is required. */
    public record LoginResult(boolean otpRequired, Store store, String devCode) {
    }

    /**
     * Validates credentials. On a trusted device the login completes immediately;
     * otherwise an OTP is sent to the owner's mobile and {@code otpRequired} is true.
     */
    @Transactional
    public LoginResult login(String username, String rawPassword, String deviceId) {
        Store store = storeRepository.findByUsername(normalizeUsername(username))
                .filter(s -> s.getPasswordHash() != null
                        && passwordEncoder.matches(rawPassword, s.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (isDeviceTrusted(store.getId(), deviceId)) {
            return new LoginResult(false, store, null);
        }
        String code = generateAndSendOtp(store.getPhoneNumber());
        return new LoginResult(true, store, code);
    }

    /** Verifies the new-device OTP, trusts the device, and completes login. */
    @Transactional
    public Store verifyDevice(String username, String code, String deviceId) {
        Store store = storeRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        consumeOtp(store.getPhoneNumber(), code);
        trustDevice(store.getId(), deviceId);
        return store;
    }

    /** Updates the store profile for an already-authenticated owner. */
    @Transactional
    public Store updateStore(Store store, String storeName, String ownerName, String phoneNumber) {
        if (storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("storeName is required");
        }
        String name = storeName.trim();
        if (storeRepository.existsByNameIgnoreCaseAndIdNot(name, store.getId())) {
            throw new ConflictException("Store name already taken");
        }
        String phone = normalizePhone(phoneNumber);
        if (!phone.equals(store.getPhoneNumber())) {
            if (storeRepository.existsByPhoneNumberAndIdNot(phone, store.getId())) {
                throw new ConflictException("Mobile number already registered");
            }
            store.setPhoneNumber(phone);
            // A new number must be re-verified.
            store.setPhoneVerified(false);
        }
        store.setName(name);
        store.setOwnerName(blankToNull(ownerName));
        return storeRepository.save(store);
    }

    /** Sends an OTP to the owner's mobile so they can verify they own the number. */
    @Transactional
    public String requestPhoneVerification(Store store) {
        return generateAndSendOtp(store.getPhoneNumber());
    }

    /** Confirms phone ownership via OTP, marks the number verified and trusts the device. */
    @Transactional
    public Store confirmPhoneVerification(Store store, String code, String deviceId) {
        consumeOtp(store.getPhoneNumber(), code);
        store.setPhoneVerified(true);
        store = storeRepository.save(store);
        trustDevice(store.getId(), deviceId);
        return store;
    }

    /** Allowed MIME types for the payment QR code image. */
    private static final java.util.Set<String> ALLOWED_QR_TYPES =
            java.util.Set.of("image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif");

    /** Maximum accepted QR image size in bytes (2 MB). */
    private static final int MAX_QR_BYTES = 2 * 1024 * 1024;

    /** Stores (or replaces) the owner's payment QR code image. */
    @Transactional
    public Store updateQrCode(Store store, byte[] image, String contentType) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("QR code image is required");
        }
        if (image.length > MAX_QR_BYTES) {
            throw new IllegalArgumentException("QR code image must be 2 MB or smaller");
        }
        String type = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!ALLOWED_QR_TYPES.contains(type)) {
            throw new IllegalArgumentException("QR code must be a PNG, JPG, WebP or GIF image");
        }
        // Normalize the odd but common "image/jpg" to the standard MIME type.
        if (type.equals("image/jpg")) {
            type = "image/jpeg";
        }
        storeRepository.updateQrCode(store.getId(), image, type);
        // Keep the (detached) entity consistent for the response we return.
        store.setQrCodeContentType(type);
        return store;
    }

    /** Removes the owner's payment QR code image. */
    @Transactional
    public Store removeQrCode(Store store) {
        storeRepository.updateQrCode(store.getId(), null, null);
        store.setQrCodeContentType(null);
        return store;
    }

    /** Loads the owner's QR image bytes on demand (never fetched for ordinary requests). */
    @Transactional(readOnly = true)
    public byte[] loadQrCodeImage(Store store) {
        return storeRepository.findQrCodeImageById(store.getId());
    }

    /** Whether OTPs are delivered by a real SMS gateway (true) or only logged (false). */
    public boolean isLiveSms() {
        return smsSender.isLive();
    }

    private boolean isDeviceTrusted(Long storeId, String deviceId) {
        return deviceId != null && !deviceId.isBlank()
                && trustedDeviceRepository.existsByStoreIdAndDeviceId(storeId, deviceId);
    }

    private void trustDevice(Long storeId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        if (!trustedDeviceRepository.existsByStoreIdAndDeviceId(storeId, deviceId.trim())) {
            trustedDeviceRepository.save(new TrustedDevice(storeId, deviceId.trim()));
        }
    }

    private String generateAndSendOtp(String phone) {
        enforceRateLimit(phone);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(otpProperties.getTtlMinutes(), ChronoUnit.MINUTES);
        otpRepository.save(new OtpCode(phone, code, expiresAt));
        smsSender.send(phone, "Your Utang verification code is " + code
                + ". It expires in " + otpProperties.getTtlMinutes() + " minutes.");
        return code;
    }

    private void consumeOtp(String phone, String code) {
        OtpCode otp = otpRepository
                .findFirstByPhoneNumberAndCodeAndConsumedFalseAndExpiresAtAfterOrderByIdDesc(
                        phone, code, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP"));
        otp.markConsumed();
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

    private static String normalizePhone(String phoneNumber) {
        return PhoneNumbers.toE164(phoneNumber);
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        return username.trim().toLowerCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
