package com.example.subscription.service.impl;

import com.example.subscription.kafka.BookKeepingProducer;
import com.example.subscription.model.BookKeeping;
import com.example.subscription.repository.BookKeepingRepository;
import com.example.subscription.service.BookKeepingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookKeepingServiceImpl implements BookKeepingService {
    
    private final BookKeepingRepository bookKeepingRepository;
    private final BookKeepingProducer bookKeepingProducer;

    @Override
    @Transactional
    public BookKeeping createBookKeepingEntry(BookKeeping bookKeeping) {
        // Check if idempotency key already exists in database
        if (bookKeepingRepository.existsByIdempotencyKey(bookKeeping.getIdempotencyKey())) {
            log.warn("Book keeping entry already exists: {}", bookKeeping.getIdempotencyKey());
            return bookKeepingRepository.findByIdempotencyKey(bookKeeping.getIdempotencyKey())
                    .orElseThrow();
        }

        bookKeeping.setStatus("INITIATED");
        bookKeeping.setCreatedAt(LocalDateTime.now());
        bookKeeping.setRetryCount(0);
        
        BookKeeping saved = bookKeepingRepository.save(bookKeeping);
        log.info("Created book keeping entry: id={}, idempotencyKey={}", saved.getId(), saved.getIdempotencyKey());
        
        return saved;
    }

    @Override
    @Transactional
    public void markAsCompleted(Long bookKeepingId, String paymentReferenceId) {
        BookKeeping bookKeeping = bookKeepingRepository.findById(bookKeepingId)
                .orElseThrow(() -> new RuntimeException("Book keeping entry not found: " + bookKeepingId));
        
        bookKeeping.setStatus("COMPLETED");
        bookKeeping.setPaymentReferenceId(paymentReferenceId);
        bookKeeping.setCompletedAt(LocalDateTime.now());
        
        bookKeepingRepository.save(bookKeeping);
        
        // Send to Kafka for async processing
        bookKeepingProducer.sendBookKeepingEvent(bookKeeping);
        log.info("Marked book keeping as completed and sent to Kafka: id={}", bookKeepingId);
    }

    @Override
    public BookKeeping findByIdempotencyKey(String idempotencyKey) {
        return bookKeepingRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);
    }
}

