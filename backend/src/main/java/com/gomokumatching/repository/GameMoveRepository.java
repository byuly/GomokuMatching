package com.gomokumatching.repository;

import com.gomokumatching.model.GameMove;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the GameMove entity.
 * Used for game replay and move history.
 */
@Repository
public interface GameMoveRepository extends JpaRepository<GameMove, UUID> {

    /**
     * Find all moves for a specific game, ordered by move number (for replay)
     */
    @Query("SELECT gm FROM GameMove gm WHERE gm.game.gameId = :gameId ORDER BY gm.moveNumber ASC")
    List<GameMove> findByGameIdOrderByMoveNumber(@Param("gameId") UUID gameId);

    /**
     * Count total moves in a game
     */
    long countByGame_GameId(UUID gameId);

    /**
     * Find specific move in a game
     */
    @Query("SELECT gm FROM GameMove gm WHERE gm.game.gameId = :gameId AND gm.moveNumber = :moveNumber")
    GameMove findByGameIdAndMoveNumber(@Param("gameId") UUID gameId, @Param("moveNumber") int moveNumber);

    /**
     * Delete all moves for a game (cascade delete helper)
     */
    @Modifying
    @Transactional
    void deleteByGame_GameId(UUID gameId);
}
