# 🎮 Gomoku Matching – Classic Strategy Board Game with AI Opponents

## 🧠 Overview

**Gomoku 1v1** is a competitive, real-time strategy board game where two players face off to place five stones in a row on a grid. The game supports both human-vs-human matches and human-vs-AI matches, featuring machine learning-powered AI opponents with varying skill levels.

Built with Spring Boot, Redis, PostgreSQL, and a Django-based AI service, the application delivers real-time gameplay with WebSocket support and persistent game history.

---

## 🎯 Game Concept

- Two players compete on a 15x15 grid to align five stones horizontally, vertically, or diagonally
- Player-vs-player matches use WebSocket for real-time bidirectional communication
- Player-vs-AI matches communicate with Django microservice for move calculation
- Active game state cached in Redis with 2-hour TTL
- Completed games persisted to PostgreSQL with full move history
- JWT-based authentication for secure player identification

---

## ⚙️ System Architecture

```plaintext
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         GOMOKU HYBRID ARCHITECTURE                              │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐                  ┌──────────────────────────────────────────┐
│   React + Vite  │                  │         Spring Boot Application         │
│   Frontend UI   │                  │                                          │
│                 │     WebSocket    │  ┌─────────────────────────────────────┐ │
│ • Game Board    │◄────────────────►│  │      WebSocket Layer (PvP)         │ │
│ • Player Input  │   Player vs      │  │  • Real-time move broadcasting     │ │
│ • Live Updates  │   Player moves   │  │  • Player session management       │ │
│ • Match Lobby   │                  │  │  • Game state synchronization      │ │
│                 │                  │  └─────────────────────────────────────┘ │
│                 │     HTTP/REST    │                   │                       │
│                 │◄────────────────►│           ┌───────▼─────────┐             │
│                 │   Player vs AI   │           │  GAME SERVICES  │             │
└─────────────────┘   moves & state  │           │                 │             │
                                     │           │ • GameService   │             │
┌─────────────────┐                  │           │ • PlayerStatsSvc│             │
│   PostgreSQL    │◄─────────────────┤           └───────┬─────────┘             │
│   Database      │  Final Results   │                   │                       │
│                 │                  │           ┌───────▼─────────┐             │
│ • Player Stats  │                  │           │ KAFKA PRODUCERS │             │
│ • Game History  │                  │           │  (Event Logging)│             │
│ • Match Results │                  │           │ • Game moves    │             │
└─────────────────┘                  │           │ • Match events  │             │
                                     │           └─────────────────┘             │
┌─────────────────┐                  │                   │                       │
│   Redis Cache   │◄─────────────────┤                   │                       │
│                 │  Active Games &  │                   │                       │
│ • GameSessions  │  Matchmaking     └───────────────────┼───────────────────────┘
│ • int[][] board │                                      ▼
│ • Queue (FIFO)  │                  ┌─────────────────────────────────────────────┐
│ • TTL: 2 hours  │                  │              APACHE KAFKA CLUSTER           │
└─────────────────┘                  │ ┌─────────────────────────────────────────┐ │
         ▲                           │ │           TOPIC: game-move-made         │ │
         │                           │ │ Event log of ALL moves (player & AI)   │ │
         │                           │ │ Used for: game replay, analytics       │ │
         │                           │ └─────────────────────────────────────────┘ │
         │                           │ ┌─────────────────────────────────────────┐ │
         │                           │ │           TOPIC: match-created          │ │
         │                           │ │ Event log when matches are formed      │ │
         │                           │ │ Used for: match history, analytics     │ │
         │                           │ └─────────────────────────────────────────┘ │
         │                           └─────────────────────────────────────────────┘
         │                                               │
         │                                               ▼
         │                           ┌─────────────────────────────────────────────┐
         │                           │            KAFKA CONSUMER SERVICES          │
         │                           │                                             │
         │                           │ ┌─────────────────────────────────────────┐ │
         │                           │ │        GameMovesConsumer                │ │
         │                           │ │  • Game replay data persistence         │ │
         │                           │ │  • Move analytics (player & AI)         │ │
         │                           │ │  • Anti-cheat pattern detection        │ │
         │                           │ └─────────────────────────────────────────┘ │
         │                           │ ┌─────────────────────────────────────────┐ │
         │                           │ │        MatchCreatedConsumer             │ │
         │                           │ │  • Match history persistence            │ │
         │                           │ │  • Player statistics updates            │ │
         │                           │ │  • Match analytics                      │ │
         │                           │ └─────────────────────────────────────────┘ │
         │                           └─────────────────────────────────────────────┘
         │
         │
┌────────┴────────┐                  ┌─────────────────────────────────────────────┐
│  MATCHMAKING    │                  │          Python AI Microservice             │
│    SERVICE      │     HTTP/gRPC    │          (Same Repository)                  │
│                 │◄────────────────►│                                             │
│ • Redis Queue   │   AI move        │ • PyTorch model inference                   │
│   (ZADD/ZPOP)   │   requests       │ • Multiple difficulty levels                │
│ • FIFO pairing  │                  │ • Board evaluation engine                   │
│ • MMR-based     │                  │ • Minimax with neural net evaluation        │
│ • Room creation │                  │ • Dockerized alongside Spring Boot          │
└─────────────────┘                  └─────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              COMMUNICATION PATTERNS                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PLAYER vs PLAYER: WebSocket bidirectional real-time communication              │
│ PLAYER vs AI: Spring Boot → HTTP/gRPC → Python AI Service                      │
│ MATCHMAKING: Redis queue (ZADD/ZPOPMIN) for FIFO player pairing                │
│ ANALYTICS/LOGGING: Kafka event streams for all game/match events               │
└─────────────────────────────────────────────────────────────────────────────────┘
```

