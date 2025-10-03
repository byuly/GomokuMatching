package com.gomokumatching.exception;

import java.util.UUID;

/**
 * Exception thrown when trying to modify a completed game.
 *
 * Reasons:
 * - Game has already ended (status = COMPLETED or ABANDONED)
 * - Attempting to make a move in a finished game
 *
 * HTTP Status: 409 Conflict
 */
public class GameAlreadyCompletedException extends RuntimeException {

    public GameAlreadyCompletedException(UUID gameId) {
        super(String.format("Game %s has already been completed", gameId));
    }

    public GameAlreadyCompletedException(String message) {
        super(message);
    }
}
