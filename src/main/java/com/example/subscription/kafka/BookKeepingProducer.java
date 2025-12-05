package com.example.subscription.kafka;

import com.example.subscription.config.AppProperties;
import com.example.subscription.model.BookKeeping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookKeepingProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendBookKeepingEvent(BookKeeping bookKeeping) {
        try {
            String json = objectMapper.writeValueAsString(bookKeeping);
            String topic = appProperties.getKafka().getTopicBookKeeping();
            
            kafkaTemplate.send(topic, bookKeeping.getIdempotencyKey(), json)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Sent book keeping event to Kafka: idempotencyKey={}", bookKeeping.getIdempotencyKey());
                        } else {
                            log.error("Failed to send book keeping event to Kafka", ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Error serializing book keeping event", e);
        }
    }
}

