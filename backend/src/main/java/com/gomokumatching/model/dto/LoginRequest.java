package com.gomokumatching.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for user login request.
 *
 * Supports both username and email login.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;
}
