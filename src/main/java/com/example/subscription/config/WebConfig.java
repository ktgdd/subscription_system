package com.example.subscription.config;

import com.example.subscription.middleware.JwtAuthInterceptor;
import com.example.subscription.middleware.RequestLoggingInterceptor;
import com.example.subscription.middleware.RoleAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final RoleAuthorizationInterceptor roleAuthorizationInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Request logging first (to capture all requests)
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**");
        
        // JWT authentication
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/health", "/actuator/**");
        
        // Role authorization
        registry.addInterceptor(roleAuthorizationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/health", "/actuator/**");
    }
}

