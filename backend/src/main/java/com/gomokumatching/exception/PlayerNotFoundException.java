package com.gomokumatching.exception;

import java.util.UUID;

/**
 * Exception thrown when a player is not found in the database.
 *
 * Used in scenarios where player lookup is required but fails.
 */
public class PlayerNotFoundException extends RuntimeException {

    private final UUID playerId;

    public PlayerNotFoundException(UUID playerId) {
        super("Player not found: " + playerId);
        this.playerId = playerId;
    }

    public PlayerNotFoundException(String username) {
        super("Player not found with username: " + username);
        this.playerId = null;
    }

    public PlayerNotFoundException(UUID playerId, String message) {
        super(message);
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
