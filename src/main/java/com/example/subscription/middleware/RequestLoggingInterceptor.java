package com.example.subscription.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate correlation ID if not present
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Set in MDC for logging
        MDC.put("correlationId", correlationId);
        MDC.put("traceId", correlationId);
        MDC.put("spanId", UUID.randomUUID().toString().substring(0, 8));
        
        // Set in response header
        response.setHeader("X-Correlation-ID", correlationId);

        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        log.info("Incoming request: method={}, uri={}, correlationId={}", 
                request.getMethod(), request.getRequestURI(), correlationId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String correlationId = MDC.get("correlationId");
            
            if (ex != null) {
                log.error("Request failed: method={}, uri={}, status={}, duration={}ms, correlationId={}", 
                        request.getMethod(), request.getRequestURI(), response.getStatus(), 
                        duration, correlationId, ex);
            } else {
                log.info("Request completed: method={}, uri={}, status={}, duration={}ms, correlationId={}", 
                        request.getMethod(), request.getRequestURI(), response.getStatus(), 
                        duration, correlationId);
            }
        }

        // Clear MDC
        MDC.clear();
    }
}

