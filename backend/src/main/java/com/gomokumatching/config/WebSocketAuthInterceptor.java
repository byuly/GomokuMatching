package com.gomokumatching.config;

import com.gomokumatching.security.CustomUserDetailsService;
import com.gomokumatching.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * WebSocket channel interceptor for JWT authentication.
 *
 * Intercepts CONNECT frames to validate JWT tokens before establishing WebSocket connection.
 *
 * Token Sources (checked in order):
 * 1. Authorization header (preferred): "Bearer {token}"
 * 2. Custom token header: "token: {jwt}"
 * 3. First native header if contains "Bearer "
 *
 * Security Flow:
 * 1. Client sends CONNECT frame with JWT
 * 2. Interceptor extracts and validates token
 * 3. Sets user principal in message headers
 * 4. Principal available in @MessageMapping methods via Principal parameter
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Intercept messages before they are sent to the channel.
     *
     * For CONNECT commands, extract and validate JWT token.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwt = extractJwtFromHeaders(accessor);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                try {
                    UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
                    UserDetails userDetails = userDetailsService.loadUserById(userId);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // set user in STOMP session
                    accessor.setUser(authentication);

                    log.debug("WebSocket authenticated for user: {}", userId);
                } catch (Exception ex) {
                    log.error("WebSocket authentication failed", ex);
                    // don't set user - connection will proceed but @MessageMapping will fail authorization
                }
            } else {
                log.warn("WebSocket CONNECT without valid JWT token");
            }
        }

        return message;
    }

    /**
     * Extract JWT token from STOMP headers.
     *
     * Checks multiple possible locations for compatibility with different clients.
     */
    private String extractJwtFromHeaders(StompHeaderAccessor accessor) {
        // authorization header (standard)
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // cCustom "token" header (for clients that can't set Authorization)
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader;
        }

        // check all native headers for any containing "Bearer "
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null) {
            for (String header : authHeaders) {
                if (header != null && header.startsWith("Bearer ")) {
                    return header.substring(7);
                }
            }
        }

        log.debug("No JWT found in WebSocket CONNECT headers");
        return null;
    }
}
