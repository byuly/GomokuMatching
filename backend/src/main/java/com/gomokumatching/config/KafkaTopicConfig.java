package com.gomokumatching.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka topic configuration.
 *
 * Creates topics automatically on application startup if they don't exist.
 *
 * Topics:
 * - game-move-made: Every move (player & AI) for replay and analytics
 * - match-created: Match formation events for history and metrics
 *
 * Configuration:
 * - Partitions: 3 (allows parallel processing)
 * - Replication Factor: 1 (single broker setup)
 * - Retention: 7 days (configurable)
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Kafka admin client for topic management
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Topic: game-move-made
     *
     * Contains every move made in every game (player and AI moves).
     * Used for game replay, move analytics, and anti-cheat detection.
     */
    @Bean
    public NewTopic gameMoveMadeTopic() {
        return TopicBuilder.name("game-move-made")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Topic: match-created
     *
     * Contains match formation events (matchmaking, direct challenge, AI game).
     * Used for match history, matchmaking analytics, and system monitoring.
     */
    @Bean
    public NewTopic matchCreatedTopic() {
        return TopicBuilder.name("match-created")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Dead Letter Queue topic for failed event processing.
     *
     * Events that fail processing after retries are sent here for manual inspection.
     */
    @Bean
    public NewTopic deadLetterQueueTopic() {
        return TopicBuilder.name("game-events-dlq")
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .config("cleanup.policy", "delete")
                .build();
    }
}