--- 

## 📁 Project Structure

```plaintext
gomoku-matching/
├── backend/                                # Spring Boot application
│   ├── src/main/java/com/gomokumatching/
│   │   ├── BackendApp.java
│   │   ├── controller/
│   │   │   ├── GameController.java
│   │   │   ├── MatchmakingController.java
│   │   │   ├── WebSocketController.java
│   │   │   ├── AuthController.java
│   │   │   └── ProfileController.java
│   │   ├── service/
│   │   │   ├── GameService.java                # Core game logic
│   │   │   ├── MatchmakingService.java         # Redis-based player pairing
│   │   │   ├── AIServiceClient.java            # Client for Python AI service
│   │   │   └── PlayerStatsService.java         # Statistics management
│   │   ├── kafka/
│   │   │   ├── producer/
│   │   │   │   ├── GameMoveProducer.java
│   │   │   │   └── MatchCreatedProducer.java
│   │   │   └── consumer/
│   │   │       ├── GameMovesConsumer.java
│   │   │       └── MatchCreatedConsumer.java
│   │   ├── model/
│   │   │   ├── Player.java                // PLAYER
│   │   │   ├── PlayerStats.java           // PLAYER_STATS
│   │   │   ├── AIOpponent.java            // AI_OPPONENT
│   │   │   ├── Game.java                  // GAME
│   │   │   ├── GameMove.java              // GAME_MOVE
│   │   │   ├── GameSession.java           // In-memory game session
│   │   │   ├── KafkaEventLog.java         // KAFKA_EVENT_LOG
│   │   │   ├── GameAnalytics.java         // GAME_ANALYTICS
│   │   │   ├── AIModelPerformance.java    // AI_MODEL_PERFORMANCE
│   │   │   └── PlayerAIMatchup.java       // PLAYER_AI_MATCHUP
│   │   ├── repository/
│   │   │   ├── GameRepository.java
│   │   │   ├── PlayerRepository.java
│   │   │   └── GameStatsRepository.java
│   │   ├── config/
│   │   │   ├── KafkaConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── WebSocketConfig.java
│   │   │   ├── FirebaseConfig.java
│   │   │   └── SecurityConfig.java
│   │   └── security/
│   │       └── FirebaseFilter.java
│   └── pom.xml
│
├── ai-service/                             # Python AI microservice (Django)
│   ├── gomoku_ai/
│   │   ├── settings/                       # Django settings (dev/prod)
│   │   ├── urls.py                         # URL routing
│   │   └── wsgi.py                         # WSGI application
│   ├── ai_engine/
│   │   ├── views.py                        # Django REST API endpoints
│   │   ├── ai_logic.py                     # PyTorch model inference
│   │   ├── minimax.py                      # Minimax algorithm
│   │   └── models/
│   │       ├── easy_model.pth
│   │       ├── medium_model.pth
│   │       └── hard_model.pth
│   ├── requirements.txt
│   ├── manage.py
│   └── Dockerfile
│
├── frontend/                               # React frontend
│   ├── src/
│   │   ├── components/
│   │   ├── services/
│   │   ├── pages/
│   │   ├── firebase.ts
│   │   └── App.tsx
│   ├── package.json
│   └── vite.config.js
│
├── docker-compose.yml                      # Multi-service orchestration
└── README.md
```

