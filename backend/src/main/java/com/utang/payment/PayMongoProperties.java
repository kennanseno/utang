package com.utang.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "utang.paymongo")
public class PayMongoProperties {

    /** PayMongo secret key. When blank, the mock gateway is used. */
    private String secretKey = "";

    /** Shared secret used to verify webhook signatures. */
    private String webhookSecret = "";

    /** PayMongo API base URL. */
    private String baseUrl = "https://api.paymongo.com/v1";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }
}
