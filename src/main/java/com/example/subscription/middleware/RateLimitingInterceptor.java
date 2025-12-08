package com.example.subscription.middleware;

import com.example.subscription.cache.RedisCacheService;
import com.example.subscription.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RedisCacheService redisCacheService;
    private final AppProperties appProperties;
    
    private static final String RATE_LIMIT_PREFIX = "ratelimit";
    private static final int DEFAULT_RATE_LIMIT = 100; // requests per minute
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        Long userId = (Long) request.getAttribute("userId");
        
        if (userId == null) {
            // If no user ID, skip rate limiting (shouldn't happen if JWT is valid)
            return true;
        }

        // Per-user rate limiting
        String userKey = redisCacheService.buildKey(RATE_LIMIT_PREFIX, "user", userId.toString(), path);
        
        // Check current count
        String currentCountStr = redisCacheService.get(userKey).block();
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
        
        if (currentCount >= DEFAULT_RATE_LIMIT) {
            log.warn("Rate limit exceeded: userId={}, path={}, count={}", userId, path, currentCount);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_RATE_LIMIT));
            response.setHeader("X-RateLimit-Remaining", "0");
            return false;
        }

        // Increment counter
        redisCacheService.set(userKey, String.valueOf(currentCount + 1), Duration.ofSeconds(DEFAULT_WINDOW_SECONDS))
                .block();

        // Set rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_RATE_LIMIT));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(DEFAULT_RATE_LIMIT - currentCount - 1));

        return true;
    }
}

