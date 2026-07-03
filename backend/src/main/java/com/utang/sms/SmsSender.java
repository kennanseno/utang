package com.utang.sms;

/**
 * Sends outbound SMS messages (OTP codes, payment reminders, etc.).
 *
 * <p>Two implementations exist: {@link TwilioSmsSender} for real delivery and
 * {@link LogSmsSender} which logs messages when Twilio credentials are absent.
 */
public interface SmsSender {

    /**
     * Delivers a text message to the given phone number.
     *
     * @param toPhoneNumber recipient in E.164 format (e.g. {@code +639171234567})
     * @param message       message body
     */
    void send(String toPhoneNumber, String message);

    /** Provider identifier, e.g. {@code "twilio"} or {@code "log"}. */
    String provider();

    /** Whether messages are actually delivered to a carrier (true) or only logged (false). */
    boolean isLive();
}
