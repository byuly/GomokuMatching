package com.gomokumatching.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "player_ai_matchup", schema = "gomoku",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "ai_opponent_id"}))
public class PlayerAIMatchup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "matchup_id")
    private UUID matchupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_opponent_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AIOpponent aiOpponent;

    @Column(name = "games_played")
    private int gamesPlayed = 0;

    @Column(name = "player_wins")
    private int playerWins = 0;

    @Column(name = "ai_wins")
    private int aiWins = 0;

    @Column(name = "draws")
    private int draws = 0;

    @Column(name = "player_win_rate", precision = 5, scale = 4)
    private BigDecimal playerWinRate = BigDecimal.ZERO;

    @Column(name = "last_game_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastGameDate;

    @Column(name = "first_game_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime firstGameDate;
}
