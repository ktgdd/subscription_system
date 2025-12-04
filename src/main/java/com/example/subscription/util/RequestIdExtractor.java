package com.example.subscription.util;

import java.util.UUID;

public class RequestIdExtractor {
    
    public static String extractFromJwt(String requestIdClaim) {
        // TODO: Extract from JWT claims
        // If not present, generate UUID
        return requestIdClaim != null && !requestIdClaim.isEmpty() 
            ? requestIdClaim 
            : UUID.randomUUID().toString();
    }
}

