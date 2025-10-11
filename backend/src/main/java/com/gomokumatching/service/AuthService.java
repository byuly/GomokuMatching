package com.gomokumatching.service;

import com.gomokumatching.exception.BadCredentialsException;
import com.gomokumatching.exception.ResourceAlreadyExistsException;
import com.gomokumatching.model.Player;
import com.gomokumatching.model.dto.AuthResponse;
import com.gomokumatching.model.dto.LoginRequest;
import com.gomokumatching.model.dto.RegisterRequest;
import com.gomokumatching.model.enums.AccountStatusEnum;
import com.gomokumatching.repository.PlayerRepository;
import com.gomokumatching.security.CustomUserDetails;
import com.gomokumatching.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Authentication service handling user registration and login.
 * - BCrypt password hashing (work factor 12)
 * - Duplicate username/email validation
 * - Account status checking (active, suspended, deleted)
 * - JWT token generation
 * - Transactional consistency
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user.
     *
     * @param request Registration request
     * @return Authentication response with JWT tokens
     * @throws ResourceAlreadyExistsException if username or email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user: {}", request.getUsername());

        // check if username already exists
        if (playerRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException(
                    "Username already exists: " + request.getUsername()
            );
        }

        // check if email already exists
        if (playerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Email already exists: " + request.getEmail()
            );
        }

        // create new player
        Player player = new Player();
        player.setUsername(request.getUsername());
        player.setEmail(request.getEmail().toLowerCase());
        player.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        player.setActive(true);
        player.setAccountStatus(AccountStatusEnum.ACTIVE);

        // save player
        Player savedPlayer = playerRepository.save(player);

        logger.info("Successfully registered user: {} with ID: {}",
                savedPlayer.getUsername(), savedPlayer.getPlayerId());

        // generate JWT tokens
        String accessToken = jwtTokenProvider.generateTokenFromUserId(savedPlayer.getPlayerId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedPlayer.getPlayerId());

        return AuthResponse.withRefreshToken(
                savedPlayer.getPlayerId(),
                savedPlayer.getUsername(),
                savedPlayer.getEmail(),
                accessToken,
                refreshToken,
                jwtTokenProvider.getExpirationInSeconds()
        );
    }

    /**
     * Authenticate user and generate JWT tokens.
     *
     * @param request Login request (supports username or email)
     * @return Authentication response with JWT tokens
     * @throws BadCredentialsException if credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("Attempting login for: {}", request.getUsernameOrEmail());

        // determine if input is email or username
        boolean isEmail = request.getUsernameOrEmail().contains("@");

        // find player
        Player player;
        if (isEmail) {
            player = playerRepository.findByEmailIgnoreCase(request.getUsernameOrEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        } else {
            player = playerRepository.findByUsername(request.getUsernameOrEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        }

        // check account status
        if (!player.isActive()) {
            throw new BadCredentialsException("Account is inactive");
        }

        if (player.getAccountStatus() == AccountStatusEnum.SUSPENDED) {
            throw new BadCredentialsException("Account is suspended");
        }

        if (player.getAccountStatus() == AccountStatusEnum.DELETED) {
            throw new BadCredentialsException("Account has been deleted");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        player.getUsername(), // use username for authentication
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // updating the last timestamp
        player.setLastLogin(OffsetDateTime.now());
        playerRepository.save(player);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // generate new JWT token
        String accessToken = jwtTokenProvider.generateTokenFromUserId(userDetails.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getId());

        logger.info("Successfully authenticated user: {} (ID: {})",
                userDetails.getUsername(), userDetails.getId());

        return AuthResponse.withRefreshToken(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                accessToken,
                refreshToken,
                jwtTokenProvider.getExpirationInSeconds()
        );
    }
}
