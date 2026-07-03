package com.utang.email;

/**
 * Sends outbound transactional email (e.g. account verification codes).
 *
 * <p>Two implementations exist: {@link ResendEmailSender} for real delivery and
 * {@link LogEmailSender} which logs messages when no provider is configured.
 */
public interface EmailSender {

    /**
     * Delivers an email to the given address.
     *
     * @param toEmail recipient email address
     * @param subject message subject
     * @param body    plain-text message body
     */
    void send(String toEmail, String subject, String body);

    /** Provider identifier, e.g. {@code "resend"} or {@code "log"}. */
    String provider();

    /** Whether messages are actually delivered (true) or only logged (false). */
    boolean isLive();
}
