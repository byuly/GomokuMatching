# Gomoku Matching Backend

## 🏗️ Architecture Overview

This Spring Boot application implements a **hybrid architecture** separating real-time gameplay from analytics/logging.

### Critical Path (Real-time)
```
Player Move → WebSocket → GameService → Redis Cache → Broadcast
                                           ↓
AI Turn → HTTP/gRPC → Python AI Service → GameService → Redis → Broadcast
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

## 🎯 Design Principles

✅ **Real-time first**: Critical gameplay uses WebSocket + Redis (sub-100ms latency)
✅ **Shadow logging**: Kafka records everything for replay/analytics (non-blocking)
✅ **AI as player**: AI moves logged identically to human moves
✅ **Redis for state**: Active games cached in-memory with TTL
✅ **PostgreSQL for history**: Final persistence via Kafka consumers

## 📊 Data Flow

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

## 🔧 Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Web Framework** | Spring Boot 3.5.4 | REST + WebSocket endpoints |
| **Real-time** | Spring WebSockets (STOMP) | Bidirectional game communication |
| **Caching** | Redis | Active game sessions + matchmaking queue |
| **Event Streaming** | Apache Kafka | Game replay logging + analytics |
| **Database** | PostgreSQL 17 | Player stats, game history |
| **Security** | Firebase Auth | JWT token validation |
| **AI Client** | RestTemplate/gRPC | Communication with Python service |

## 📁 Package Structure

```
com.gomokumatching/
├── controller/
│   ├── GameController.java           # WebSocket game moves
│   ├── MatchmakingController.java    # Queue management
│   ├── AuthController.java           # Firebase auth
│   └── ProfileController.java        # Player profiles
├── service/
│   ├── GameService.java              # Core game logic (int[][] board)
│   ├── MatchmakingService.java       # Redis queue (ZADD/ZPOP)
│   ├── AIServiceClient.java          # Python AI HTTP client
│   └── PlayerStatsService.java       # Stats management
├── kafka/
│   ├── producer/
│   │   ├── GameMoveProducer.java     # Logs all moves
│   │   └── MatchCreatedProducer.java # Logs match formation
│   └── consumer/
│       ├── GameMovesConsumer.java    # Persists moves to DB
│       └── MatchCreatedConsumer.java # Persists match history
├── model/
│   ├── Player.java                   # User entity
│   ├── GameSession.java              # In-memory game state
│   ├── Game.java                     # Persisted game record
│   └── GameMove.java                 # Individual move record
├── repository/
│   ├── PlayerRepository.java
│   ├── GameRepository.java
│   └── GameStatsRepository.java
└── config/
    ├── RedisConfig.java              # Redis connection
    ├── KafkaConfig.java              # Kafka topics/serialization
    ├── WebSocketConfig.java          # STOMP configuration
    ├── FirebaseConfig.java           # Firebase Admin SDK
    └── SecurityConfig.java           # Security filters

```

## 🚀 Running the Backend

### Prerequisites
- Java 21
- Maven 3.8+
- Docker (for Redis, Kafka, PostgreSQL)

### Start Dependencies
```bash
# From project root
docker-compose up -d postgres redis kafka
```

### Run Application
```bash
cd backend
./mvnw spring-boot:run
```

### Create Kafka Topics
```bash
docker-compose exec kafka kafka-topics --create --topic game-move-made --bootstrap-server localhost:9092
docker-compose exec kafka kafka-topics --create --topic match-created --bootstrap-server localhost:9092
```

### Verify
- **API**: http://localhost:8080
- **WebSocket**: ws://localhost:8080/ws
- **Health**: http://localhost:8080/actuator/health

## 🎮 Key Endpoints

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

## 📦 Dependencies

```xml
<!-- Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- Data -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Messaging -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.3.0</version>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

## 🔄 Kafka Event Schemas

### game-move-made
```json
{
  "gameId": "uuid",
  "playerId": "uuid",
  "isAI": false,
  "row": 7,
  "col": 7,
  "moveNumber": 1,
  "timestamp": "2024-10-01T12:00:00Z"
}
```

### match-created
```json
{
  "matchId": "uuid",
  "player1Id": "uuid",
  "player2Id": "uuid",  // null for AI games
  "gameType": "PVP",    // or "PVAI"
  "createdAt": "2024-10-01T12:00:00Z"
}
```

## 🎯 Benefits of This Architecture

| Benefit | How We Achieve It |
|---------|------------------|
| **Low Latency** | WebSocket + Redis (no DB blocking) |
| **Complete Audit** | Kafka logs every move |
| **Game Replay** | Reconstruct from Kafka events |
| **Scalability** | Redis for hot data, PostgreSQL for cold |
| **Decoupling** | Kafka consumers run independently |
| **AI Flexibility** | Python microservice, easy to retrain |
