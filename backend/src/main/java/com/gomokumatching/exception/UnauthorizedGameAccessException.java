package com.gomokumatching.exception;

import java.util.UUID;

/**
 * Exception thrown when a user tries to access a game they're not authorized to access.
 *
 * Reasons:
 * - User is not a player in the game
 * - User is trying to modify another player's game
 *
 * HTTP Status: 403 Forbidden
 */
public class UnauthorizedGameAccessException extends RuntimeException {

    public UnauthorizedGameAccessException(UUID playerId, UUID gameId) {
        super(String.format("Player %s is not authorized to access game %s", playerId, gameId));
    }

    public UnauthorizedGameAccessException(String message) {
        super(message);
    }
}
