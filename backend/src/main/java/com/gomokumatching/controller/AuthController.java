package com.gomokumatching.controller;

import com.gomokumatching.model.dto.AuthResponse;
import com.gomokumatching.model.dto.LoginRequest;
import com.gomokumatching.model.dto.RegisterRequest;
import com.gomokumatching.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * Endpoints:
 * - POST /api/auth/register - Register new user
 * - POST /api/auth/login - Authenticate user
 *
 * All endpoints are public (no authentication required).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Register a new user.
     *
     * @param request Registration request with username, email, password
     * @return 201 Created with JWT tokens and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received for username: {}", request.getUsername());

        AuthResponse response = authService.register(request);

        logger.info("User registered successfully: {}", response.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and return JWT tokens.
     *
     * @param request Login request with username/email and password
     * @return 200 OK with JWT tokens and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for: {}", request.getUsernameOrEmail());

        AuthResponse response = authService.login(request);

        logger.info("User logged in successfully: {}", response.getUsername());

        return ResponseEntity.ok(response);
    }
}
