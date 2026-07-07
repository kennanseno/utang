package com.utang.security;

import com.utang.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A dependency-free, in-memory fixed-window rate limiter for the unauthenticated
 * surface ({@code /public/**} and {@code /auth/**}). Keyed by client IP so a single
 * host cannot scrape the public stats/pay pages or brute-force login at will.
 *
 * <p>State is per-instance and non-durable; for a multi-node deployment this should be
 * backed by a shared store (e.g. Redis). It is intentionally simple for the v1 single node.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Safety valve: if the map grows unreasonably large, drop it rather than leak memory. */
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final AppProperties properties;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.getRateLimit().isEnabled()) {
            return true;
        }
        // CORS preflight carries no credentials and should never be throttled.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/public/") || path.startsWith("/auth/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (allow(clientKey(request))) {
            chain.doFilter(request, response);
        } else {
            writeTooManyRequests(response);
        }
    }

    private boolean allow(String clientId) {
        long windowSeconds = Math.max(1, properties.getRateLimit().getWindowSeconds());
        int limit = properties.getRateLimit().getPublicPerWindow();
        long currentWindow = Instant.now().getEpochSecond() / windowSeconds;

        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.clear();
        }

        Window window = windows.compute(clientId, (key, existing) ->
                (existing == null || existing.windowId != currentWindow)
                        ? new Window(currentWindow)
                        : existing);

        return window.count.incrementAndGet() <= limit;
    }

    /** Prefer the left-most X-Forwarded-For hop when behind a proxy, else the socket address. */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER,
                String.valueOf(Math.max(1, properties.getRateLimit().getWindowSeconds())));
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\","
                + "\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Too many requests. Please slow down and try again shortly.\"}");
    }

    /** One fixed window of request counts for a single client. */
    private static final class Window {
        private final long windowId;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long windowId) {
            this.windowId = windowId;
        }
    }
}
