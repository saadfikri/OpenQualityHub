package com.openqualityhub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$|^[0-9a-fA-F:]+$"
    );

    private final int authLimit;
    private final int exportLimit;
    private final int generateLimit;
    private final int defaultLimit;
    private final ConcurrentHashMap<String, SlidingWindow> windows = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(
            @Value("${app.rate-limit.auth-requests-per-minute:20}") int authLimit,
            @Value("${app.rate-limit.export-requests-per-minute:10}") int exportLimit,
            @Value("${app.rate-limit.generate-requests-per-minute:5}") int generateLimit,
            @Value("${app.rate-limit.default-requests-per-minute:60}") int defaultLimit) {
        this.authLimit = authLimit;
        this.exportLimit = exportLimit;
        this.generateLimit = generateLimit;
        this.defaultLimit = defaultLimit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = resolveLimit(path);
        String clientIp = resolveClientIp(request);
        String bucketKey = clientIp + ":" + resolveBucket(path);
        SlidingWindow window = windows.computeIfAbsent(bucketKey, k -> new SlidingWindow());

        if (!window.tryAcquire(limit)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", "Rate limit exceeded. Please try again later.",
                "timestamp", Instant.now().toString()
            )));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimit(String path) {
        if (path.startsWith("/api/v1/auth/")) return authLimit;
        if (path.startsWith("/api/v1/export")) return exportLimit;
        if (path.contains("/generate")) return generateLimit;
        return defaultLimit;
    }

    private String resolveBucket(String path) {
        if (path.startsWith("/api/v1/auth/")) return "auth";
        if (path.startsWith("/api/v1/export")) return "export";
        if (path.contains("/generate")) return "generate";
        return "default";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            String ip = xff.split(",")[0].trim();
            if (IP_PATTERN.matcher(ip).matches()) {
                return ip;
            }
            log.warn("Malformed IP in X-Forwarded-For header: {}", ip);
        }
        return request.getRemoteAddr();
    }

    private static class SlidingWindow {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean tryAcquire(int max) {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                synchronized (this) {
                    if (now - windowStart > 60_000) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }
            return count.incrementAndGet() <= max;
        }
    }
}
