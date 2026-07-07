package com.utang.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.utang.domain.Customer;
import com.utang.domain.EntryType;
import com.utang.domain.Store;
import com.utang.dto.Dtos.PublicStatsResponse;
import com.utang.repository.StoreRepository;
import com.utang.service.CustomerService;
import com.utang.service.LedgerService;
import java.math.BigDecimal;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Exercises the public endpoints over the full HTTP stack, including the
 * {@link com.utang.security.RateLimitFilter}. A deliberately small window is
 * configured so the throttle can be triggered quickly and deterministically.
 *
 * <p>Methods share one per-IP rate-limit bucket, so the stats assertions run
 * first (before the bucket is deliberately exhausted by the throttle test).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "utang.rate-limit.public-per-window=5",
                "utang.rate-limit.window-seconds=60"
        })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PublicEndpointsIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private LedgerService ledgerService;

    @Test
    @Order(1)
    void stats_reflectSeededLedgerActivity() {
        PublicStatsResponse before = rest.getForObject("/public/stats", PublicStatsResponse.class);
        assertThat(before).isNotNull();

        Store store = storeRepository.save(new Store("0917" + System.nanoTime(), "Stats Store " + System.nanoTime()));
        Customer customer = customerService.create(store.getId(), "Juan", "09171110001");
        ledgerService.applyEntry(customer.getId(), EntryType.DEBIT, new BigDecimal("100.00"), "utang");
        ledgerService.applyEntry(customer.getId(), EntryType.CREDIT, new BigDecimal("30.00"), "bayad");

        PublicStatsResponse after = rest.getForObject("/public/stats", PublicStatsResponse.class);
        assertThat(after).isNotNull();

        assertThat(after.storeCount()).isEqualTo(before.storeCount() + 1);
        assertThat(after.customerCount()).isEqualTo(before.customerCount() + 1);
        assertThat(after.totalRecorded().subtract(before.totalRecorded()))
                .isEqualByComparingTo("100.00");
        assertThat(after.totalCollected().subtract(before.totalCollected()))
                .isEqualByComparingTo("30.00");
    }

    @Test
    @Order(2)
    void publicEndpoint_isRateLimitedPerIp() {
        // Fire more requests than the configured window allows; a 429 must appear.
        HttpStatus last = HttpStatus.OK;
        boolean sawSuccess = false;
        boolean sawThrottle = false;
        for (int i = 0; i < 20; i++) {
            ResponseEntity<String> response = rest.getForEntity("/public/stats", String.class);
            last = HttpStatus.valueOf(response.getStatusCode().value());
            if (last == HttpStatus.OK) {
                sawSuccess = true;
            }
            if (last == HttpStatus.TOO_MANY_REQUESTS) {
                sawThrottle = true;
                break;
            }
        }
        assertThat(sawSuccess).as("at least one request should succeed").isTrue();
        assertThat(sawThrottle).as("the limiter should eventually return 429").isTrue();
        assertThat(last).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
