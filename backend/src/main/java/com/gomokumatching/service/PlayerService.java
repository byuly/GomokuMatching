package com.gomokumatching.service;

import com.gomokumatching.model.Player;
import com.gomokumatching.model.dto.PlayerProfileDTO;

import java.util.UUID;

/**
 * Service interface for player-related operations.
 */
public interface PlayerService {

    /**
     * Get player by UUID.
     *
     * @param playerId Player's UUID
     * @return Player entity
     */
    Player getPlayerById(UUID playerId);

    /**
     * Get player profile DTO by UUID.
     *
     * @param playerId Player's UUID
     * @return Player profile DTO
     */
    PlayerProfileDTO getPlayerProfile(UUID playerId);

    /**
     * Update player's username.
     *
     * @param playerId Player's UUID
     * @param username New username
     */
    void updateUsername(UUID playerId, String username);
}
