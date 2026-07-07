package com.utang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "utang")
public class AppProperties {

    /** Public base URL used to build customer payment links in reminder messages. */
    private String publicBaseUrl = "http://localhost:3000";

    /** OTP generation and rate-limit settings. */
    private final Otp otp = new Otp();

    /** IP-based throttling for unauthenticated endpoints. */
    private final RateLimit rateLimit = new RateLimit();

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Otp getOtp() {
        return otp;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
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

    /**
     * Fixed-window, per-IP rate limit applied to unauthenticated endpoints
     * ({@code /public/**} and {@code /auth/**}) to blunt scraping and brute force.
     */
    public static class RateLimit {

        /** Master switch; disable in tests that need unthrottled HTTP access. */
        private boolean enabled = true;

        /** Max requests allowed per client IP within a single window. */
        private int publicPerWindow = 60;

        /** Length of the fixed window, in seconds. */
        private long windowSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPublicPerWindow() {
            return publicPerWindow;
        }

        public void setPublicPerWindow(int publicPerWindow) {
            this.publicPerWindow = publicPerWindow;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}