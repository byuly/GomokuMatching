## backend overview

### Critical Path (Real-time)
```
Player Move → WebSocket → GameService → Redis Cache → Broadcast
                                           ↓
AI Turn → HTTP → Python AI Service → GameService → Redis → Broadcast
```

### Shadow Path (Async)
```
All Moves → Kafka (game-move-made) → GameMovesConsumer → PostgreSQL (replay data)
Match Created → Kafka (match-created) → MatchCreatedConsumer → PostgreSQL (history)
```

### Matchmaking Flow
```
Player → MatchmakingService → Redis Queue (ZADD/ZPOPMIN)
                                    ↓
                              Match Paired → WebSocket Notification
                                    ↓
                              Kafka (match-created) → Analytics
```
## data flow

### Player vs Player (PvP)
1. Move received via WebSocket
2. Validate against Redis game session (`int[][] board`)
3. Update Redis cache
4. Broadcast to both players via WebSocket
5. **Async**: Log to Kafka `game-move-made` topic

### Player vs AI (PvAI)
1. Player move processed (same as PvP)
2. Detect AI turn → HTTP request to `ai-service:8000`
3. Python AI service returns move coordinates
4. Apply AI move to Redis, broadcast to player
5. **Async**: Log AI move to same `game-move-made` topic

### Matchmaking
1. Player joins queue → Redis `ZADD matchmaking:queue {timestamp} {playerId}`
2. Service polls → `ZPOPMIN matchmaking:queue 2` (get 2 oldest)
3. If pair found → Create game session, notify via WebSocket
4. **Async**: Log to Kafka `match-created` topic

## running

### versions
- Java 21
- Maven 3.8+
- Docker (for Redis, Kafka, PostgreSQL)

```bash
# From project root
docker-compose up -d postgres redis kafka
```

## endpoints

### REST API
```
POST   /api/auth/login              # Firebase authentication
GET    /api/profile                 # Get player profile
PUT    /api/profile/username        # Update username
POST   /api/matchmaking/queue       # Join matchmaking
POST   /api/game/ai/move            # Player vs AI move
```

### WebSocket (STOMP)
```
SUBSCRIBE /topic/game/{gameId}      # Receive game updates
SEND      /app/game/move            # Send player move
```
