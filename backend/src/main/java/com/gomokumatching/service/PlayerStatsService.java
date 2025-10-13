package com.gomokumatching.service;

import com.gomokumatching.model.Player;
import com.gomokumatching.model.PlayerStats;
import com.gomokumatching.repository.PlayerRepository;
import com.gomokumatching.repository.PlayerStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing player statistics and MMR (Matchmaking Rating).
 *
 * Responsibilities:
 * - Update player stats after games (wins, losses, draws)
 * - Calculate MMR using Elo rating system
 * - Track win streaks and peak MMR
 * - Initialize stats for new players
 *
 * MMR System (Elo Rating):
 * - Starting MMR: 1000
 * - K-factor: 32 (for players with < 30 games), 16 (experienced players)
 * - Formula: newMMR = oldMMR + K * (actualScore - expectedScore)
 * - Expected score: 1 / (1 + 10^((opponentMMR - playerMMR) / 400))
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerStatsService {

    private final PlayerStatsRepository playerStatsRepository;
    private final PlayerRepository playerRepository;

    private static final int STARTING_MMR = 1000;
    private static final int K_FACTOR_NEW = 32; // For players with < 30 games
    private static final int K_FACTOR_EXPERIENCED = 16; // For players with >= 30 games

    /**
     * Update player stats after a game completes.
     *
     * Updates for both players (or one player in PvAI):
     * - Win/loss/draw counters
     * - MMR calculation
     * - Win streaks
     * - Peak MMR tracking
     *
     * @param player1Id Player 1 ID
     * @param player2Id Player 2 ID (null for AI games)
     * @param winnerId Winner ID (null for draw or AI win)
     * @param isDraw Whether game ended in draw
     */
    @Transactional
    public void updateStatsAfterGame(UUID player1Id, UUID player2Id, UUID winnerId, boolean isDraw) {
        try {
            PlayerStats player1Stats = getOrCreateStats(player1Id);

            if (player2Id != null) {
                // PvP game - update both players
                PlayerStats player2Stats = getOrCreateStats(player2Id);
                updateStatsForPvP(player1Stats, player2Stats, player1Id, player2Id, winnerId, isDraw);
            } else {
                // PvAI game - update only human player
                updateStatsForPvAI(player1Stats, winnerId, isDraw, player1Id);
            }

            log.info("✅ Updated player stats: player1={}, player2={}, winner={}, draw={}",
                    player1Id, player2Id, winnerId, isDraw);

        } catch (Exception e) {
            log.error("❌ Failed to update player stats: player1={}, player2={}, error={}",
                    player1Id, player2Id, e.getMessage(), e);
            // stats updates shouldn't fail game completion
        }
    }

    /**
     * Update stats for both players in PvP game.
     *
     * Calculates MMR changes using Elo rating system.
     */
    private void updateStatsForPvP(
            PlayerStats player1Stats,
            PlayerStats player2Stats,
            UUID player1Id,
            UUID player2Id,
            UUID winnerId,
            boolean isDraw
    ) {
        // determine game outcome
        double player1Score, player2Score;
        if (isDraw) {
            player1Score = 0.5;
            player2Score = 0.5;
            player1Stats.setDraws(player1Stats.getDraws() + 1);
            player2Stats.setDraws(player2Stats.getDraws() + 1);
            player1Stats.setCurrentStreak(0);
            player2Stats.setCurrentStreak(0);
        } else if (player1Id.equals(winnerId)) {
            player1Score = 1.0;
            player2Score = 0.0;
            player1Stats.setWins(player1Stats.getWins() + 1);
            player2Stats.setLosses(player2Stats.getLosses() + 1);
            updateStreak(player1Stats, true);
            player2Stats.setCurrentStreak(0);
        } else {
            player1Score = 0.0;
            player2Score = 1.0;
            player1Stats.setLosses(player1Stats.getLosses() + 1);
            player2Stats.setWins(player2Stats.getWins() + 1);
            player1Stats.setCurrentStreak(0);
            updateStreak(player2Stats, true);
        }

        // calculate MMR changes
        int player1NewMMR = calculateNewMMR(
                player1Stats.getCurrentMmr(),
                player2Stats.getCurrentMmr(),
                player1Score,
                player1Stats.getTotalGames()
        );

        int player2NewMMR = calculateNewMMR(
                player2Stats.getCurrentMmr(),
                player1Stats.getCurrentMmr(),
                player2Score,
                player2Stats.getTotalGames()
        );

        // update MMR and peak
        player1Stats.setCurrentMmr(player1NewMMR);
        player2Stats.setCurrentMmr(player2NewMMR);

        if (player1NewMMR > player1Stats.getPeakMmr()) {
            player1Stats.setPeakMmr(player1NewMMR);
        }
        if (player2NewMMR > player2Stats.getPeakMmr()) {
            player2Stats.setPeakMmr(player2NewMMR);
        }

        // increment total games
        player1Stats.setTotalGames(player1Stats.getTotalGames() + 1);
        player2Stats.setTotalGames(player2Stats.getTotalGames() + 1);

        // save to database
        playerStatsRepository.save(player1Stats);
        playerStatsRepository.save(player2Stats);

        log.debug("PvP stats updated: player1 MMR {} → {}, player2 MMR {} → {}",
                player1Stats.getCurrentMmr() - (player1NewMMR - player1Stats.getCurrentMmr()),
                player1NewMMR,
                player2Stats.getCurrentMmr() - (player2NewMMR - player2Stats.getCurrentMmr()),
                player2NewMMR);
    }

    /**
     * Update stats for player in PvAI game.
     *
     * Simpler than PvP - no MMR changes against AI
     */
    private void updateStatsForPvAI(PlayerStats playerStats, UUID winnerId, boolean isDraw, UUID playerId) {
        if (isDraw) {
            playerStats.setDraws(playerStats.getDraws() + 1);
            playerStats.setCurrentStreak(0);
        } else if (playerId.equals(winnerId)) {
            playerStats.setWins(playerStats.getWins() + 1);
            updateStreak(playerStats, true);
        } else {
            // AI won
            playerStats.setLosses(playerStats.getLosses() + 1);
            playerStats.setCurrentStreak(0);
        }

        playerStats.setTotalGames(playerStats.getTotalGames() + 1);
        playerStatsRepository.save(playerStats);

        log.debug("PvAI stats updated: player={}, total games={}, wins={}",
                playerId, playerStats.getTotalGames(), playerStats.getWins());
    }

    /**
     * Calculate new MMR using Elo rating system.
     *
     * Formula: newMMR = oldMMR + K * (actualScore - expectedScore)
     *
     * @param playerMMR Current player MMR
     * @param opponentMMR Opponent MMR
     * @param actualScore Actual game score (1.0 = win, 0.5 = draw, 0.0 = loss)
     * @param gamesPlayed Number of games player has played
     * @return New MMR
     */
    private int calculateNewMMR(int playerMMR, int opponentMMR, double actualScore, int gamesPlayed) {
        // K-factor: higher for new players (more volatile rating)
        int kFactor = (gamesPlayed < 30) ? K_FACTOR_NEW : K_FACTOR_EXPERIENCED;

        // calculate expected score using Elo formula
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentMMR - playerMMR) / 400.0));

        // calculate MMR change
        int mmrChange = (int) Math.round(kFactor * (actualScore - expectedScore));

        // apply change (minimum MMR is 0)
        int newMMR = Math.max(0, playerMMR + mmrChange);

        return newMMR;
    }

    /**
     * Update win streak for player.
     *
     * Tracks current streak and updates longest win streak if needed.
     *
     * @param stats Player stats
     * @param isWin Whether player won
     */
    private void updateStreak(PlayerStats stats, boolean isWin) {
        if (isWin) {
            stats.setCurrentStreak(stats.getCurrentStreak() + 1);

            if (stats.getCurrentStreak() > stats.getLongestWinStreak()) {
                stats.setLongestWinStreak(stats.getCurrentStreak());
            }
        } else {
            stats.setCurrentStreak(0);
        }
    }

    /**
     * Get or create player stats.
     *
     * If stats don't exist, create with default values (MMR 1000).
     *
     * @param playerId Player ID
     * @return Player stats (existing or newly created)
     */
    private PlayerStats getOrCreateStats(UUID playerId) {
        return playerStatsRepository.findByPlayer_PlayerId(playerId)
                .orElseGet(() -> createDefaultStats(playerId));
    }

    /**
     * Create default stats for new player.
     *
     * @param playerId Player ID
     * @return New PlayerStats with default values
     */
    private PlayerStats createDefaultStats(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalStateException("Player not found: " + playerId));

        PlayerStats stats = new PlayerStats();
        stats.setPlayer(player);
        stats.setTotalGames(0);
        stats.setWins(0);
        stats.setLosses(0);
        stats.setDraws(0);
        stats.setCurrentMmr(STARTING_MMR);
        stats.setPeakMmr(STARTING_MMR);
        stats.setCurrentStreak(0);
        stats.setLongestWinStreak(0);

        PlayerStats saved = playerStatsRepository.save(stats);

        log.info("Created default stats for player {}: MMR={}", playerId, STARTING_MMR);

        return saved;
    }

    /**
     * Get player stats by player ID.
     *
     * @param playerId Player ID
     * @return Player stats, or null if not found
     */
    public PlayerStats getPlayerStats(UUID playerId) {
        return playerStatsRepository.findByPlayer_PlayerId(playerId).orElse(null);
    }
}
