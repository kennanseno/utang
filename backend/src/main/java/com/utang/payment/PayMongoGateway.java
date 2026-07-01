package com.utang.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PayMongo adapter. Creates hosted payment links via the PayMongo Links API and
 * verifies webhook signatures. Used only when a secret key is configured.
 *
 * @see <a href="https://developers.paymongo.com/reference/links-resource">PayMongo Links</a>
 */
public class PayMongoGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PayMongoGateway.class);

    private final PayMongoProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public PayMongoGateway(PayMongoProperties properties) {
        this.properties = properties;
    }

    @Override
    public String provider() {
        return "paymongo";
    }

    @Override
    public PaymentLinkResult createPaymentLink(BigDecimal amount, String description) {
        // PayMongo expects the amount in centavos.
        long centavos = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String referenceNumber = "utang_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        String body = """
                {"data":{"attributes":{"amount":%d,"description":%s,"remarks":%s}}}
                """.formatted(centavos, jsonString(description), jsonString(referenceNumber));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/links"))
                .header("Authorization", basicAuth(properties.getSecretKey()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("PayMongo link creation failed ({}): {}", response.statusCode(), response.body());
                throw new IllegalStateException("PayMongo link creation failed: " + response.statusCode());
            }
            JsonNode data = mapper.readTree(response.body()).path("data");
            String referenceId = data.path("id").asText(referenceNumber);
            String checkoutUrl = data.path("attributes").path("checkout_url").asText();
            return PaymentLinkResult.of(referenceId, checkoutUrl);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to reach PayMongo", e);
        }
    }

    @Override
    public boolean verifySignature(String rawBody, String signatureHeader) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank() || signatureHeader == null) {
            return false;
        }
        // PayMongo signature header format: "t=<timestamp>,te=<test-sig>,li=<live-sig>".
        String timestamp = null;
        String expectedSig = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            switch (kv[0].trim()) {
                case "t" -> timestamp = kv[1].trim();
                case "te", "li" -> expectedSig = kv[1].trim();
                default -> { }
            }
        }
        if (timestamp == null || expectedSig == null) {
            return false;
        }
        String signedPayload = timestamp + "." + rawBody;
        String computed = hmacSha256Hex(signedPayload, properties.getWebhookSecret());
        return constantTimeEquals(computed, expectedSig);
    }

    private static String basicAuth(String secretKey) {
        String token = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private static String jsonString(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String hmacSha256Hex(String data, String key) {
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
