package com.gomokumatching.service.kafka;

import com.gomokumatching.model.GameSession;
import com.gomokumatching.model.dto.kafka.MatchCreatedEvent;
import com.gomokumatching.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// consumes match-created events from kafka streams, creates game session, sends websocket notifications
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchCreatedConsumer {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = "match-created",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "matchCreatedEventKafkaListenerContainerFactory"
    )
    public void consumeMatchCreated(
            @Payload MatchCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            log.info("match created event: game={}, type={}, source={}, players={}/{}, partition={}, offset={}",
                    event.getGameId(),
                    event.getGameType(),
                    event.getMatchSource(),
                    event.getPlayer1Id(),
                    event.getPlayer2Id() != null ? event.getPlayer2Id() : "AI-" + event.getAiDifficulty(),
                    partition,
                    offset);

            if ("MATCHMAKING".equals(event.getMatchSource())) {
                processMatchmakingMatch(event);
            }

        } catch (Exception e) {
            log.error("failed to process match-created event: game={}, error={}",
                    event.getGameId(), e.getMessage(), e);
        }
    }

    private void processMatchmakingMatch(MatchCreatedEvent event) {
        try {
            GameSession session = gameService.createPvPGame(
                    event.getPlayer1Id(),
                    event.getPlayer2Id()
            );

            log.info("game session created for match: gameId={}, player1={}, player2={}",
                    session.getGameId(),
                    event.getPlayer1Id(),
                    event.getPlayer2Id());

            notifyPlayersOfMatch(session);

        } catch (Exception e) {
            log.error("failed to create game for match event: game={}, error={}",
                    event.getGameId(), e.getMessage(), e);
        }
    }

    // send websocket notifications to both players at /user/{userId}/queue/match-found
    private void notifyPlayersOfMatch(GameSession session) {
        try {
            Map<String, Object> matchNotification = new HashMap<>();
            matchNotification.put("gameId", session.getGameId());
            matchNotification.put("gameType", "HUMAN_VS_HUMAN");
            matchNotification.put("opponentId", null);
            matchNotification.put("websocketTopic", "/topic/game/" + session.getGameId());
            matchNotification.put("message", "Match found! Your opponent is ready.");

            Map<String, Object> player1Notification = new HashMap<>(matchNotification);
            player1Notification.put("opponentId", session.getPlayer2Id());
            player1Notification.put("yourPlayerNumber", 1);
            player1Notification.put("yourColor", "BLACK");

            messagingTemplate.convertAndSendToUser(
                    session.getPlayer1Id().toString(),
                    "/queue/match-found",
                    player1Notification
            );

            Map<String, Object> player2Notification = new HashMap<>(matchNotification);
            player2Notification.put("opponentId", session.getPlayer1Id());
            player2Notification.put("yourPlayerNumber", 2);
            player2Notification.put("yourColor", "WHITE");

            messagingTemplate.convertAndSendToUser(
                    session.getPlayer2Id().toString(),
                    "/queue/match-found",
                    player2Notification
            );

            log.info("websocket notifications sent to players {} and {}",
                    session.getPlayer1Id(), session.getPlayer2Id());

        } catch (Exception e) {
            log.error("failed to send match notifications for game {}: {}",
                    session.getGameId(), e.getMessage(), e);
        }
    }
}
