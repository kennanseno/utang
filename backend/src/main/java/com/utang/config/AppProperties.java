package com.utang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "utang")
public class AppProperties {

    /** Public base URL used to build customer payment links in reminder messages. */
    private String publicBaseUrl = "http://localhost:3000";

    /** OTP generation and rate-limit settings. */
    private final Otp otp = new Otp();

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Otp getOtp() {
        return otp;
    }

    /** Settings for phone OTP: lifetime and per-number request throttling. */
    public static class Otp {

        /** How long a generated OTP stays valid. */
        private int ttlMinutes = 5;

        /** Minimum gap between OTP requests for the same number (anti-SMS-pumping). */
        private int resendCooldownSeconds = 60;

        /** Maximum OTP requests allowed per number within a rolling hour. */
        private int maxPerHour = 5;

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }

        public int getResendCooldownSeconds() {
            return resendCooldownSeconds;
        }

        public void setResendCooldownSeconds(int resendCooldownSeconds) {
            this.resendCooldownSeconds = resendCooldownSeconds;
        }

        public int getMaxPerHour() {
            return maxPerHour;
        }

        public void setMaxPerHour(int maxPerHour) {
            this.maxPerHour = maxPerHour;
        }
    }
}
