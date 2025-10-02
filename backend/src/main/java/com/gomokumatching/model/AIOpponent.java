package com.gomokumatching.model;

import com.gomokumatching.model.enums.DifficultyLevelEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "ai_opponent", schema = "gomoku")
public class AIOpponent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ai_id")
    private UUID aiId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    private DifficultyLevelEnum difficultyLevel;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "model_file_path", nullable = false, length = 500)
    private String modelFilePath;

    @Column(name = "win_rate_target", nullable = false, precision = 5, scale = 4)
    private BigDecimal winRateTarget;

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastUpdated;
}
