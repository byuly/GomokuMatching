package com.gomokumatching.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time Player vs Player gameplay.
 *
 * Architecture:
 * - STOMP protocol over WebSocket (with SockJS fallback)
 * - In-memory message broker for pub/sub
 * - JWT authentication via custom interceptor
 * - Topic-based broadcasting for game updates
 *
 * Connection Flow:
 * 1. Client connects to /ws endpoint (SockJS handshake)
 * 2. Client sends CONNECT frame with JWT in headers
 * 3. WebSocketAuthInterceptor validates JWT
 * 4. Client subscribes to /topic/game/{gameId}
 * 5. Client sends messages to /app/game/{gameId}/move
 * 6. Server broadcasts updates to /topic/game/{gameId}
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * Configure STOMP endpoint with SockJS fallback.
     *
     * Endpoint: /ws
     * - Allows WebSocket connections
     * - SockJS fallback for browsers that don't support WebSocket
     * - CORS enabled for development (configure properly for production)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // TODO: Configure proper CORS for prod
                .withSockJS();  // enabling fallback with SockJS
    }

    /**
     * Configure message broker.
     *
     * Application destination prefix: /app
     * - Messages sent to /app/* are routed to @MessageMapping methods
     *
     * Broker destination prefix: /topic
     * - Messages sent to /topic/* are broadcast to all subscribers
     *
     * User destination prefix: /user
     * - Messages sent to /user/{username}/* are sent to specific user
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // enable simple in-memory message broker
        // consider RabbitMQ or ActiveMQ
        registry.enableSimpleBroker("/topic", "/user");

        // application destination prefix
        // client sends to: /app/game/{gameId}/move
        // ,apped to: @MessageMapping("/game/{gameId}/move")
        registry.setApplicationDestinationPrefixes("/app");

        // user destination prefix for user-specific messages
        // used for sending errors to specific users
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configure client inbound channel with JWT authentication interceptor.
     *
     * This interceptor validates JWT tokens before allowing WebSocket connection.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
