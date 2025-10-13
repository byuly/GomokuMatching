package com.gomokumatching.service.kafka;

import com.gomokumatching.model.AIOpponent;
import com.gomokumatching.model.Game;
import com.gomokumatching.model.GameMove;
import com.gomokumatching.model.Player;
import com.gomokumatching.model.dto.kafka.GameMoveEvent;
import com.gomokumatching.model.enums.PlayerTypeEnum;
import com.gomokumatching.model.enums.StoneColorEnum;
import com.gomokumatching.repository.AIOpponentRepository;
import com.gomokumatching.repository.GameMoveRepository;
import com.gomokumatching.repository.GameRepository;
import com.gomokumatching.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for game move events.
 *
 * Listens to: game-move-made topic
 *
 * Responsibilities:
 * - Consume GameMoveEvent messages
 * - Persist moves to game_move table for replay functionality
 * - Handle duplicate events (idempotent via unique constraints)
 * - Log errors to DLQ on failures
 *
 * Consumer Strategy:
 * - Manual offset commit (implicit via auto-commit in config)
 * - 3 concurrent consumers (matches partition count)
 * - Retry on transient failures, DLQ on permanent failures
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameMovesConsumer {

    private final GameMoveRepository gameMoveRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final AIOpponentRepository aiOpponentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consume game move events and persist to database.
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
    @Transactional
    public void consumeGameMove(
            @Payload GameMoveEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            log.debug("Consuming move event: game={}, move={}, partition={}, offset={}",
                    event.getGameId(), event.getMoveNumber(), partition, offset);

            // convert event to entity
            GameMove gameMove = convertEventToEntity(event);

            // save to database (idempotent via unique constraints)
            gameMoveRepository.save(gameMove);

            log.info("✅ Persisted move: game={}, move={}, player={}, position=({},{})",
                    event.getGameId(),
                    event.getMoveNumber(),
                    event.getPlayerType(),
                    event.getBoardX(),
                    event.getBoardY());

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // duplicate event (unique constraint violation) - this is normal in at-least-once delivery
            log.debug("Duplicate move event (already persisted): game={}, move={}",
                    event.getGameId(), event.getMoveNumber());

        } catch (Exception e) {
            // let it propagate to trigger retry or DLQ
            log.error("❌ Failed to process move event: game={}, move={}, error={}",
                    event.getGameId(), event.getMoveNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to process game move event", e);
        }
    }

    /**
     * Convert GameMoveEvent to GameMove entity.
     *
     * Fetches related entities (Game, Player, AI) from database.
     *
     * @param event Kafka event
     * @return GameMove entity ready for persistence
     */
    private GameMove convertEventToEntity(GameMoveEvent event) {
        GameMove gameMove = new GameMove();

        // fetch game (must exist)
        Game game = gameRepository.findById(event.getGameId())
                .orElseThrow(() -> new IllegalStateException(
                        "Game not found for move event: " + event.getGameId()));
        gameMove.setGame(game);

        // set move details
        gameMove.setMoveNumber(event.getMoveNumber());
        gameMove.setPlayerType(PlayerTypeEnum.valueOf(event.getPlayerType()));
        gameMove.setBoardX(event.getBoardX());
        gameMove.setBoardY(event.getBoardY());
        gameMove.setStoneColor(StoneColorEnum.valueOf(event.getStoneColor()));
        gameMove.setTimeTakenMs(event.getTimeTakenMs());

        // set player or AI
        if ("HUMAN".equals(event.getPlayerType())) {
            if (event.getPlayerId() != null) {
                Player player = playerRepository.findById(event.getPlayerId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Player not found: " + event.getPlayerId()));
                gameMove.setPlayer(player);
                gameMove.setAiOpponent(null);
            }
        } else if ("AI".equals(event.getPlayerType())) {
            if (event.getAiOpponentId() != null) {
                AIOpponent aiOpponent = aiOpponentRepository.findById(event.getAiOpponentId())
                        .orElseThrow(() -> new IllegalStateException(
                                "AI opponent not found: " + event.getAiOpponentId()));
                gameMove.setAiOpponent(aiOpponent);
                gameMove.setPlayer(null);
            }
        }

        // serialize board state as JSON
        try {
            String boardStateJson = objectMapper.writeValueAsString(event.getBoardStateAfterMove());
            gameMove.setBoardStateAfterMove(boardStateJson);
        } catch (Exception e) {
            log.warn("Failed to serialize board state for move {}: {}", event.getMoveNumber(), e.getMessage());
            gameMove.setBoardStateAfterMove(null);
        }

        return gameMove;
    }
}
