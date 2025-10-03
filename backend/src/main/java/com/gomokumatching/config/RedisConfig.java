package com.gomokumatching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for game session caching and matchmaking queue.
 *
 * Features:
 * - Game sessions stored with 2-hour TTL
 * - Matchmaking queue using sorted sets (ZADD/ZPOPMIN)
 * - JSON serialization for complex objects
 */
@Configuration
public class RedisConfig {

    /**
     * Configure RedisTemplate for storing game sessions and other objects.
     *
     * Key Serializer: String (for readable keys like "game:session:uuid")
     * Value Serializer: JSON (for GameSession objects with int[][] board)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys (readable in Redis CLI)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values (supports complex objects)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
