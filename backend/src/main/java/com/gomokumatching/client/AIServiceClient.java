package com.gomokumatching.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for communicating with the AI microservice.
 *
 * Handles all HTTP requests to the Django AI service for move generation,
 * position evaluation, and game validation.
 */
@Component
public class AIServiceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AIServiceClient(
            RestTemplate restTemplate,
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    /**
     * Get the best AI move for the current board state.
     *
     * @param boardState 15x15 2D array (0=empty, 1=black, 2=white)
     * @param currentPlayer Current player (1=black, 2=white)
     * @param difficulty AI difficulty ('easy', 'medium', 'hard', 'expert')
     * @return AIMoveResponse with row, col, and difficulty
     * @throws AIServiceException if communication fails
     */
    public AIMoveResponse getAIMove(int[][] boardState, int currentPlayer, String difficulty) {
        String url = aiServiceUrl + "/api/move/";

        Map<String, Object> request = new HashMap<>();
        request.put("board_state", boardState);
        request.put("current_player", currentPlayer);
        request.put("difficulty", difficulty);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<AIMoveResponse> response = restTemplate.postForEntity(
                url, entity, AIMoveResponse.class
            );

            if (response.getBody() == null) {
                throw new AIServiceException("AI service returned empty response");
            }

            return response.getBody();

        } catch (RestClientException e) {
            throw new AIServiceException("Failed to get AI move: " + e.getMessage(), e);
        }
    }

    /**
     * Check if AI service is healthy.
     *
     * @return true if service is responsive
     */
    public boolean isHealthy() {
        String url = aiServiceUrl + "/api/health/";

        try {
            ResponseEntity<HealthCheckResponse> response = restTemplate.getForEntity(
                url, HealthCheckResponse.class
            );

            return response.getBody() != null &&
                   "healthy".equals(response.getBody().getStatus());

        } catch (RestClientException e) {
            return false;
        }
    }

    /**
     * Response object for AI move.
     */
    public static class AIMoveResponse {
        private int row;
        private int col;
        private String difficulty;

        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }

        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    }

    /**
     * Response object for health check.
     */
    public static class HealthCheckResponse {
        private String status;
        private String version;
        private boolean model_loaded;
        private String device;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public boolean isModelLoaded() { return model_loaded; }
        public void setModel_loaded(boolean model_loaded) { this.model_loaded = model_loaded; }

        public String getDevice() { return device; }
        public void setDevice(String device) { this.device = device; }
    }

    /**
     * Exception thrown when AI service communication fails.
     */
    public static class AIServiceException extends RuntimeException {
        public AIServiceException(String message) {
            super(message);
        }

        public AIServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