---

## 🔧 Core Technologies

| Component                | Technology                          | Purpose                                    |
|--------------------------|-------------------------------------|--------------------------------------------|
| **Backend**              | Java Spring Boot 3.5                | REST API and WebSocket server              |
| **Authentication**       | JWT (Spring Security)               | Token-based authentication                 |
| **Real-Time Updates**    | Spring WebSockets (STOMP)           | PvP game state broadcasting                |
| **Active Game Cache**    | Redis 7                             | In-memory sessions with 2-hour TTL         |
| **AI Opponent**          | Django + Python                     | Microservice for move calculation          |
| **AI Communication**     | HTTP REST                           | Spring Boot → Django                       |
| **Database**             | PostgreSQL 15                       | Player data, game history, statistics      |
| **Containerization**     | Docker Compose                      | Multi-service orchestration                |

---

## 🔐 Authentication

JWT-based authentication with Spring Security.

**Registration Flow:**
1. Client sends username, email, password to `/api/auth/register`
2. Backend hashes password with BCrypt (work factor 12)
3. User record created in PostgreSQL `player` table
4. PlayerStats record auto-created with default values

**Login Flow:**
1. Client sends credentials to `/api/auth/login`
2. Backend validates against BCrypt hash
3. JWT access token generated (1 hour expiry)
4. JWT refresh token generated (7 days expiry)
5. Tokens returned to client

**Protected Endpoints:**
- Client includes `Authorization: Bearer <token>` header
- Spring Security filter validates JWT signature and expiry
- User context extracted from token claims

---

## 🎮 Gameplay Workflow

### **1. Game Creation**

**PvP:**
1. Client POST `/api/matchmaking/create-pvp` with opponent username
2. Backend creates GameSession with unique UUID
3. Session stored in Redis with 2-hour TTL
4. WebSocket rooms created for both players

**PvAI:**
1. Client POST `/api/matchmaking/create-pvai` with AI opponent ID
2. Backend creates GameSession (player2Id = null, aiOpponentId set)
3. Session stored in Redis

### **2. Move Processing (PvP)**

1. Player clicks board position
2. WebSocket message sent to `/app/game/{gameId}/move`
3. GameService validates move:
   - Check player authorization
   - Validate move legality (position empty, correct turn)
   - Update int[][] board in GameSession
   - Check win condition (5 in a row)
4. Updated state broadcast via WebSocket to both players
5. GameSession updated in Redis

### **3. Move Processing (PvAI)**

1. Player POST `/api/game/{gameId}/move` with row/col
2. GameService processes player move (same validation)
3. If game still in progress, request AI move:
   - HTTP POST to Django service at `http://ai-service:8000/api/ai/move`
   - Django loads model based on difficulty
   - Returns optimal move coordinates
4. GameService processes AI move
5. Return updated state to client

### **4. Game Completion**

When win/draw/forfeit detected:
1. GameSession status set to COMPLETED/ABANDONED
2. `saveCompletedGameToDatabase()` called:
   - Convert GameSession → Game entity
   - Map enums and relationships
   - Serialize final board state as JSONB
   - Serialize move sequence as JSONB array `[[row, col, player], ...]`
   - Save to PostgreSQL
3. Redis session remains available for retrieval
4. TTL ensures cleanup after 2 hours

**Move History Format:**
```json
{
  "game_id": "0e086cf4-40ae-43f6-9fce-7ac9374ac100",
  "move_sequence": [
    [7, 7, 1],
    [8, 7, 2],
    [7, 8, 1],
    [8, 8, 2],
    [7, 9, 1]
  ]
}
```
Each move is `[row, col, player]` where player is `1` (Player 1) or `2` (Player 2/AI).

---

## 🤖 AI Service Architecture

Separate Django microservice handles AI move calculations:

- **Django REST Framework**: HTTP endpoint for move requests
- **Isolated service**: AI logic decoupled from game server
- **Future extensibility**: Ready for ML model integration
- **Simple communication**: HTTP REST requests from Spring Boot

### AI Service Structure

