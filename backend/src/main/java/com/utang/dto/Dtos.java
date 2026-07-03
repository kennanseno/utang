package com.utang.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request/response payloads for the Utang API. */
public final class Dtos {

    private Dtos() {
    }

    // ---- Auth ----
    public record RequestOtpRequest(@NotBlank String phoneNumber) {
    }

    /**
     * Response to an OTP request. When SMS delivery is not configured, {@code devCode}
     * echoes the generated code for testing; in live mode it is {@code null}.
     */
    public record RequestOtpResponse(String phoneNumber, String devCode, String message) {
    }

    public record VerifyOtpRequest(@NotBlank String phoneNumber, @NotBlank String code) {
    }

    /** {@code onboarded} is false for a store that still needs to complete onboarding. */
    public record AuthResponse(String token, boolean onboarded, StoreResponse store) {
    }

    /** Store owner onboarding profile. */
    public record OnboardingRequest(@NotBlank String storeName, String ownerName) {
    }

    public record StoreResponse(
            Long id,
            String phoneNumber,
            String name,
            String ownerName,
            boolean onboarded) {
    }

    // ---- Customers ----
    public record CreateCustomerRequest(@NotBlank String name, String phoneNumber) {
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

    // ---- Payments ----
    public record CreatePaymentLinkRequest(Long customerId, BigDecimal amount) {
    }

    public record PaymentLinkResponse(String referenceId, BigDecimal amount, String checkoutUrl) {
    }

    // ---- Public ----
    public record PublicPayResponse(
            String storeName,
            String customerName,
            BigDecimal outstandingBalance,
            String checkoutUrl) {
    }
}
