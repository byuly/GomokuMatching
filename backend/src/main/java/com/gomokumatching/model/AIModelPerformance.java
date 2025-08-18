package com.gomokumatching.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "ai_model_performance", schema = "gomoku")
public class AIModelPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "performance_id")
    private UUID performanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_opponent_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AIOpponent aiOpponent;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "games_played")
    private int gamesPlayed = 0;

    @Column(name = "games_won")
    private int gamesWon = 0;

    @Column(name = "actual_win_rate", precision = 5, scale = 4)
    private BigDecimal actualWinRate = BigDecimal.ZERO;

    @Column(name = "target_win_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal targetWinRate;

    @Column(name = "total_moves_made")
    private int totalMovesMade = 0;

    @Column(name = "average_thinking_time_ms", precision = 10, scale = 2)
    private BigDecimal averageThinkingTimeMs;

    @Column(name = "performance_period_start", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime performancePeriodStart;

    @Column(name = "performance_period_end", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime performancePeriodEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "performance_metrics", columnDefinition = "jsonb")
    private String performanceMetrics;
}
