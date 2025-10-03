package com.gomokumatching.model.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Error message DTO for WebSocket errors.
 *
 * Sent to specific user's error queue when WebSocket operation fails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMessageDTO {

    /**
     * Error code (e.g., INVALID_MOVE, UNAUTHORIZED, GAME_NOT_FOUND)
     */
    private String errorCode;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Timestamp of error
     */
    private LocalDateTime timestamp;

    /**
     * Additional error details (optional)
     */
    private String details;

    /**
     * Create error message
     */
    public static ErrorMessageDTO of(String errorCode, String message) {
        return ErrorMessageDTO.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error message with details
     */
    public static ErrorMessageDTO of(String errorCode, String message, String details) {
        return ErrorMessageDTO.builder()
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
