package com.example.subscription.middleware;

import com.example.subscription.config.AppProperties;
import com.example.subscription.util.RequestIdExtractor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.jsonwebtoken.ExpiredJwtException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final AppProperties appProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // dev bypass for testing
        if (appProperties.getJwt().isDevBypassEnabled()) {
            String userId = request.getHeader("X-User-Id");
            String reqId = request.getHeader("X-Request-Id");
            String role = request.getHeader("X-Role");
            
            if (userId != null) {
                request.setAttribute("userId", Long.parseLong(userId));
                request.setAttribute("role", role != null ? role : "USER");
                // generate request id if not provided
                String finalReqId = reqId != null ? reqId : UUID.randomUUID().toString();
                request.setAttribute("requestId", finalReqId);
                return true;
            }
        }
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
            
            Claims claims;
            try {
                claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (ExpiredJwtException e) {
                // In debug mode, allow expired tokens
                if (appProperties.getJwt().isDebugMode() && appProperties.getJwt().isDebugSkipExpiryValidation()) {
                    log.warn("DEBUG MODE: Allowing expired JWT token - expiry validation is disabled");
                    claims = e.getClaims();
                } else {
                    throw e;
                }
            }

            // Store claims in request attributes for use in controllers/services
            request.setAttribute("userId", Long.parseLong(claims.get(appProperties.getJwt().getUserIdClaim(), String.class)));
            request.setAttribute("role", claims.get(appProperties.getJwt().getRoleClaim(), String.class));
            request.setAttribute("requestId", RequestIdExtractor.extractFromJwt(
                    claims.get(appProperties.getJwt().getRequestIdClaim(), String.class)));

            return true;
        } catch (Exception e) {
            log.error("JWT validation failed", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}

