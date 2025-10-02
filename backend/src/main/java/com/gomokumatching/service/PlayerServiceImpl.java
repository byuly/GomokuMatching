package com.gomokumatching.service;

import com.gomokumatching.exception.ResourceAlreadyExistsException;
import com.gomokumatching.model.Player;
import com.gomokumatching.model.dto.PlayerProfileDTO;
import com.gomokumatching.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of PlayerService for player-related operations.
 */
@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);

    private final PlayerRepository playerRepository;

    @Override
    @Transactional(readOnly = true)
    public Player getPlayerById(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Player not found with ID: " + playerId
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerProfileDTO getPlayerProfile(UUID playerId) {
        Player player = getPlayerById(playerId);

        return PlayerProfileDTO.builder()
                .playerId(player.getPlayerId())
                .username(player.getUsername())
                .email(player.getEmail())
                .createdAt(player.getCreatedAt())
                .lastLogin(player.getLastLogin())
                .accountStatus(player.getAccountStatus())
                .build();
    }

    @Override
    @Transactional
    public void updateUsername(UUID playerId, String username) {
        logger.info("Updating username for player ID: {}", playerId);

        // Check if new username already exists (exclude current player)
        if (playerRepository.existsByUsername(username)) {
            Player existingPlayer = playerRepository.findByUsername(username)
                    .orElseThrow();

            // If the username belongs to a different player, throw exception
            if (!existingPlayer.getPlayerId().equals(playerId)) {
                throw new ResourceAlreadyExistsException(
                        "Username already taken: " + username
                );
            }

            // If it's the same player's current username, no update needed
            logger.debug("Username unchanged for player ID: {}", playerId);
            return;
        }

        // Update username
        Player player = getPlayerById(playerId);
        player.setUsername(username);
        playerRepository.save(player);

        logger.info("Username updated successfully for player ID: {}", playerId);
    }
}
