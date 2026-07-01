package com.utang.service;

import com.utang.domain.Customer;
import com.utang.domain.PaymentLink;
import com.utang.error.NotFoundException;
import com.utang.payment.PaymentGateway;
import com.utang.payment.PaymentLinkResult;
import com.utang.repository.CustomerRepository;
import com.utang.repository.PaymentLinkRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates PayMongo payment links and links them to a customer so the webhook
 * can resolve which customer to credit.
 */
@Service
public class PaymentService {

    private final PaymentGateway paymentGateway;
    private final CustomerRepository customerRepository;
    private final PaymentLinkRepository paymentLinkRepository;

    public PaymentService(PaymentGateway paymentGateway,
                          CustomerRepository customerRepository,
                          PaymentLinkRepository paymentLinkRepository) {
        this.paymentGateway = paymentGateway;
        this.customerRepository = customerRepository;
        this.paymentLinkRepository = paymentLinkRepository;
    }

    @Transactional
    public PaymentLink createLink(Long storeId, Long customerId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        Customer customer = customerRepository.findByIdAndStoreId(customerId, storeId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));

        String description = "Utang payment - " + customer.getName();
        PaymentLinkResult result = paymentGateway.createPaymentLink(amount, description);

        PaymentLink link = new PaymentLink(
                customer.getId(), result.referenceId(), amount, result.checkoutUrl());
        return paymentLinkRepository.save(link);
    }
}
