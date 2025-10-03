package com.gomokumatching.exception;

/**
 * Exception thrown when a move is invalid.
 *
 * Reasons:
 * - Position is occupied
 * - Position is out of bounds
 * - Not player's turn
 *
 * HTTP Status: 400 Bad Request
 */
public class InvalidMoveException extends RuntimeException {

    public InvalidMoveException(String message) {
        super(message);
    }

    public InvalidMoveException(int row, int col, String reason) {
        super(String.format("Invalid move at position (%d, %d): %s", row, col, reason));
    }
}
