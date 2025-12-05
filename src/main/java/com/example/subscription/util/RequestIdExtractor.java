package com.example.subscription.util;

import java.util.UUID;

public class RequestIdExtractor {
    
    public static String extractFromJwt(String requestIdClaim) {
        // Extract from JWT claims, if not present, generate UUID
        return requestIdClaim != null && !requestIdClaim.isEmpty() 
            ? requestIdClaim 
            : UUID.randomUUID().toString();
    }
}

