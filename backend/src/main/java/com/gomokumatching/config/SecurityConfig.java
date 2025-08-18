package com.gomokumatching.config;

import com.gomokumatching.security.FirebaseFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures the application's security settings, including the security filter chain.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the security filter chain that applies to all HTTP requests.
     *
     * How it works:
     * 1. CSRF (Cross-Site Request Forgery) is disabled, as it's not needed for a stateless API
     *    that uses token-based authentication.
     * 2. Session management is set to STATELESS, meaning the server does not create or maintain sessions.
     *    Every request must be independently authenticated, typically with a token.
     * 3. It defines authorization rules: endpoints under "/api/public/**" are permitted for everyone,
     *    while all other endpoints under "/api/**" require authentication.
     * 4. It adds the custom FirebaseFilter before the standard UsernamePasswordAuthenticationFilter.
     *    This ensures that the Firebase ID token is verified for every incoming request before
     *    the request reaches the controller.
     *
     * @param http The HttpSecurity object to configure.
     * @param firebaseFilter The custom filter for verifying Firebase ID tokens.
     * @return The configured SecurityFilterChain.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, FirebaseFilter firebaseFilter) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())

            // Set session management to stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Define authorization rules
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**", "/api/profiles/sync").permitAll() // Public endpoints
                .anyRequest().authenticated() // All other endpoints require authentication
            )

            // Add the custom Firebase filter to the chain
            .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
