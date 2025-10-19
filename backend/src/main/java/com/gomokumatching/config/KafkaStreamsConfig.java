package com.gomokumatching.config;

import com.gomokumatching.model.dto.kafka.MatchmakingState;
import com.gomokumatching.model.dto.kafka.QueueEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.HashMap;
import java.util.Map;

// kafka streams config for real-time matchmaking
// uses rocksdb-backed state store for fault tolerance
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "matchmaking-streams");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams/matchmaking");

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class);

        // at_least_once for single-broker dev setup (use exactly_once_v2 in production with replication)
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);

        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024);

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.gomokumatching.model.dto.kafka");

        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public JsonSerde<QueueEvent> queueEventSerde() {
        JsonSerde<QueueEvent> serde = new JsonSerde<>(QueueEvent.class);
        serde.configure(
                Map.of(JsonDeserializer.TRUSTED_PACKAGES, "com.gomokumatching.model.dto.kafka"),
                false
        );
        return serde;
    }

    @Bean
    public JsonSerde<MatchmakingState> matchmakingStateSerde() {
        JsonSerde<MatchmakingState> serde = new JsonSerde<>(MatchmakingState.class);
        serde.configure(
                Map.of(JsonDeserializer.TRUSTED_PACKAGES, "com.gomokumatching.model.dto.kafka"),
                false
        );
        return serde;
    }
}
