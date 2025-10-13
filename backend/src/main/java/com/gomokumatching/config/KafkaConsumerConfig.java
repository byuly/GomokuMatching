package com.gomokumatching.config;

import com.gomokumatching.model.dto.kafka.GameMoveEvent;
import com.gomokumatching.model.dto.kafka.MatchCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for processing game events.
 *
 * Configuration:
 * - Group ID: gomoku-app (from application.yml)
 * - Auto offset reset: latest (only process new messages)
 * - Error handling: ErrorHandlingDeserializer for resilience
 * - Manual commit: false (auto-commit for simplicity)
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Base consumer configuration
     */
    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    /**
     * Consumer factory for GameMoveEvent
     */
    @Bean
    public ConsumerFactory<String, GameMoveEvent> gameMoveEventConsumerFactory() {
        Map<String, Object> props = consumerConfigs();

        // JSON deserialization with error handling
        JsonDeserializer<GameMoveEvent> jsonDeserializer = new JsonDeserializer<>(GameMoveEvent.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        ErrorHandlingDeserializer<GameMoveEvent> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                errorHandlingDeserializer
        );
    }

    /**
     * Listener container factory for GameMoveEvent
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, GameMoveEvent> gameMoveEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, GameMoveEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(gameMoveEventConsumerFactory());
        factory.setConcurrency(3); // 3 consumer threads (matches partition count)
        return factory;
    }

    /**
     * Consumer factory for MatchCreatedEvent
     */
    @Bean
    public ConsumerFactory<String, MatchCreatedEvent> matchCreatedEventConsumerFactory() {
        Map<String, Object> props = consumerConfigs();

        // JSON deserialization with error handling
        JsonDeserializer<MatchCreatedEvent> jsonDeserializer = new JsonDeserializer<>(MatchCreatedEvent.class);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        ErrorHandlingDeserializer<MatchCreatedEvent> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                errorHandlingDeserializer
        );
    }

    /**
     * Listener container factory for MatchCreatedEvent
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MatchCreatedEvent> matchCreatedEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MatchCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(matchCreatedEventConsumerFactory());
        factory.setConcurrency(3); // 3 consumer threads (matches partition count)
        return factory;
    }
}
