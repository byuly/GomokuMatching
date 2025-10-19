package com.gomokumatching.service.kafka;

import com.gomokumatching.model.Game;
import com.gomokumatching.model.GameMove;
import com.gomokumatching.model.Player;
import com.gomokumatching.model.dto.kafka.GameMoveEvent;
import com.gomokumatching.model.enums.PlayerTypeEnum;
import com.gomokumatching.model.enums.StoneColorEnum;
import com.gomokumatching.repository.GameMoveRepository;
import com.gomokumatching.repository.GameRepository;
import com.gomokumatching.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for game move events - EVENT-DRIVEN PERSISTENCE + ANALYTICS.
 *
 * Listens to: game-move-made topic
 *
 * Responsibilities:
 * - Persist individual moves to game_move table (event-driven architecture)
 * - Track move patterns and statistics for analytics
 * - Monitor game activity in real-time
 * - Future: Feed data to analytics dashboards, metrics systems
 *
 * Architecture:
 * - Asynchronous event-driven persistence (decoupled from game logic)
 * - game.move_sequence JSONB stores full sequence for fast replay
 * - game_move table stores individual rows for detailed analysis
 * - Auto-commit offsets (configured in application.yml)
 * - 3 concurrent consumers (matches partition count)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameMovesConsumer {

    private final GameMoveRepository gameMoveRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;

    /**
     * Consume game move events for persistence and analytics.
     *
     * Event-Driven Architecture:
     * - Persists moves to game_move table asynchronously (decoupled from game logic)
     * - Tracks analytics and patterns in real-time
     * - game.move_sequence JSONB is still saved synchronously when game ends (for fast replay)
     *
     * Error Handling:
     * - Log and continue on failures (eventual consistency)
     * - No retries (accepts data loss for individual moves)
     * - game.move_sequence JSONB provides fallback with complete move history
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
            log.debug("📊 Move event received - game={}, move={}, player={}, position=({},{}), partition={}, offset={}",
                    event.getGameId(),
                    event.getMoveNumber(),
                    event.getPlayerType(),
                    event.getBoardX(),
                    event.getBoardY(),
                    partition,
                    offset);

            // Persist move to database (event-driven)
            persistMoveToDatabase(event);

            // Track analytics
            trackMoveAnalytics(event);

        } catch (Exception e) {
            // Log but don't throw - accept eventual consistency
            log.error("❌ Failed to process move event: game={}, move={}, error={}",
                    event.getGameId(), event.getMoveNumber(), e.getMessage());
        }
    }

    /**
     * Persist move to game_move table asynchronously.
     *
     * Event-driven persistence strategy:
     * - Saves moves as they happen during gameplay
     * - Decoupled from game logic
     * - No duplicate checking (trusts at-least-once delivery)
     *
     * @param event Move event to persist
     */
    private void persistMoveToDatabase(GameMoveEvent event) {
        try {
            // Fetch game entity
            Game game = gameRepository.findById(event.getGameId())
                    .orElseThrow(() -> new IllegalStateException("Game not found: " + event.getGameId()));

            // Create GameMove entity from event
            GameMove gameMove = new GameMove();
            gameMove.setGame(game);
            gameMove.setMoveNumber(event.getMoveNumber());
            gameMove.setBoardX(event.getBoardX());
            gameMove.setBoardY(event.getBoardY());
            gameMove.setStoneColor(StoneColorEnum.valueOf(event.getStoneColor()));

            // Set player type and player reference
            if ("HUMAN".equals(event.getPlayerType())) {
                gameMove.setPlayerType(PlayerTypeEnum.HUMAN);
                Player player = playerRepository.findById(event.getPlayerId())
                        .orElseThrow(() -> new IllegalStateException("Player not found: " + event.getPlayerId()));
                gameMove.setPlayer(player);
                gameMove.setAiDifficulty(null);
            } else {
                gameMove.setPlayerType(PlayerTypeEnum.AI);
                gameMove.setPlayer(null);
                gameMove.setAiDifficulty(event.getAiDifficulty());
            }

            // Save to database
            gameMoveRepository.save(gameMove);

            log.debug("move persisted to DB: game={}, move={}", event.getGameId(), event.getMoveNumber());

        } catch (Exception e) {
            log.error("failed to persist move to DB: game={}, move={}, error={}",
                    event.getGameId(), event.getMoveNumber(), e.getMessage());
            // Don't rethrow - accept eventual consistency
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
