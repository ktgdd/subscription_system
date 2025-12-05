package com.example.subscription.kafka;

import com.example.subscription.model.BookKeeping;
import com.example.subscription.repository.BookKeepingRepository;
import com.example.subscription.repository.UserSubscriptionRepository;
import com.example.subscription.service.UserSubscriptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookKeepingConsumer {

    private final BookKeepingRepository bookKeepingRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserSubscriptionService userSubscriptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${app.kafka.topic.book-keeping}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeBookKeepingEvent(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            BookKeeping bookKeeping = objectMapper.readValue(message, BookKeeping.class);
            log.info("Received book keeping event: idempotencyKey={}, status={}", 
                    bookKeeping.getIdempotencyKey(), bookKeeping.getStatus());

            if ("COMPLETED".equals(bookKeeping.getStatus())) {
                processCompletedBookKeeping(bookKeeping);
            }
            
            acknowledgment.acknowledge();
        } catch (JsonProcessingException e) {
            log.error("Error deserializing book keeping event", e);
            acknowledgment.acknowledge(); // Acknowledge to avoid reprocessing
        } catch (Exception e) {
            log.error("Error processing book keeping event", e);
            // Don't acknowledge on error - will retry
        }
    }

    private void processCompletedBookKeeping(BookKeeping bookKeeping) {
        try {
            userSubscriptionService.materializeFromBookKeeping(bookKeeping);
            
            bookKeeping.setStatus("PROCESSED");
            bookKeeping.setProcessedAt(LocalDateTime.now());
            bookKeepingRepository.save(bookKeeping);
            
            log.info("Successfully processed book keeping: idempotencyKey={}", bookKeeping.getIdempotencyKey());
        } catch (Exception e) {
            log.error("Error materializing book keeping to user subscription", e);
            bookKeeping.setRetryCount(bookKeeping.getRetryCount() + 1);
            if (bookKeeping.getRetryCount() >= 3) {
                bookKeeping.setStatus("FAILED");
                bookKeeping.setErrorMessage(e.getMessage());
            }
            bookKeepingRepository.save(bookKeeping);
        }
    }
}