```python
# ai-service/ai_engine/views.py (Django REST API)
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import json
import torch

@csrf_exempt
def calculate_move(request):
    if request.method == 'POST':
        data = json.loads(request.body)
        board = data['board']        # 15x15 board state
        difficulty = data['difficulty']  # "easy", "medium", "hard"

        model = load_model(difficulty)
        best_move = ai_engine.evaluate(board, model)

        return JsonResponse({
            "row": best_move[0],
            "col": best_move[1]
        })
```

### Spring Boot Integration

```java
@Service
public class AIServiceClient {
    private final RestTemplate restTemplate;

    public Move getAIMove(int[][] board, String difficulty) {
        String url = "http://ai-service:8000/calculate-move";
        AIRequest request = new AIRequest(board, difficulty);
        return restTemplate.postForObject(url, request, Move.class);
    }
}
```

### Communication Options

| Protocol | Use Case | Pros | Cons |
|----------|----------|------|------|
| **HTTP/REST** | Simple request/response | Easy debugging, familiar | Slight overhead |
| **gRPC** | High-performance | Binary protocol, fast | More complex setup |

For this project, **HTTP/REST with Django** is used for simplicity and Python ML library compatibility.

---
## 🎯 Data Structure Decision: **2D Integer Array**

**Board Representation:**
```java
int[][] board = new int[15][15];

// Values:
// 0 = empty position
// 1 = player 1 stone
// 2 = player 2/AI stone
```

**Why 2D Array?**
- ✅ **Simple indexing**: `board[row][col]`
- ✅ **Fast win detection**: Easy to check directions (horizontal, vertical, diagonal)
- ✅ **AI-friendly**: Perfect for minimax algorithms and pattern matching
- ✅ **Memory efficient**: Only 225 integers (15x15)
- ✅ **Cache-friendly**: Contiguous memory access for performance

## 🏗️ Storage Architecture

### **2-Tier Storage Strategy**

```
┌─────────────────────────────────────────────────────────────────┐
│                    ACTIVE GAME LAYER (Redis)                   │
├─────────────────────────────────────────────────────────────────┤
│ • GameSession objects with int[][] board                        │
│ • Key: game:session:{UUID}                                     │
│ • TTL: 2 hours (auto-cleanup)                                  │
│ • Purpose: Fast move validation and real-time updates          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼ (On completion/forfeit)
┌─────────────────────────────────────────────────────────────────┐
│                 PERSISTENCE LAYER (PostgreSQL)                 │
├─────────────────────────────────────────────────────────────────┤
│ • game table: Final state, winner, duration                    │
│ • final_board_state: JSONB snapshot of final board            │
│ • move_sequence: JSONB array [[row,col,player],...]           │
│ • game_move table: Reserved for future Kafka integration      │
│ • player_stats: Wins, losses, MMR tracking                     │
└─────────────────────────────────────────────────────────────────┘
```

## 🎮 Data Flow

### **Player vs Player**
1. Move received via WebSocket
2. Validate against Redis GameSession
3. Update int[][] board
4. Broadcast to both players via WebSocket
5. On completion → Save to PostgreSQL

### **Player vs AI**
1. Player move via HTTP POST
2. Validate and update Redis
3. Request AI move from Django service
4. Apply AI move to Redis
5. Return updated state
6. On completion → Save to PostgreSQL

## 📊 GameSession Object Structure

```java
public class GameSession {
    private UUID gameId;
    private GameType gameType;        // HUMAN_VS_HUMAN, HUMAN_VS_AI
    private GameStatus status;        // IN_PROGRESS, COMPLETED, ABANDONED
    private UUID player1Id;
    private UUID player2Id;           // null for AI games
    private UUID aiOpponentId;        // null for PvP games
    private int[][] board = new int[15][15];
    private int currentPlayer;        // 1 or 2
    private int moveCount;
    private String winnerType;        // PLAYER1, PLAYER2, AI, DRAW, NONE
    private UUID winnerId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime lastActivity;

    public boolean isValidMove(int row, int col);
    public boolean checkIfBoardFull();
    public void makeMove(int row, int col, int player);
    public void switchPlayer();
}
```

## 🔄 Serialization

### **Redis Storage**
GameSession objects serialized to JSON using Jackson ObjectMapper:
- Stored with key pattern: `game:session:{UUID}`
- TTL set to 2 hours (7200 seconds)
- Automatic expiration prevents memory leaks

### **PostgreSQL Storage**
```java
// int[][] board → JSONB for database storage
String boardJson = objectMapper.writeValueAsString(session.getBoard());
game.setFinalBoardState(boardJson);
```

