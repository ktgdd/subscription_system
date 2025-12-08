package com.example.subscription.service;

import com.example.subscription.kafka.BookKeepingProducer;
import com.example.subscription.model.BookKeeping;
import com.example.subscription.repository.BookKeepingRepository;
import com.example.subscription.service.impl.BookKeepingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookKeepingServiceTest {

    @Mock
    private BookKeepingRepository bookKeepingRepository;

    @Mock
    private BookKeepingProducer bookKeepingProducer;

    @InjectMocks
    private BookKeepingServiceImpl bookKeepingService;

    private BookKeeping testBookKeeping;

    @BeforeEach
    void setUp() {
        testBookKeeping = new BookKeeping();
        testBookKeeping.setId(1L);
        testBookKeeping.setIdempotencyKey("test:1:2:req123");
        testBookKeeping.setUserId(100L);
        testBookKeeping.setSubscriptionPlanId(1L);
        testBookKeeping.setEventType("SUBSCRIBED");
        testBookKeeping.setStatus("INITIATED");
    }

    @Test
    void testCreateBookKeepingEntry_Success() {
        // Given
        when(bookKeepingRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);
        when(bookKeepingRepository.save(any(BookKeeping.class)))
                .thenReturn(testBookKeeping);

        // When
        BookKeeping result = bookKeepingService.createBookKeepingEntry(testBookKeeping);

        // Then
        assertNotNull(result);
        assertEquals("INITIATED", result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertNotNull(result.getCreatedAt());
        verify(bookKeepingRepository).save(any(BookKeeping.class));
    }

    @Test
    void testCreateBookKeepingEntry_Duplicate() {
        // Given
        when(bookKeepingRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(true);
        when(bookKeepingRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.of(testBookKeeping));

        // When
        BookKeeping result = bookKeepingService.createBookKeepingEntry(testBookKeeping);

        // Then
        assertNotNull(result);
        verify(bookKeepingRepository, never()).save(any(BookKeeping.class));
    }

    @Test
    void testMarkAsCompleted() {
        // Given
        when(bookKeepingRepository.findById(1L))
                .thenReturn(Optional.of(testBookKeeping));
        when(bookKeepingRepository.save(any(BookKeeping.class)))
                .thenReturn(testBookKeeping);
        doNothing().when(bookKeepingProducer).sendBookKeepingEvent(any(BookKeeping.class));

        // When
        bookKeepingService.markAsCompleted(1L, "payment-ref-123");

        // Then
        assertEquals("COMPLETED", testBookKeeping.getStatus());
        assertEquals("payment-ref-123", testBookKeeping.getPaymentReferenceId());
        assertNotNull(testBookKeeping.getCompletedAt());
        verify(bookKeepingProducer).sendBookKeepingEvent(any(BookKeeping.class));
    }
}

