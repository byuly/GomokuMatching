package com.gomokumatching.model;

import com.gomokumatching.model.enums.ProcessingStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "kafka_event_log", schema = "gomoku")
public class KafkaEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "topic_name", nullable = false, length = 100)
    private String topicName;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_game_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_player_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_payload", nullable = false, columnDefinition = "jsonb")
    private String eventPayload;

    @CreationTimestamp
    @Column(name = "event_timestamp", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime eventTimestamp;

    @Column(name = "kafka_partition", length = 10)
    private String kafkaPartition;

    @Column(name = "kafka_offset")
    private Long kafkaOffset;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", columnDefinition = "processing_status_enum")
    private ProcessingStatusEnum processingStatus = ProcessingStatusEnum.PENDING;
}
