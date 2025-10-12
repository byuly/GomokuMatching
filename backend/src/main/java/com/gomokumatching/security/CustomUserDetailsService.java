package com.gomokumatching.security;

import com.gomokumatching.model.Player;
import com.gomokumatching.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for loading user-specific data.
 *
 * Security Features:
 * - Loads user from database by username
 * - Provides detailed exception messages
 * - Transactional for consistent reads
 * - Integrates with Spring Security authentication
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final PlayerRepository playerRepository;

    /**
     * Load user by username for Spring Security authentication.
     *
     * @param username Username to load
     * @return UserDetails object containing user information
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Player player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username
                ));

        return new CustomUserDetails(player);
    }

    /**
     * Load user by email (alternative lookup).
     * TODO: add email based login
     *
     * @param email Email to load
     * @return UserDetails object containing user information
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        Player player = playerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        return new CustomUserDetails(player);
    }

    /**
     * Load user by ID (for JWT token validation).
     * Used by JwtAuthenticationFilter to load user after validating token.
     *
     * @param userId User's UUID
     * @return UserDetails object containing user information
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(java.util.UUID userId) throws UsernameNotFoundException {
        Player player = playerRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with ID: " + userId
                ));

        return new CustomUserDetails(player);
    }
}
