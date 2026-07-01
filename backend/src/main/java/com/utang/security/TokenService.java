package com.utang.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Issues and parses opaque session tokens.
 *
 * <p>MVP note: this encodes the store id in a base64 token rather than a signed JWT.
 * It is intentionally simple; harden (signing/expiry) before production.
 */
@Component
public class TokenService {

    private static final String PREFIX = "utang.";

    public String issue(Long storeId) {
        String raw = "store:" + storeId;
        return PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Long> resolveStoreId(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            return Optional.empty();
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(token.substring(PREFIX.length())),
                    StandardCharsets.UTF_8);
            if (!decoded.startsWith("store:")) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(decoded.substring("store:".length())));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