### **Database Schema**
```sql
CREATE TABLE game (
    game_id UUID PRIMARY KEY,
    game_type VARCHAR(20),
    game_status VARCHAR(20),
    player1_id UUID REFERENCES player(player_id),
    player2_id UUID REFERENCES player(player_id),
    ai_opponent_id UUID REFERENCES ai_opponent(ai_id),
    winner_type VARCHAR(20),
    winner_id UUID REFERENCES player(player_id),
    total_moves INTEGER,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    final_board_state JSONB,
    move_sequence JSONB,  -- [[row, col, player], ...]
    game_duration_seconds INTEGER
);

CREATE TABLE game_move (
    move_id UUID PRIMARY KEY,
    game_id UUID REFERENCES game(game_id),
    move_number INTEGER,
    player_type VARCHAR(20),
    player_id UUID REFERENCES player(player_id),
    ai_opponent_id UUID REFERENCES ai_opponent(ai_id),
    board_x INTEGER,
    board_y INTEGER,
    stone_color VARCHAR(20),
    move_timestamp TIMESTAMP WITH TIME ZONE
);
```

## 🚀 Performance

### **Why This Works**

1. **Fast Gameplay**
   - In-memory operations in Redis
   - No database writes during active play
   - WebSocket broadcasting for instant updates

2. **Scalable Storage**
   - Database writes only on completion
   - Redis TTL prevents memory leaks
   - Separate hot/cold storage paths

3. **Memory Efficient**
   - 15×15 int array = ~900 bytes per game
   - Redis handles thousands of concurrent games
   - Automatic cleanup after 2 hours

## 💡 Implementation Notes

### **Board Coordinate System**
```
    0  1  2  3  4 ... 14
0   ⋅  ⋅  ⋅  ⋅  ⋅     ⋅
1   ⋅  ⋅  ⋅  ⋅  ⋅     ⋅  
2   ⋅  ⋅  ●  ⋅  ⋅     ⋅  ← board[2][2] = 1
3   ⋅  ⋅  ⋅  ○  ⋅     ⋅  ← board[3][3] = 2
...
14  ⋅  ⋅  ⋅  ⋅  ⋅     ⋅

// Center position
int centerRow = 7, centerCol = 7;
board[centerRow][centerCol] = 1; // First move at center
```

### **Win Detection**
- Check only around last placed stone
- Four directions: horizontal, vertical, two diagonals
- Count consecutive stones in each direction
- Return true if count ≥ 5

Implementation in `GameService.checkWinCondition()` at line 214.

## 🐳 Docker Setup

Services orchestrated with Docker Compose:

```yaml
services:
  postgres:
    image: postgres:15-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: gomoku_db
      POSTGRES_USER: gomoku_user
      POSTGRES_PASSWORD: gomoku_password

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  backend:
    build: ./backend
    ports: ["8080:8080"]
    depends_on: [postgres, redis, ai-service]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gomoku_db
      SPRING_REDIS_HOST: redis
      AI_SERVICE_URL: http://ai-service:8000

  ai-service:
    build: ./ai-service
    ports: ["8001:8000"]
    environment:
      DJANGO_SETTINGS_MODULE: gomoku_ai.settings.development

  pgadmin:
    image: dpage/pgadmin4:latest
    ports: ["5050:80"]
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@gomoku.com
      PGADMIN_DEFAULT_PASSWORD: admin

  redis-commander:
    image: rediscommander/redis-commander:latest
    ports: ["8081:8081"]
```

### Running

```bash
# Start all services
docker-compose up -d

# Check logs
docker logs gomoku-backend
docker logs gomoku-ai-service

# Stop services
docker-compose down
```

---

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local development)
- Python 3.12 (for AI service development)

### Quick Start

```bash
# Clone repository
git clone <repository-url>
cd GomokuMatching

# Start all services
docker-compose up -d

# Wait for services to initialize (~30 seconds)
docker-compose logs -f backend

# Verify health
curl http://localhost:8080/actuator/health
```

### Access Application

- **Game UI**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **AI Service (Django)**: http://localhost:8001
- **Redis**: localhost:6379
- **PostgreSQL**: localhost:5432 (gomoku_user/gomoku_password)
- **pgAdmin**: http://localhost:5050 (admin@gomoku.com / admin)
- **Redis Commander**: http://localhost:8081
