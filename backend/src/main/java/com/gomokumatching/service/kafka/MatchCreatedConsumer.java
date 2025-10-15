package com.gomokumatching.service.kafka;

import com.gomokumatching.model.dto.kafka.MatchCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for match-created events.
 *
 * Listens to: match-created topic
 *
 * Responsibilities:
 * - Consume MatchCreatedEvent messages
 * - Log match creation for analytics and monitoring
 * - Future: Update matchmaking metrics, player activity tracking
 *
 * Consumer Strategy:
 * - Auto-commit offsets (configured in KafkaConsumerConfig)
 * - 3 concurrent consumers (matches partition count)
 * - Non-critical path - errors logged but don't fail system
 *
 * Use Cases:
 * - Match history analytics
 * - Matchmaking performance metrics (avg wait time, success rate)
 * - Player activity tracking (matches per day, peak hours)
 * - System monitoring and alerting
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchCreatedConsumer {

    /**
     * Consume match-created events for analytics.
     *
     * Currently logs events for monitoring. Future enhancements:
     * - Calculate matchmaking metrics (wait time, success rate)
     * - Track player activity patterns
     * - Trigger notifications or webhooks
     * - Feed data to analytics dashboard
     *
     * @param event MatchCreatedEvent from Kafka
     * @param partition Kafka partition number
     * @param offset Kafka offset
     */
    @KafkaListener(
            topics = "match-created",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "matchCreatedEventKafkaListenerContainerFactory"
    )
    public void consumeMatchCreated(
            @Payload MatchCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            log.info("📊 Match created event: game={}, type={}, source={}, players={}/{}, partition={}, offset={}",
                    event.getGameId(),
                    event.getGameType(),
                    event.getMatchSource(),
                    event.getPlayer1Id(),
                    event.getPlayer2Id() != null ? event.getPlayer2Id() : "AI-" + event.getAiDifficulty(),
                    partition,
                    offset);

            // TODO: Persist to match_analytics table
            // TODO: Update matchmaking metrics
            // future: Trigger notifications (Discord, Slack, etc.)

        } catch (Exception e) {
            // analytics failures shouldn't disrupt system
            log.error("❌ Failed to process match-created event: game={}, error={}",
                    event.getGameId(), e.getMessage(), e);
        }
    }

    /**
     * Calculate matchmaking statistics.
     *
     * Future implementation:
     * - Average wait time per player
     * - Match success rate (matched vs timed out)
     * - Peak matchmaking hours
     * - Player retention metrics
     */
    private void updateMatchmakingMetrics(MatchCreatedEvent event) {
        // TODO: Implement when analytics tables are created
        log.debug("Matchmaking metrics update placeholder for game {}", event.getGameId());
    }
}
