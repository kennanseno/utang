package com.utang.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback SMS sender used when Twilio credentials are not configured. It logs the
 * message instead of delivering it, so the app runs end-to-end without an SMS gateway.
 */
public class LogSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LogSmsSender.class);

    @Override
    public void send(String toPhoneNumber, String message) {
        log.info("[SMS:log] to={} body={}", toPhoneNumber, message);
    }

    @Override
    public String provider() {
        return "log";
    }

    @Override
    public boolean isLive() {
        return false;
    }
}
