package com.utang.sms;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Twilio SMS adapter. Sends messages via the Twilio Messages REST API using a plain
 * HTTP client (no SDK dependency).
 *
 * @see <a href="https://www.twilio.com/docs/sms/api/message-resource">Twilio Messages</a>
 */
public class TwilioSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final TwilioProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TwilioSmsSender(TwilioProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(String toPhoneNumber, String message) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("recipient phone number is required");
        }
        String url = properties.getBaseUrl() + "/Accounts/" + properties.getAccountSid() + "/Messages.json";
        String form = "To=" + encode(toPhoneNumber) + "&" + senderParam() + "&Body=" + encode(message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", basicAuth(properties.getAccountSid(), properties.getAuthToken()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("Twilio SMS send failed ({}): {}", response.statusCode(), response.body());
                throw new IllegalStateException("Twilio SMS send failed: " + response.statusCode());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to reach Twilio", e);
        }
    }

    @Override
    public String provider() {
        return "twilio";
    }

    @Override
    public boolean isLive() {
        return true;
    }

    /**
     * Prefers a Messaging Service SID ({@code MessagingServiceSid}) when configured,
     * otherwise falls back to a sender number ({@code From}).
     */
    private String senderParam() {
        String messagingServiceSid = properties.getMessagingServiceSid();
        if (messagingServiceSid != null && !messagingServiceSid.isBlank()) {
            return "MessagingServiceSid=" + encode(messagingServiceSid);
        }
        return "From=" + encode(properties.getFromNumber());
    }

    private static String basicAuth(String accountSid, String authToken) {
        String token = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
