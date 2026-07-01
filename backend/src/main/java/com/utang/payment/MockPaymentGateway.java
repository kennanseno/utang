package com.utang.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock payment gateway used when no PayMongo secret key is configured.
 * Lets the full flow (link + webhook) be exercised locally without external calls.
 */
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    private final PayMongoProperties properties;

    public MockPaymentGateway(PayMongoProperties properties) {
        this.properties = properties;
    }

    @Override
    public String provider() {
        return "paymongo";
    }

    @Override
    public PaymentLinkResult createPaymentLink(BigDecimal amount, String description) {
        String referenceId = "mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String checkoutUrl = "https://mock.paymongo.local/checkout/" + referenceId;
        log.info("[MOCK] Created payment link {} for {} ({})", referenceId, amount, description);
        return PaymentLinkResult.of(referenceId, checkoutUrl);
    }

    @Override
    public boolean verifySignature(String rawBody, String signatureHeader) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            // No secret configured in mock mode — accept for local testing.
            return true;
        }
        if (signatureHeader == null) {
            return false;
        }
        String expected = hmacSha256(rawBody, properties.getWebhookSecret());
        return constantTimeEquals(expected, signatureHeader.trim());
    }

    private static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
