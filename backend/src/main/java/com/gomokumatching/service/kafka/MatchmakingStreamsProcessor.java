package com.gomokumatching.service.kafka;

import com.gomokumatching.model.dto.kafka.MatchCreatedEvent;
import com.gomokumatching.model.dto.kafka.MatchmakingState;
import com.gomokumatching.model.dto.kafka.QueueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;
import java.util.UUID;

// kafka streams processor for event-driven matchmaking
// aggregates queue events into state, creates matches when >=2 players available
@Configuration
@Slf4j
@RequiredArgsConstructor
public class MatchmakingStreamsProcessor {

    private final JsonSerde<QueueEvent> queueEventSerde;
    private final JsonSerde<MatchmakingState> matchmakingStateSerde;
    private final GameEventProducer gameEventProducer;
    private final QueueEventProducer queueEventProducer;

    private static final String QUEUE_EVENTS_TOPIC = "matchmaking-queue-events";
    private static final String STATE_STORE_NAME = "matchmaking-state-store";

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        log.info("building kafka streams matchmaking topology");

        KStream<String, QueueEvent> queueEvents = streamsBuilder.stream(
                QUEUE_EVENTS_TOPIC,
                Consumed.with(Serdes.String(), queueEventSerde)
        );

        // aggregate all events into single global queue (key = "global-queue" ensures strict ordering)
        KTable<String, MatchmakingState> queueState = queueEvents
                .selectKey((key, event) -> "global-queue")
                .groupByKey(Grouped.with(Serdes.String(), queueEventSerde))
                .aggregate(
                        MatchmakingState::new,
                        (key, event, state) -> {
                            if (event.getAction() == QueueEvent.QueueAction.PLAYER_JOINED) {
                                log.debug("player joined queue: {}", event.getPlayerId());
                                return state.addPlayer(event);
                            } else if (event.getAction() == QueueEvent.QueueAction.PLAYER_LEFT) {
                                log.debug("player left queue: {}", event.getPlayerId());
                                return state.removePlayer(event);
                            } else {
                                log.warn("unknown queue action: {}", event.getAction());
                                return state;
                            }
                        },
                        Materialized.<String, MatchmakingState, org.apache.kafka.streams.state.KeyValueStore<org.apache.kafka.common.utils.Bytes, byte[]>>as(STATE_STORE_NAME)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(matchmakingStateSerde)
                );

        // when >=2 players available, create match and publish cleanup events
        queueState
                .toStream()
                .filter((key, state) -> {
                    boolean canMatch = state != null && state.canCreateMatch();
                    if (canMatch) {
                        log.info("queue has {} players, creating match", state.getWaitingCount());
                    }
                    return canMatch;
                })
                .foreach((key, state) -> {
                    MatchmakingState.MatchResult result = state.createMatch();

                    if (result.isMatchCreated()) {
                        UUID player1Id = result.getPlayer1Id();
                        UUID player2Id = result.getPlayer2Id();
                        UUID gameId = UUID.randomUUID();

                        log.info("match created: game={}, player1={}, player2={}",
                                gameId, player1Id, player2Id);

                        try {
                            MatchCreatedEvent matchEvent = MatchCreatedEvent.forMatchmaking(
                                    gameId, player1Id, player2Id
                            );
                            gameEventProducer.publishMatchCreated(matchEvent);

                            // publish PLAYER_LEFT events to remove matched players from state
                            queueEventProducer.publishPlayerLeft(player1Id);
                            queueEventProducer.publishPlayerLeft(player2Id);

                        } catch (Exception e) {
                            log.error("failed to process match creation: game={}, error={}",
                                    gameId, e.getMessage(), e);
                        }
                    }
                });

        log.info("kafka streams matchmaking topology built successfully");
    }
}
