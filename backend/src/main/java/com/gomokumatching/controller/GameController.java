package com.gomokumatching.controller;

import com.gomokumatching.client.AIServiceClient;
import com.gomokumatching.exception.GameNotFoundException;
import com.gomokumatching.exception.InvalidGameRequestException;
import com.gomokumatching.model.GameSession;
import com.gomokumatching.model.dto.game.*;
import com.gomokumatching.security.CustomUserDetails;
import com.gomokumatching.service.GameAuthorizationService;
import com.gomokumatching.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for game management operations.
 *
 * Handles:
 * - Game creation (PvP and PvAI)
 * - Move processing for PvAI games
 * - Game state retrieval
 * - Game forfeiting
 * - Move history
 *
 * Security: All endpoints require JWT authentication
 *
 * Note: PvP moves are handled via WebSocket in GameWebSocketController
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;
    private final GameAuthorizationService authService;
    private final AIServiceClient aiServiceClient;

    /**
     * Create a new game (PvP or PvAI).
     *
     * POST /api/game/create
     *
     * Request body:
     * {
     *   "gameType": "HUMAN_VS_HUMAN",  // or "HUMAN_VS_AI"
     *   "player2Id": "uuid",            // for PvP (null for AI)
     *   "aiOpponentId": "uuid"          // for PvAI (null for PvP)
     * }
     *
     * @param request Game creation request
     * @param currentUser Authenticated user (player 1)
     * @return 201 Created with game ID and connection info
     */
    @PostMapping("/create")
    public ResponseEntity<GameCreatedResponse> createGame(
            @Valid @RequestBody CreateGameRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID player1Id = currentUser.getId();

        log.info("Create game request from player {}: type={}", player1Id, request.getGameType());

        if (!request.isValid() || !request.isGameTypeConsistent()) {
            throw new InvalidGameRequestException(
                "Invalid game request. For PvP games, set player2Id. For PvAI games, set aiOpponentId."
            );
        }

        GameSession session;

        if (request.getGameType() == GameSession.GameType.HUMAN_VS_HUMAN) {
            // Player vs Player
            session = gameService.createPvPGame(player1Id, request.getPlayer2Id());
            log.info("Created PvP game {}: player1={}, player2={}",
                    session.getGameId(), player1Id, request.getPlayer2Id());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(GameCreatedResponse.forPvPGame(session.getGameId()));

        } else {
            // Player vs AI
            session = gameService.createPvAIGame(player1Id, request.getAiOpponentId());
            log.info("Created PvAI game {}: player={}, ai={}",
                    session.getGameId(), player1Id, request.getAiOpponentId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(GameCreatedResponse.forPvAIGame(session.getGameId()));
        }
    }

    /**
     * Make a move in a PvAI game.
     *
     * POST /api/game/{gameId}/move
     *
     * Request body:
     * {
     *   "row": 7,
     *   "col": 7
     * }
     *
     * Note: For PvP games, use WebSocket instead.
     *
     * @param gameId Game ID
     * @param request Move request
     * @param currentUser Authenticated user
     * @return 200 OK with updated game state
     */
    @PostMapping("/{gameId}/move")
    public ResponseEntity<GameStateResponse> makeMove(
            @PathVariable UUID gameId,
            @Valid @RequestBody MakeMoveRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();

        log.info("Move request for game {}: player={}, row={}, col={}",
                gameId, playerId, request.getRow(), request.getCol());

        authService.validatePlayerAccess(gameId, playerId);

        GameSession session = gameService.processMove(
                gameId,
                playerId,
                request.getRow(),
                request.getCol()
        );

        log.info("Move processed for game {}: moveCount={}, status={}",
                gameId, session.getMoveCount(), session.getStatus());

        // if game is PvAI and still ongoing, get AI move
        if (session.getGameType() == GameSession.GameType.HUMAN_VS_AI &&
            session.getStatus() == GameSession.GameStatus.IN_PROGRESS) {

            log.info("Getting AI move for game {}", gameId);

            try {
                // get from Django service
                AIServiceClient.AIMoveResponse aiMove = aiServiceClient.getAIMove(
                    session.getBoard(),
                    session.getCurrentPlayer(),
                    "medium"  // TODO: make configurable
                );

                log.info("AI move for game {}: row={}, col={}", gameId, aiMove.getRow(), aiMove.getCol());

                // processing ai move
                session = gameService.processMove(
                    gameId,
                    session.getPlayer2Id(),  // AI will always be player 2
                    aiMove.getRow(),
                    aiMove.getCol()
                );

                log.info("AI move processed for game {}: moveCount={}, status={}",
                        gameId, session.getMoveCount(), session.getStatus());

            } catch (AIServiceClient.AIServiceException e) {
                log.error("AI service error for game {}: {}", gameId, e.getMessage());
                // don't fail the request, just return state after player move
                // make frontend can retry or handle gracefully
            }
        }

        return ResponseEntity.ok(GameStateResponse.fromGameSession(session));
    }

    /**
     * Get current game state.
     *
     * GET /api/game/{gameId}
     *
     * @param gameId Game ID
     * @param currentUser Authenticated user
     * @return 200 OK with game state
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameStateResponse> getGameState(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();

        log.debug("Get game state request: game={}, player={}", gameId, playerId);

        GameSession session = authService.getGameSessionWithAccess(gameId, playerId);

        if (session == null) {
            throw new GameNotFoundException(gameId);
        }

        return ResponseEntity.ok(GameStateResponse.fromGameSession(session));
    }

    /**
     * Forfeit a game.
     *
     * POST /api/game/{gameId}/forfeit
     *
     * Player gives up, opponent wins.
     *
     * @param gameId Game ID
     * @param currentUser Authenticated user
     * @return 200 OK with final game state
     */
    @PostMapping("/{gameId}/forfeit")
    public ResponseEntity<GameStateResponse> forfeitGame(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();

        log.info("Forfeit request: game={}, player={}", gameId, playerId);

        authService.validatePlayerAccess(gameId, playerId);

        GameSession session = gameService.forfeitGame(gameId, playerId);

        log.info("Game {} forfeited by player {}", gameId, playerId);

        return ResponseEntity.ok(GameStateResponse.fromGameSession(session));
    }

    /**
     * Get move history for a game (for replay).
     *
     * GET /api/game/{gameId}/moves
     *
     * Note: Only available for completed games (persisted to database).
     * Active games don't have move history in DB yet (Phase 5 - Kafka consumers).
     *
     * @param gameId Game ID
     * @param currentUser Authenticated user
     * @return 200 OK with list of moves
     */
    @GetMapping("/{gameId}/moves")
    public ResponseEntity<List<MoveDTO>> getGameMoves(
            @PathVariable UUID gameId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();

        log.debug("Get game moves request: game={}, player={}", gameId, playerId);

        // Validate access
        authService.validatePlayerAccess(gameId, playerId);

        // TODO Phase 5: Query GameMoveRepository for move history
        // for now just return empty list (moves will be persisted via Kafka when implemented)

        log.warn("Move history not yet implemented (Phase 5 - Kafka consumers)");

        return ResponseEntity.ok(List.of());
    }
}
