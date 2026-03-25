package com.unibond.common.ratelimit;

import com.unibond.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String key = resolveKey(req);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(req.getRequestURI()));

        if (bucket.tryConsume(1)) {
            return true;
        }

        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(
            ErrorResponse.of("RATE_LIMIT_EXCEEDED", "请求频率超限")));
        return false;
    }

    private String resolveKey(HttpServletRequest req) {
        // Use userId if authenticated, otherwise IP
        var auth = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.unibond.common.security.UserPrincipal p) {
            return "user:" + p.userId() + ":" + req.getRequestURI();
        }
        return "ip:" + req.getRemoteAddr() + ":" + req.getRequestURI();
    }

    private Bucket createBucket(String uri) {
        if (uri.contains("/auth/email/send")) {
            return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(60)).build())
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofHours(1)).build())
                .build();
        }
        if (uri.contains("/auth/email/login")) {
            return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build())
                .build();
        }
        if (uri.contains("/couple/bind")) {
            return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build())
                .build();
        }
        if (uri.contains("/auth/")) {
            return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(30).refillGreedy(30, Duration.ofMinutes(1)).build())
                .build();
        }
        // Default: 60/min for authenticated endpoints
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build())
            .build();
    }
}
