package com.example.subscription.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = (String) request.getAttribute("role");
        String path = request.getRequestURI();

        // Admin-only endpoints
        List<String> adminPaths = Arrays.asList(
            "/api/subscription-plans"
        );

        boolean isAdminPath = adminPaths.stream().anyMatch(path::startsWith);
        
        if (isAdminPath && !"ADMIN".equals(role)) {
            log.warn("Unauthorized access attempt: role={}, path={}", role, path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }
}

