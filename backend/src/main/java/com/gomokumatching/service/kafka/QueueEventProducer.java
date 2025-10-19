package com.gomokumatching.service.kafka;

import com.gomokumatching.model.dto.kafka.QueueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// publishes queue events with synchronous blocking for reliability
// uses constant key "global-queue" to ensure strict FIFO ordering across all events
@Service
@Slf4j
@RequiredArgsConstructor
public class QueueEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String QUEUE_EVENTS_TOPIC = "matchmaking-queue-events";

    public void publishPlayerJoined(UUID playerId) {
        publishEvent(QueueEvent.playerJoined(playerId));
    }

    public void publishPlayerLeft(UUID playerId) {
        publishEvent(QueueEvent.playerLeft(playerId));
    }

    public void publishPlayerTimeout(UUID playerId) {
        publishEvent(QueueEvent.playerTimeout(playerId));
    }

    private void publishEvent(QueueEvent event) {
        try {
            String key = "global-queue"; // same partition = strict ordering

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(QUEUE_EVENTS_TOPIC, key, event);

            SendResult<String, Object> result = future.get();

            log.info("published queue event: action={}, player={}, partition={}",
                    event.getAction(),
                    event.getPlayerId(),
                    result.getRecordMetadata().partition());

        } catch (Exception e) {
            log.error("failed to publish queue event: action={}, player={}, error={}",
                    event.getAction(),
                    event.getPlayerId(),
                    e.getMessage(),
                    e);

            throw new RuntimeException("Failed to publish queue event: " + e.getMessage(), e);
        }
    }
}
