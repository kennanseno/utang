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

    /** In MVP the OTP is echoed back for easy testing (no SMS gateway). */
    public record RequestOtpResponse(String phoneNumber, String devCode, String message) {
    }

    public record VerifyOtpRequest(@NotBlank String phoneNumber, @NotBlank String code) {
    }

    public record AuthResponse(String token, StoreResponse store) {
    }

    public record StoreResponse(Long id, String phoneNumber, String name) {
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
