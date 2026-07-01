package com.utang.web;

import com.utang.domain.PaymentLink;
import com.utang.domain.Store;
import com.utang.dto.Dtos.CreatePaymentLinkRequest;
import com.utang.dto.Dtos.PaymentLinkResponse;
import com.utang.security.CurrentStore;
import com.utang.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Create a PayMongo payment link for a customer's balance (or a partial amount). */
    @PostMapping("/payments/link")
    public PaymentLinkResponse createLink(@CurrentStore Store store,
                                          @RequestBody CreatePaymentLinkRequest request) {
        PaymentLink link = paymentService.createLink(store.getId(), request.customerId(), request.amount());
        return new PaymentLinkResponse(link.getReferenceId(), link.getAmount(), link.getCheckoutUrl());
    }
}
