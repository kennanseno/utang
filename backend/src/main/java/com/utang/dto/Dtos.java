package com.utang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request/response payloads for the Utang API. */
public final class Dtos {

    private Dtos() {
    }

    // ---- Auth ----

    /** Store owner registration: username, password, mobile number and store profile. */
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 60)
            @Pattern(regexp = "^[a-zA-Z0-9]+$",
                    message = "may only contain letters and numbers")
            String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank String phoneNumber,
            @NotBlank String storeName,
            String ownerName) {
    }

    /** Store owner login with username + password. */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** Completes a new-device login by verifying the OTP sent to the owner's mobile. */
    public record VerifyDeviceRequest(@NotBlank String username, @NotBlank String code) {
    }

    /** Successful authentication: an opaque session token plus the store profile. */
    public record AuthResponse(String token, StoreResponse store) {
    }

    /**
     * Login outcome. {@code status} is {@code AUTHENTICATED} (with {@code token} +
     * {@code store}) or {@code OTP_REQUIRED} (with a masked {@code phoneNumber}, and
     * {@code devCode} echoed only when SMS delivery is not configured).
     */
    public record LoginResponse(
            String status,
            String token,
            StoreResponse store,
            String phoneNumber,
            String devCode) {

        public static LoginResponse authenticated(String token, StoreResponse store) {
            return new LoginResponse("AUTHENTICATED", token, store, null, null);
        }

        public static LoginResponse otpRequired(String maskedPhone, String devCode) {
            return new LoginResponse("OTP_REQUIRED", null, null, maskedPhone, devCode);
        }
    }

    public record StoreResponse(
            Long id,
            String username,
            String phoneNumber,
            String name,
            String ownerName,
            boolean onboarded,
            boolean phoneVerified,
            boolean hasQrCode) {
    }

    /** Store profile update for an authenticated owner. */
    public record UpdateStoreRequest(
            @NotBlank String storeName,
            String ownerName,
            @NotBlank String phoneNumber) {
    }

    /** Confirms the OTP sent to the owner's mobile during phone verification. */
    public record PhoneVerificationRequest(@NotBlank String code) {
    }

    /**
     * Response to a phone verification request: the masked target number, plus a
     * {@code devCode} echoed only when SMS delivery is not configured.
     */
    public record PhoneVerificationResponse(String phoneNumber, String devCode) {
    }

    // ---- Customers ----
    public record CreateCustomerRequest(@NotBlank String name, @NotBlank String phoneNumber) {
    }

    public record UpdateCustomerRequest(@NotBlank String name, @NotBlank String phoneNumber) {
    }

    public record CustomerResponse(
            Long id,
            String name,
            String phoneNumber,
            BigDecimal currentBalance,
            String payToken) {
    }

    // ---- Ledger ----
    public record DebitRequest(Long customerId, BigDecimal amount, String note) {
    }

    public record CreditRequest(Long customerId, BigDecimal amount, String note) {
    }

    public record LedgerEntryResponse(
            Long id,
            String type,
            BigDecimal amount,
            String note,
            Instant createdAt) {
    }

    public record LedgerResponse(
            Long customerId,
            BigDecimal currentBalance,
            List<LedgerEntryResponse> entries) {
    }

    // ---- Reminders ----
    public record ReminderPreviewResponse(String message, boolean canSendToday) {
    }

    public record RemindResponse(String message, boolean sent) {
    }

    // ---- Public ----
    public record PublicPayResponse(
            String storeName,
            String customerName,
            BigDecimal outstandingBalance,
            boolean storeHasQrCode,
            List<PublicLedgerEntry> history) {
    }

    /** Result of a customer notifying the store owner that they have paid. */
    public record PaidNotificationResponse(boolean sent) {
    }

    /** A single transaction shown on the public payment page for transparency. */
    public record PublicLedgerEntry(
            String type,
            BigDecimal amount,
            String note,
            Instant createdAt) {
    }
}
