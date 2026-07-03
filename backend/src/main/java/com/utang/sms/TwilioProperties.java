package com.utang.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "utang.twilio")
public class TwilioProperties {

    /** Twilio Account SID. When blank, the log sender is used. */
    private String accountSid = "";

    /** Twilio Auth Token. */
    private String authToken = "";

    /** Sender phone number in E.164 format (e.g. +639171234567). */
    private String fromNumber = "";

    /** Optional Messaging Service SID (MG…). When set, it is used instead of fromNumber. */
    private String messagingServiceSid = "";

    /** Twilio REST API base URL. */
    private String baseUrl = "https://api.twilio.com/2010-04-01";

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getMessagingServiceSid() {
        return messagingServiceSid;
    }

    public void setMessagingServiceSid(String messagingServiceSid) {
        this.messagingServiceSid = messagingServiceSid;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return notBlank(accountSid) && notBlank(authToken)
                && (notBlank(fromNumber) || notBlank(messagingServiceSid));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
