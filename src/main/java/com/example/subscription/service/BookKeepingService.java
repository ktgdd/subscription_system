package com.example.subscription.service;

import com.example.subscription.model.BookKeeping;

public interface BookKeepingService {
    
    BookKeeping createBookKeepingEntry(BookKeeping bookKeeping);
    
    void markAsCompleted(Long bookKeepingId, String paymentReferenceId);
    
    BookKeeping findByIdempotencyKey(String idempotencyKey);
}
