package com.gomokumatching.controller;

import com.gomokumatching.model.GameSession;
import com.gomokumatching.model.dto.game.ErrorMessageDTO;
import com.gomokumatching.model.dto.game.GameStateResponse;
import com.gomokumatching.model.dto.game.MakeMoveRequest;
import com.gomokumatching.security.CustomUserDetails;
import com.gomokumatching.service.GameAuthorizationService;
import com.gomokumatching.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * WebSocket controller for real-time Player vs Player gameplay.
 *
 * Message Flow:
 * 1. Client connects to /ws (via WebSocketConfig)
 * 2. Client subscribes to /topic/game/{gameId}
 * 3. Client sends move to /app/game/{gameId}/move
 * 4. Server validates, processes, and broadcasts to /topic/game/{gameId}
 * 5. Both players receive updated game state in real-time
 *
 * Security:
 * - JWT validated in WebSocketAuthInterceptor
 * - Principal injected via Spring Security
 * - Authorization checked per-message
 *
 * Error Handling:
 * - Errors sent to user's queue: /user/queue/errors
 * - Only the user who caused error receives it (not broadcast)
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketController {

    private final GameService gameService;
    private final GameAuthorizationService authService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle player move via WebSocket.
     *
     * Client sends to: /app/game/{gameId}/move
     * Server broadcasts to: /topic/game/{gameId}
     *
     * @param gameId Game ID from path variable
     * @param request Move request (row, col)
     * @param principal Authenticated user principal
     * @return Updated game state (broadcast to all subscribers)
     */
    @MessageMapping("/game/{gameId}/move")
    @SendTo("/topic/game/{gameId}")
    public GameStateResponse handleMove(
            @DestinationVariable UUID gameId,
            @Valid MakeMoveRequest request,
            Principal principal
    ) {
        // extract user id from principal
        UUID playerId = extractUserId(principal);

        log.info("WebSocket move: game={}, player={}, row={}, col={}",
                gameId, playerId, request.getRow(), request.getCol());

        authService.validatePlayerAccess(gameId, playerId);
        authService.validatePlayerTurn(gameId, playerId);

        GameSession session = gameService.processMove(
                gameId,
                playerId,
                request.getRow(),
                request.getCol()
        );

        log.info("WebSocket move processed: game={}, moveCount={}, status={}",
                gameId, session.getMoveCount(), session.getStatus());

        // returning state(broadcast to all subscribers of /topic/game/{gameId})
        return GameStateResponse.fromGameSession(session);
    }

    /**
     * Handle game forfeit via WebSocket.
     *
     * Client sends to: /app/game/{gameId}/forfeit
     * Server broadcasts to: /topic/game/{gameId}
     *
     * @param gameId Game ID from path variable
     * @param principal Authenticated user principal
     * @return Final game state (broadcast to all subscribers)
     */
    @MessageMapping("/game/{gameId}/forfeit")
    @SendTo("/topic/game/{gameId}")
    public GameStateResponse handleForfeit(
            @DestinationVariable UUID gameId,
            Principal principal
    ) {
        UUID playerId = extractUserId(principal);

        log.info("WebSocket forfeit: game={}, player={}", gameId, playerId);

        authService.validatePlayerAccess(gameId, playerId);

        GameSession session = gameService.forfeitGame(gameId, playerId);

        log.info("WebSocket forfeit processed: game={}, winner={}",
                gameId, session.getWinnerType());

        // return final statee (broadcast to all subscribers)
        return GameStateResponse.fromGameSession(session);
    }

    /**
     * Handle exceptions in WebSocket message handlers.
     *
     * Sends error message to the specific user's error queue (not broadcast).
     * Other players won't see this error.
     *
     * @param ex Exception that occurred
     * @return Error message sent to /user/queue/errors
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorMessageDTO handleException(Exception ex) {
        log.error("WebSocket error", ex);

        String errorCode = determineErrorCode(ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "An error occurred";

        return ErrorMessageDTO.of(errorCode, message, ex.getClass().getSimpleName());
    }

    /**
     * Extract user ID from Spring Security Principal.
     *
     * @param principal Principal from WebSocket authentication
     * @return User ID
     * @throws IllegalStateException if principal is invalid
     */
    private UUID extractUserId(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken =
                    (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal;

            Object principalObj = authToken.getPrincipal();

            if (principalObj instanceof CustomUserDetails) {
                return ((CustomUserDetails) principalObj).getId();
            }
        }

        throw new IllegalStateException("Invalid principal: unable to extract user ID");
    }

    /**
     * Determine error code from exception type.
     *
     * @param ex Exception
     * @return Error code string
     */
    private String determineErrorCode(Exception ex) {
        String className = ex.getClass().getSimpleName();

        return switch (className) {
            case "GameNotFoundException" -> "GAME_NOT_FOUND";
            case "InvalidMoveException" -> "INVALID_MOVE";
            case "UnauthorizedGameAccessException" -> "UNAUTHORIZED";
            case "GameAlreadyCompletedException" -> "GAME_COMPLETED";
            case "IllegalStateException" -> "ILLEGAL_STATE";
            case "IllegalArgumentException" -> "INVALID_INPUT";
            default -> "INTERNAL_ERROR";
        };
    }

    /**
     * Send game state update to specific game's topic.
     * Useful for server-initiated updates (e.g., AI moves, game events).
     *
     * @param gameId Game ID
     * @param state Game state to broadcast
     */
    public void broadcastGameState(UUID gameId, GameStateResponse state) {
        String destination = "/topic/game/" + gameId;
        messagingTemplate.convertAndSend(destination, state);
        log.debug("Broadcast game state to {}", destination);
    }
}
