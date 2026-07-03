package com.utang.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resend email adapter. Sends messages via the Resend REST API using a plain HTTP
 * client (no SDK dependency).
 *
 * @see <a href="https://resend.com/docs/api-reference/emails/send-email">Resend Send Email</a>
 */
public class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final ResendProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResendEmailSender(ResendProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("recipient email is required");
        }

        String payload = buildPayload(toEmail, subject, body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/emails"))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("Resend email send failed ({}): {}", response.statusCode(), response.body());
                throw new IllegalStateException("Resend email send failed: " + response.statusCode());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to reach Resend", e);
        }
    }

    @Override
    public String provider() {
        return "resend";
    }

    @Override
    public boolean isLive() {
        return true;
    }

    private String buildPayload(String toEmail, String subject, String body) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("from", properties.getFromEmail());
        node.putArray("to").add(toEmail);
        node.put("subject", subject);
        node.put("text", body);
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize email payload", e);
        }
    }
}
