package com.gomokumatching.exception;

import java.util.UUID;

/**
 * Exception thrown when a game is not found.
 *
 * HTTP Status: 404 Not Found
 */
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(UUID gameId) {
        super("Game not found with ID: " + gameId);
    }

    public GameNotFoundException(String message) {
        super(message);
    }
}
