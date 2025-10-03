package com.gomokumatching.repository;

import com.gomokumatching.model.PlayerStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the PlayerStats entity.
 */
@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, UUID> {

    /**
     * Find stats by player ID
     */
    Optional<PlayerStats> findByPlayer_PlayerId(UUID playerId);

    /**
     * Get leaderboard (top players by MMR)
     */
    @Query("SELECT ps FROM PlayerStats ps ORDER BY ps.currentMmr DESC")
    Page<PlayerStats> findLeaderboard(Pageable pageable);

    /**
     * Get top N players by MMR
     */
    @Query("SELECT ps FROM PlayerStats ps ORDER BY ps.currentMmr DESC")
    List<PlayerStats> findTopPlayers(Pageable pageable);

    /**
     * Get players with MMR in range (for matchmaking)
     */
    @Query("SELECT ps FROM PlayerStats ps WHERE ps.currentMmr BETWEEN :minMmr AND :maxMmr ORDER BY ps.currentMmr")
    List<PlayerStats> findPlayersByMmrRange(int minMmr, int maxMmr);

    /**
     * Check if player stats exist
     */
    boolean existsByPlayer_PlayerId(UUID playerId);
}
