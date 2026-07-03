package com.utang.dto;

import jakarta.validation.constraints.Email;
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

    /** Store owner registration: username, password, mobile number, email and store profile. */
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 60)
            @Pattern(regexp = "^[a-zA-Z0-9]+$",
                    message = "may only contain letters and numbers")
            String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank String phoneNumber,
            @NotBlank @Email String email,
            @NotBlank String storeName,
            String ownerName) {
    }

    /** Store owner login with username + password. */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** Successful authentication: an opaque session token plus the store profile. */
    public record AuthResponse(String token, StoreResponse store) {
    }

    public record StoreResponse(
            Long id,
            String username,
            String phoneNumber,
            String email,
            String name,
            String ownerName,
            boolean onboarded,
            boolean emailVerified,
            boolean hasQrCode) {
    }

    /** Store profile update for an authenticated owner. */
    public record UpdateStoreRequest(
            @NotBlank String storeName,
            String ownerName,
            @NotBlank String phoneNumber,
            @NotBlank @Email String email) {
    }

    /** Confirms the code emailed to the owner during email verification. */
    public record EmailVerificationRequest(@NotBlank String code) {
    }

    /**
     * Response to an email verification request: the masked target address, plus a
     * {@code devCode} echoed only when email delivery is not configured.
     */
    public record EmailVerificationResponse(String email, String devCode) {
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
    public record DebitRequest(Long customerId, BigDecimal amount, @NotBlank String note) {
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
            List<LedgerEntryResponse> entries,
            int page,
            int size,
            long totalEntries,
            boolean hasMore) {
    }

    // ---- Reminders ----
    public record ReminderPreviewResponse(String message) {
    }

    // ---- Public ----
    public record PublicPayResponse(
            String storeName,
            String storePhoneNumber,
            String customerName,
            BigDecimal outstandingBalance,
            boolean storeHasQrCode,
            List<PublicLedgerEntry> history,
            int page,
            int size,
            long totalHistory,
            boolean hasMore) {
    }

    /** A single transaction shown on the public payment page for transparency. */
    public record PublicLedgerEntry(
            String type,
            BigDecimal amount,
            String note,
            Instant createdAt) {
    }
}
