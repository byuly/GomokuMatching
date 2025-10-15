package com.gomokumatching.service.kafka;

import com.gomokumatching.model.dto.kafka.GameMoveEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for game move events - ANALYTICS ONLY.
 *
 * Listens to: game-move-made topic
 *
 * Responsibilities:
 * - Consume GameMoveEvent messages for real-time analytics
 * - Track move patterns and statistics
 * - Monitor game activity
 * - Future: Feed data to analytics dashboards, metrics systems
 *
 * NOTE: This consumer does NOT persist moves to database.
 * Move persistence is handled by GameService.saveMoveHistoryToDatabase()
 * when games complete, reading from Redis session.moveHistory.
 *
 * Consumer Strategy:
 * - Auto-commit offsets (configured in application.yml)
 * - 3 concurrent consumers (matches partition count)
 * - Lightweight processing (no DB writes)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameMovesConsumer {

    // No repository dependencies needed for analytics-only consumer

    /**
     * Consume game move events for analytics and monitoring.
     *
     * This consumer tracks move patterns in real-time but does NOT persist to database.
     * Move persistence is handled by GameService.saveMoveHistoryToDatabase() when games complete.
     *
     * Future use cases:
     * - Real-time analytics dashboards
     * - Move pattern analysis (popular openings, etc.)
     * - Live game monitoring
     * - Metrics collection (moves per second, game activity)
     *
     * @param event GameMoveEvent from Kafka
     * @param partition Kafka partition number
     * @param offset Kafka offset
     */
    @KafkaListener(
            topics = "game-move-made",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "gameMoveEventKafkaListenerContainerFactory"
    )
    public void consumeGameMove(
            @Payload GameMoveEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            log.debug("📊 Analytics: Move event received - game={}, move={}, player={}, position=({},{}), partition={}, offset={}",
                    event.getGameId(),
                    event.getMoveNumber(),
                    event.getPlayerType(),
                    event.getBoardX(),
                    event.getBoardY(),
                    partition,
                    offset);

            // Track analytics (no DB operations)
            trackMoveAnalytics(event);

        } catch (Exception e) {
            // Log but don't throw - analytics failures shouldn't disrupt system
            log.error("❌ Failed to process analytics for move event: game={}, move={}, error={}",
                    event.getGameId(), event.getMoveNumber(), e.getMessage());
        }
    }

    /**
     * Track move analytics and patterns.
     *
     * Currently logs move information. Future enhancements:
     * - Aggregate move statistics (position frequency, opening patterns)
     * - Track AI vs Human move patterns
     * - Calculate move timing metrics
     * - Feed data to external analytics systems
     *
     * @param event Move event to analyze
     */
    private void trackMoveAnalytics(GameMoveEvent event) {
        // example analytics logging
        // this would send to metrics systems like datadog in enterprise setting

        log.info("📈 Move Analytics: game={}, move#{}, {}({}) → ({},{})",
                event.getGameId(),
                event.getMoveNumber(),
                event.getPlayerType(),
                event.getStoneColor(),
                event.getBoardX(),
                event.getBoardY());

        // track position statistics
        if (event.getMoveNumber() == 1) {
            log.debug("Opening move at ({},{})", event.getBoardX(), event.getBoardY());
        }

        // track AI difficulty usage
        if ("AI".equals(event.getPlayerType()) && event.getAiDifficulty() != null) {
            log.debug("AI move: difficulty={}", event.getAiDifficulty());
        }

        // TODO: Future analytics implementations:
        // - metricsService.incrementMoveCounter(event.getPlayerType());
        // - patternAnalyzer.trackOpening(event);
        // - dashboardService.updateLiveStats(event);
    }
}
