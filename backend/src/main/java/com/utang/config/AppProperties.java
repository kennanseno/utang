package com.utang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "utang")
public class AppProperties {

    /** Public base URL used to build customer payment links in reminder messages. */
    private String publicBaseUrl = "http://localhost:3000";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
