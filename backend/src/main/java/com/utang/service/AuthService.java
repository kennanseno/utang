package com.utang.service;

import com.utang.config.AppProperties;
import com.utang.domain.OtpCode;
import com.utang.domain.Store;
import com.utang.email.EmailSender;
import com.utang.error.ConflictException;
import com.utang.error.TooManyRequestsException;
import com.utang.error.UnauthorizedException;
import com.utang.repository.OtpCodeRepository;
import com.utang.repository.StoreRepository;
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
 * <p>Owners register with a username, password, email and mobile number. Login uses
 * username + password only. The email address is verified once (via a code emailed
 * to the owner) so it can be trusted for account recovery; the mobile number is kept
 * as an unverified contact used for the suki-facing reminder/call features.
 */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Deliberately vague message for any registration uniqueness collision so we
     * don't disclose whether a username, store name, email or mobile number exists.
     */
    private static final String GENERIC_TAKEN_MESSAGE =
            "Some of those details are already in use. Please try a different "
                    + "username, store name, email, or mobile number.";

    private final OtpCodeRepository otpRepository;
    private final StoreRepository storeRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties.Otp otpProperties;

    public AuthService(
            OtpCodeRepository otpRepository,
            StoreRepository storeRepository,
            EmailSender emailSender,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.otpRepository = otpRepository;
        this.storeRepository = storeRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.otpProperties = appProperties.getOtp();
    }

    /** Registers a new store owner. */
    @Transactional
    public Store register(String username, String rawPassword, String phoneNumber,
                          String email, String storeName, String ownerName) {
        String uname = normalizeUsername(username);
        if (storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("storeName is required");
        }
        String name = storeName.trim();
        String phone = normalizePhone(phoneNumber);
        String mail = normalizeEmail(email);

        // For security, never reveal which detail is already in use — that would let
        // an attacker enumerate existing accounts. Any collision returns the same
        // generic message.
        if (storeRepository.existsByUsername(uname)
                || storeRepository.existsByNameIgnoreCase(name)
                || storeRepository.existsByPhoneNumber(phone)
                || storeRepository.existsByEmail(mail)) {
            throw new ConflictException(GENERIC_TAKEN_MESSAGE);
        }

        Store store = new Store(uname, passwordEncoder.encode(rawPassword), phone, mail,
                name, blankToNull(ownerName));
        store.markOnboarded();
        return storeRepository.save(store);
    }

    /** Validates credentials and completes login (password-only, no per-device code). */
    @Transactional(readOnly = true)
    public Store login(String username, String rawPassword) {
        return storeRepository.findByUsername(normalizeUsername(username))
                .filter(s -> s.getPasswordHash() != null
                        && passwordEncoder.matches(rawPassword, s.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
    }

    /** Updates the store profile for an already-authenticated owner. */
    @Transactional
    public Store updateStore(Store store, String storeName, String ownerName,
                             String phoneNumber, String email) {
        if (storeName == null || storeName.isBlank()) {
            throw new IllegalArgumentException("storeName is required");
        }
        String name = storeName.trim();
        if (storeRepository.existsByNameIgnoreCaseAndIdNot(name, store.getId())) {
            throw new ConflictException("Store name already taken");
        }
        String phone = normalizePhone(phoneNumber);
        if (!phone.equals(store.getPhoneNumber())
                && storeRepository.existsByPhoneNumberAndIdNot(phone, store.getId())) {
            throw new ConflictException("Mobile number already registered");
        }
        String mail = normalizeEmail(email);
        if (!mail.equals(store.getEmail())) {
            if (storeRepository.existsByEmailAndIdNot(mail, store.getId())) {
                throw new ConflictException("Email already registered");
            }
            store.setEmail(mail);
            // A new email must be re-verified.
            store.setEmailVerified(false);
        }
        store.setPhoneNumber(phone);
        store.setName(name);
        store.setOwnerName(blankToNull(ownerName));
        return storeRepository.save(store);
    }

    /** Sends a code to the owner's email so they can verify they own the address. */
    @Transactional
    public String requestEmailVerification(Store store) {
        return generateAndSendCode(store.getEmail());
    }

    /** Confirms email ownership via the code and marks the address verified. */
    @Transactional
    public Store confirmEmailVerification(Store store, String code) {
        consumeCode(store.getEmail(), code);
        store.setEmailVerified(true);
        return storeRepository.save(store);
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

    /** Whether codes are delivered by a real email gateway (true) or only logged (false). */
    public boolean isLiveEmail() {
        return emailSender.isLive();
    }

    private String generateAndSendCode(String email) {
        enforceRateLimit(email);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(otpProperties.getTtlMinutes(), ChronoUnit.MINUTES);
        otpRepository.save(new OtpCode(email, code, expiresAt));
        emailSender.send(email, "Your Utang verification code",
                "Your Utang verification code is " + code
                        + ". It expires in " + otpProperties.getTtlMinutes() + " minutes.");
        return code;
    }

    private void consumeCode(String email, String code) {
        OtpCode otp = otpRepository
                .findFirstByEmailAndCodeAndConsumedFalseAndExpiresAtAfterOrderByIdDesc(
                        email, code, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired code"));
        otp.markConsumed();
    }

    private void enforceRateLimit(String email) {
        Instant now = Instant.now();
        Instant cooldownThreshold = now.minusSeconds(otpProperties.getResendCooldownSeconds());
        if (otpRepository.existsByEmailAndCreatedAtAfter(email, cooldownThreshold)) {
            throw new TooManyRequestsException("Please wait a moment before requesting another code");
        }
        Instant hourAgo = now.minus(1, ChronoUnit.HOURS);
        if (otpRepository.countByEmailAndCreatedAtAfter(email, hourAgo) >= otpProperties.getMaxPerHour()) {
            throw new TooManyRequestsException("Too many code requests. Please try again later");
        }
    }

    private static String normalizePhone(String phoneNumber) {
        return PhoneNumbers.toE164(phoneNumber);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        return email.trim().toLowerCase();
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
