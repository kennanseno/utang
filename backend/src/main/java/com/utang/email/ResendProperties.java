package com.utang.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the Resend transactional email API (https://resend.com). */
@ConfigurationProperties(prefix = "utang.resend")
public class ResendProperties {

    /** Resend API key (starts with {@code re_}). When blank, the log sender is used. */
    private String apiKey = "";

    /**
     * Verified "from" address, e.g. {@code Utang <no-reply@yourdomain.com>}. Must be a
     * domain (or the Resend onboarding address) verified in your Resend account.
     */
    private String fromEmail = "";

    /** Resend REST API base URL. */
    private String baseUrl = "https://api.resend.com";

    /** True once an API key and sender address are configured. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && fromEmail != null && !fromEmail.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
