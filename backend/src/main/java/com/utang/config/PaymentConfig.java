package com.utang.config;

import com.utang.payment.MockPaymentGateway;
import com.utang.payment.PayMongoGateway;
import com.utang.payment.PayMongoProperties;
import com.utang.payment.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PayMongoProperties.class)
public class PaymentConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfig.class);

    /**
     * Selects the real PayMongo adapter when a secret key is configured, otherwise
     * falls back to the mock adapter so the app runs end-to-end without credentials.
     */
    @Bean
    public PaymentGateway paymentGateway(PayMongoProperties properties) {
        if (properties.isConfigured()) {
            log.info("Using PayMongo payment gateway");
            return new PayMongoGateway(properties);
        }
        log.warn("PAYMONGO_SECRET_KEY not set — using MOCK payment gateway");
        return new MockPaymentGateway(properties);
    }
}
