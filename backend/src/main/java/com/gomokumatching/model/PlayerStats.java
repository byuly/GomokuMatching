package com.gomokumatching.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "player_stats", schema = "gomoku")
public class PlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "stats_id")
    private UUID statsId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", referencedColumnName = "player_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player;

    @Column(name = "total_games")
    private int totalGames = 0;

    @Column(name = "wins")
    private int wins = 0;

    @Column(name = "losses")
    private int losses = 0;

    @Column(name = "draws")
    private int draws = 0;

    @Column(name = "current_mmr")
    private int currentMmr = 1000;

    @Column(name = "peak_mmr")
    private int peakMmr = 1000;

    @Column(name = "current_streak")
    private int currentStreak = 0;

    @Column(name = "longest_win_streak")
    private int longestWinStreak = 0;

    @UpdateTimestamp
    @Column(name = "last_updated", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastUpdated;
}
