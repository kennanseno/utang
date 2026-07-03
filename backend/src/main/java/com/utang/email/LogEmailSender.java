package com.utang.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback email sender used when no provider is configured. It logs the message
 * instead of delivering it, so the app runs end-to-end without an email gateway.
 */
public class LogEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("[EMAIL:log] to={} subject={} body={}", toEmail, subject, body);
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
