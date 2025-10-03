package com.gomokumatching.exception;

/**
 * Exception thrown when game creation request is invalid.
 *
 * Reasons:
 * - Both player2Id and aiOpponentId are set
 * - Neither player2Id nor aiOpponentId is set
 * - Game type doesn't match opponent type
 *
 * HTTP Status: 400 Bad Request
 */
public class InvalidGameRequestException extends RuntimeException {

    public InvalidGameRequestException(String message) {
        super(message);
    }
}
