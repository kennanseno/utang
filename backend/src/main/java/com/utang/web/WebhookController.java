package com.utang.web;

import com.utang.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * PayMongo webhook receiver. Reads the raw body (needed for signature
     * verification) and processes the event idempotently.
     */
    @PostMapping("/webhooks/paymongo")
    public ResponseEntity<Void> paymongo(
            @RequestBody String rawBody,
            @RequestHeader(value = "Paymongo-Signature", required = false) String signature) {
        webhookService.handlePayMongoWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
