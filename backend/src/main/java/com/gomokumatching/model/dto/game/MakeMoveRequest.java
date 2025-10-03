package com.gomokumatching.model.dto.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for making a move in a game.
 *
 * Board coordinates are 0-indexed (0-14 for 15x15 board)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MakeMoveRequest {

    /**
     * Row position (0-14)
     */
    @NotNull(message = "Row is required")
    @Min(value = 0, message = "Row must be between 0 and 14")
    @Max(value = 14, message = "Row must be between 0 and 14")
    private Integer row;

    /**
     * Column position (0-14)
     */
    @NotNull(message = "Column is required")
    @Min(value = 0, message = "Column must be between 0 and 14")
    @Max(value = 14, message = "Column must be between 0 and 14")
    private Integer col;
}
