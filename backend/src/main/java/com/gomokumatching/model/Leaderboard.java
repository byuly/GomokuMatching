package com.gomokumatching.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "leaderboard", schema = "gomoku",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"player_id", "season_identifier"}),
                @UniqueConstraint(columnNames = {"current_rank", "season_identifier"})
        })
public class Leaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "leaderboard_id")
    private UUID leaderboardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player;

    @Column(name = "current_rank", nullable = false)
    private int currentRank;

    @Column(name = "previous_rank")
    private Integer previousRank;

    @Column(name = "mmr_score", nullable = false)
    private int mmrScore;

    @Column(name = "games_played_season")
    private int gamesPlayedSeason = 0;

    @Column(name = "wins_season")
    private int winsSeason = 0;

    @Column(name = "win_rate_season", precision = 5, scale = 4)
    private BigDecimal winRateSeason = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "last_rank_update", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastRankUpdate;

    @Column(name = "season_identifier", nullable = false, length = 50)
    private String seasonIdentifier = "season_2025_1";
}
