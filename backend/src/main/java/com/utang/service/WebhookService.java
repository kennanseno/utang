package com.utang.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utang.domain.EntryType;
import com.utang.domain.Payment;
import com.utang.domain.PaymentLink;
import com.utang.domain.PaymentMethod;
import com.utang.error.NotFoundException;
import com.utang.error.UnauthorizedException;
import com.utang.payment.PaymentGateway;
import com.utang.repository.PaymentLinkRepository;
import com.utang.repository.PaymentRepository;
import com.utang.repository.WebhookEventRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes PayMongo webhooks idempotently:
 * verify signature → record event (unique) → record payment (unique) →
 * append CREDIT ledger entry → update balance.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final PaymentGateway gateway;
    private final WebhookEventRepository webhookRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentLinkRepository paymentLinkRepository;
    private final LedgerService ledgerService;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebhookService(PaymentGateway gateway,
                          WebhookEventRepository webhookRepository,
                          PaymentRepository paymentRepository,
                          PaymentLinkRepository paymentLinkRepository,
                          LedgerService ledgerService) {
        this.gateway = gateway;
        this.webhookRepository = webhookRepository;
        this.paymentRepository = paymentRepository;
        this.paymentLinkRepository = paymentLinkRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public void handlePayMongoWebhook(String rawBody, String signatureHeader) {
        if (!gateway.verifySignature(rawBody, signatureHeader)) {
            throw new UnauthorizedException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = mapper.readTree(rawBody);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed webhook payload");
        }

        String provider = gateway.provider();
        JsonNode data = root.path("data");
        String eventId = data.path("id").asText(null);
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Missing webhook event id");
        }

        // Webhook-level idempotency: ignore events we've already stored.
        if (webhookRepository.existsByProviderAndExternalEventId(provider, eventId)) {
            log.info("Duplicate webhook {} ignored", eventId);
            return;
        }
        try {
            webhookRepository.save(new com.utang.domain.WebhookEvent(provider, eventId, rawBody));
        } catch (DataIntegrityViolationException race) {
            log.info("Concurrent duplicate webhook {} ignored", eventId);
            return;
        }

        JsonNode inner = data.path("attributes").path("data");
        JsonNode innerAttrs = inner.path("attributes");
        String referenceId = firstNonBlank(innerAttrs.path("remarks").asText(null), inner.path("id").asText(null));
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("Missing payment reference in webhook");
        }

        PaymentLink link = paymentLinkRepository.findByProviderAndReferenceId(provider, referenceId)
                .orElseThrow(() -> new NotFoundException("No payment link for reference " + referenceId));

        // Payment-level idempotency: credit the ledger at most once per reference.
        if (paymentRepository.findByProviderAndProviderRefId(provider, referenceId).isPresent()) {
            log.info("Payment {} already recorded — skipping", referenceId);
            return;
        }

        BigDecimal amount = extractAmount(innerAttrs).orElse(link.getAmount());
        paymentRepository.save(new Payment(
                link.getCustomerId(), amount, PaymentMethod.LINK, provider, referenceId));
        ledgerService.applyEntry(link.getCustomerId(), EntryType.CREDIT, amount, "PayMongo payment");

        log.info("Recorded PayMongo payment {} of {} for customer {}",
                referenceId, amount, link.getCustomerId());
    }

    private java.util.Optional<BigDecimal> extractAmount(JsonNode innerAttrs) {
        JsonNode amountNode = innerAttrs.path("amount");
        if (amountNode.isMissingNode() || amountNode.isNull() || !amountNode.canConvertToLong()) {
            return java.util.Optional.empty();
        }
        // PayMongo amounts are in centavos.
        return java.util.Optional.of(BigDecimal.valueOf(amountNode.asLong()).movePointLeft(2));
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
