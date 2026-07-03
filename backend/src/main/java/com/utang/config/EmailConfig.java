package com.utang.config;

import com.utang.email.EmailSender;
import com.utang.email.LogEmailSender;
import com.utang.email.ResendEmailSender;
import com.utang.email.ResendProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResendProperties.class)
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    /**
     * Selects the Resend sender when an API key is configured, otherwise falls back
     * to the log sender so the app runs end-to-end without an email gateway.
     */
    @Bean
    public EmailSender emailSender(ResendProperties properties) {
        if (properties.isConfigured()) {
            log.info("Using Resend email sender");
            return new ResendEmailSender(properties);
        }
        log.warn("Resend not configured — verification emails will be logged, not delivered");
        return new LogEmailSender();
    }
}
