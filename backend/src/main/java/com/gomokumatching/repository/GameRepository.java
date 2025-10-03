package com.gomokumatching.repository;

import com.gomokumatching.model.Game;
import com.gomokumatching.model.enums.GameStatusEnum;
import com.gomokumatching.model.enums.GameTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the Game entity.
 */
@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    /**
     * Find all games for a specific player (as player1 or player2)
     */
    @Query("SELECT g FROM Game g WHERE g.player1.playerId = :playerId OR g.player2.playerId = :playerId ORDER BY g.createdAt DESC")
    Page<Game> findGamesByPlayer(@Param("playerId") UUID playerId, Pageable pageable);

    /**
     * Find completed games for a player
     */
    @Query("SELECT g FROM Game g WHERE (g.player1.playerId = :playerId OR g.player2.playerId = :playerId) AND g.gameStatus = com.gomokumatching.model.enums.GameStatusEnum.COMPLETED ORDER BY g.endedAt DESC")
    List<Game> findCompletedGamesByPlayer(@Param("playerId") UUID playerId);

    /**
     * Find games by status
     */
    List<Game> findByGameStatus(GameStatusEnum gameStatus);

    /**
     * Find games by type
     */
    List<Game> findByGameType(GameTypeEnum gameType);

    /**
     * Count total games for a player
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE (g.player1.playerId = :playerId OR g.player2.playerId = :playerId) AND g.gameStatus = com.gomokumatching.model.enums.GameStatusEnum.COMPLETED")
    long countCompletedGamesByPlayer(@Param("playerId") UUID playerId);

    /**
     * Count wins for a player
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE g.winner.playerId = :playerId AND g.gameStatus = com.gomokumatching.model.enums.GameStatusEnum.COMPLETED")
    long countWinsByPlayer(@Param("playerId") UUID playerId);

    /**
     * Find recent games (for analytics)
     */
    @Query("SELECT g FROM Game g WHERE g.gameStatus = com.gomokumatching.model.enums.GameStatusEnum.COMPLETED ORDER BY g.endedAt DESC")
    List<Game> findRecentGames(Pageable pageable);
}
