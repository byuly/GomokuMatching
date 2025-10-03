package com.gomokumatching.service;

import com.gomokumatching.exception.GameNotFoundException;
import com.gomokumatching.exception.UnauthorizedGameAccessException;
import com.gomokumatching.model.GameSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for game authorization and access control.
 *
 * Validates that users have permission to access and modify games.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameAuthorizationService {

    private final RedisService redisService;

    /**
     * Validate that a player has access to a game.
     *
     * @param gameId Game ID
     * @param playerId Player ID
     * @throws GameNotFoundException if game doesn't exist
     * @throws UnauthorizedGameAccessException if player is not authorized
     */
    public void validatePlayerAccess(UUID gameId, UUID playerId) {
        GameSession session = redisService.getGameSession(gameId);

        if (session == null) {
            throw new GameNotFoundException(gameId);
        }

        boolean isPlayer1 = playerId.equals(session.getPlayer1Id());
        boolean isPlayer2 = session.getPlayer2Id() != null &&
                            playerId.equals(session.getPlayer2Id());

        if (!isPlayer1 && !isPlayer2) {
            log.warn("Unauthorized access attempt: player {} trying to access game {}", playerId, gameId);
            throw new UnauthorizedGameAccessException(playerId, gameId);
        }

        log.debug("Player {} authorized to access game {}", playerId, gameId);
    }

    /**
     * Validate that it's the player's turn.
     *
     * @param gameId Game ID
     * @param playerId Player ID
     * @throws GameNotFoundException if game doesn't exist
     * @throws UnauthorizedGameAccessException if not player's turn
     */
    public void validatePlayerTurn(UUID gameId, UUID playerId) {
        GameSession session = redisService.getGameSession(gameId);

        if (session == null) {
            throw new GameNotFoundException(gameId);
        }

        int playerNumber = getPlayerNumber(session, playerId);

        if (playerNumber != session.getCurrentPlayer()) {
            throw new UnauthorizedGameAccessException("Not your turn");
        }

        log.debug("Player {} turn validated for game {}", playerId, gameId);
    }

    /**
     * Get player number (1 or 2) for a given player ID.
     *
     * @param session Game session
     * @param playerId Player ID
     * @return 1 or 2
     * @throws UnauthorizedGameAccessException if player not in game
     */
    public int getPlayerNumber(GameSession session, UUID playerId) {
        if (playerId.equals(session.getPlayer1Id())) {
            return 1;
        } else if (session.getPlayer2Id() != null && playerId.equals(session.getPlayer2Id())) {
            return 2;
        } else {
            throw new UnauthorizedGameAccessException(
                playerId,
                session.getGameId()
            );
        }
    }

    /**
     * Get game session and validate access in one call.
     *
     * @param gameId Game ID
     * @param playerId Player ID
     * @return GameSession
     * @throws GameNotFoundException if game doesn't exist
     * @throws UnauthorizedGameAccessException if player not authorized
     */
    public GameSession getGameSessionWithAccess(UUID gameId, UUID playerId) {
        validatePlayerAccess(gameId, playerId);
        return redisService.getGameSession(gameId);
    }
}
