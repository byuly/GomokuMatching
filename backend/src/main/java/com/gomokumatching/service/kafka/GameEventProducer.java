package com.gomokumatching.service.kafka;

import com.gomokumatching.model.dto.kafka.GameMoveEvent;
import com.gomokumatching.model.dto.kafka.MatchCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer service for publishing game events.
 *
 * Publishes events to:
 * - game-move-made: Every move (player & AI)
 * - match-created: Match formation events
 *
 * Design:
 * - Async publishing (@Async) to not block game operations
 * - Fire-and-forget pattern (game continues even if Kafka fails)
 * - Detailed logging for monitoring and debugging
 *
 * Error Handling:
 * - Log errors but don't throw exceptions
 * - Game operations should never fail due to Kafka issues
 * - Kafka is for analytics, not critical path
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String GAME_MOVE_TOPIC = "game-move-made";
    private static final String MATCH_CREATED_TOPIC = "match-created";

    /**
     * Publish game move event.
     *
     * Called after every move (player or AI) is processed.
     * Async execution ensures game flow is not blocked.
     *
     * @param event GameMoveEvent to publish
     */
    @Async
    public void publishGameMove(GameMoveEvent event) {
        try {
            String key = event.getGameId().toString();

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(GAME_MOVE_TOPIC, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("published move event: game={}, move={}, partition={}",
                            event.getGameId(),
                            event.getMoveNumber(),
                            result.getRecordMetadata().partition());
                } else {
                    log.error("failed to publish move event: game={}, move={}, error={}",
                            event.getGameId(),
                            event.getMoveNumber(),
                            ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("exception publishing move event: game={}, error={}",
                    event.getGameId(), e.getMessage(), e);
        }
    }

    /**
     * Publish match created event.
     *
     * Called when a new match is formed (matchmaking, direct challenge, AI game).
     * Async execution ensures matchmaking flow is not blocked.
     *
     * @param event MatchCreatedEvent to publish
     */
    @Async
    public void publishMatchCreated(MatchCreatedEvent event) {
        try {
            String key = event.getGameId().toString();

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(MATCH_CREATED_TOPIC, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("published match-created event: game={}, type={}, source={}, partition={}",
                            event.getGameId(),
                            event.getGameType(),
                            event.getMatchSource(),
                            result.getRecordMetadata().partition());
                } else {
                    log.error("failed to publish match-created event: game={}, error={}",
                            event.getGameId(),
                            ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("exception publishing match-created event: game={}, error={}",
                    event.getGameId(), e.getMessage(), e);
        }
    }
}
