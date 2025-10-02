package com.gomokumatching.repository;

import com.gomokumatching.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the Player entity.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    /**
     * Find player by username (case-sensitive).
     *
     * @param username Username to search
     * @return Optional containing player if found
     */
    Optional<Player> findByUsername(String username);

    /**
     * Find player by email (case-insensitive).
     *
     * @param email Email to search
     * @return Optional containing player if found
     */
    Optional<Player> findByEmailIgnoreCase(String email);

    /**
     * Check if username exists (case-sensitive).
     *
     * @param username Username to check
     * @return true if exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists (case-insensitive).
     *
     * @param email Email to check
     * @return true if exists
     */
    boolean existsByEmailIgnoreCase(String email);
}
