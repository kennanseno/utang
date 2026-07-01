package com.utang.web;

import com.utang.domain.Customer;
import com.utang.domain.PaymentLink;
import com.utang.domain.Store;
import com.utang.dto.Dtos.PublicPayResponse;
import com.utang.error.NotFoundException;
import com.utang.repository.StoreRepository;
import com.utang.service.CustomerService;
import com.utang.service.PaymentService;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated payment page data (accessed via a customer's pay token). */
@RestController
public class PublicController {

    private final CustomerService customerService;
    private final StoreRepository storeRepository;
    private final PaymentService paymentService;

    public PublicController(CustomerService customerService,
                            StoreRepository storeRepository,
                            PaymentService paymentService) {
        this.customerService = customerService;
        this.storeRepository = storeRepository;
        this.paymentService = paymentService;
    }

    @GetMapping("/public/pay/{token}")
    public PublicPayResponse pay(@PathVariable String token) {
        Customer customer = customerService.getByPayToken(token);
        Store store = storeRepository.findById(customer.getStoreId())
                .orElseThrow(() -> new NotFoundException("Store not found"));

        BigDecimal balance = customer.getCurrentBalance();
        String checkoutUrl = null;
        if (balance.signum() > 0) {
            PaymentLink link = paymentService.createLink(store.getId(), customer.getId(), balance);
            checkoutUrl = link.getCheckoutUrl();
        }
        return new PublicPayResponse(store.getName(), customer.getName(), balance, checkoutUrl);
    }
}
