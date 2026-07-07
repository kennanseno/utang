package com.utang.web;

import com.utang.domain.Customer;
import com.utang.domain.LedgerEntry;
import com.utang.domain.Store;
import com.utang.dto.Dtos.PublicLedgerEntry;
import com.utang.dto.Dtos.PublicPayResponse;
import com.utang.error.NotFoundException;
import com.utang.repository.StoreRepository;
import com.utang.service.CustomerService;
import com.utang.service.LedgerService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated payment page data (accessed via a customer's pay token). */
@RestController
public class PublicController {

    private final CustomerService customerService;
    private final StoreRepository storeRepository;
    private final LedgerService ledgerService;

    public PublicController(CustomerService customerService,
                            StoreRepository storeRepository,
                            LedgerService ledgerService) {
        this.customerService = customerService;
        this.storeRepository = storeRepository;
        this.ledgerService = ledgerService;
    }

    @GetMapping("/public/pay/{token}")
    public PublicPayResponse pay(@PathVariable String token,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        Customer customer = customerService.getByPayToken(token);
        Store store = storeRepository.findById(customer.getStoreId())
                .orElseThrow(() -> new NotFoundException("Store not found"));

        BigDecimal balance = customer.getCurrentBalance();

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<LedgerEntry> pageResult = ledgerService.history(customer.getId(), pageable);
        List<PublicLedgerEntry> history = pageResult.getContent().stream()
                .map(PublicController::toPublicEntry)
                .toList();

        return new PublicPayResponse(
                store.getName(), store.getPhoneNumber(), customer.getName(), balance,
                store.hasQrCode(), history,
                pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.hasNext());
    }

    /** Serves the store's uploaded payment QR code so the customer can scan and pay. */
    @GetMapping("/public/pay/{token}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable String token) {
        Customer customer = customerService.getByPayToken(token);
        Store store = storeRepository.findById(customer.getStoreId())
                .orElseThrow(() -> new NotFoundException("Store not found"));
        if (!store.hasQrCode()) {
            return ResponseEntity.notFound().build();
        }
        byte[] image = storeRepository.findQrCodeImageById(store.getId());
        if (image == null || image.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(store.getQrCodeContentType()))
                .body(image);
    }

    private static PublicLedgerEntry toPublicEntry(LedgerEntry e) {
        return new PublicLedgerEntry(e.getType().name(), e.getAmount(), e.getNote(), e.getCreatedAt());
    }
}
