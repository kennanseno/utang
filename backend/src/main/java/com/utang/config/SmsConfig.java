package com.utang.config;

import com.utang.sms.LogSmsSender;
import com.utang.sms.SmsSender;
import com.utang.sms.TwilioProperties;
import com.utang.sms.TwilioSmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TwilioProperties.class)
public class SmsConfig {

    private static final Logger log = LoggerFactory.getLogger(SmsConfig.class);

    /**
     * Selects the Twilio sender when credentials are configured, otherwise falls back
     * to the log sender so the app runs end-to-end without an SMS gateway.
     */
    @Bean
    public SmsSender smsSender(TwilioProperties properties) {
        if (properties.isConfigured()) {
            log.info("Using Twilio SMS sender");
            return new TwilioSmsSender(properties);
        }
        log.warn("Twilio credentials not set — SMS messages will be logged, not delivered");
        return new LogSmsSender();
    }
}
