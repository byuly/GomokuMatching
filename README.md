# 🎮 Gomoku Matching – Classic Strategy Board Game with AI Opponents

## 🧠 Overview

**Gomoku 1v1** is a competitive, real-time strategy board game where two players face off to place five stones in a row on a grid. The game supports both human-vs-human matches and human-vs-AI matches, featuring machine learning-powered AI opponents with varying skill levels.

Built on a scalable matchmaking backend with Kafka, Spring Boot, and WebSockets, Gomoku 1v1 delivers a smooth, engaging experience blending classic gameplay with modern AI challenge.

---

## 🎯 Game Concept

- Two players compete on a 15x15 grid to be the first to align five stones horizontally, vertically, or diagonally.
- Matches can be player-vs-player or player-vs-machine learning AI.
- AI opponents use trained ML models to evaluate board states and make strategic moves.
- Real-time matchmaking pairs players or assigns AI opponents based on difficulty.
- Live score updates and game state streamed to clients for instant feedback.
- Players can track wins, losses, and ranking through a live dashboard.

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
├── ai-service/                             # Python AI microservice
│   ├── app/
│   │   ├── main.py                         # FastAPI or gRPC server
│   │   ├── ai_engine.py                    # PyTorch model inference
│   │   ├── minimax.py                      # Minimax algorithm
│   │   └── models/
│   │       ├── easy_model.pth
│   │       ├── medium_model.pth
│   │       └── hard_model.pth
│   ├── requirements.txt
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
| **Backend**              | Java Spring Boot                    | Main application server                    |
| **Authentication**       | Google Firebase                     | User sign-up, login, and token validation |
| **Real-Time Updates**    | Spring WebSockets (STOMP/SockJS)    | Immediate game state broadcasting          |
| **Event Processing**     | Apache Kafka                        | Event logging, analytics, event sourcing   |
| **Matchmaking**          | Redis (Queue)                       | FIFO player pairing with ZADD/ZPOPMIN      |
| **Active Game Cache**    | Redis (Cache)                       | In-memory game sessions with TTL           |
| **AI Opponent**          | Python + PyTorch                    | Microservice for ML-based move calculation |
| **AI Communication**     | HTTP/gRPC                           | Spring Boot ↔ Python AI service            |
| **Database**             | PostgreSQL                          | Player data, game history, statistics     |
| **Frontend**             | Vite + React + TypeScript           | Game UI, board visualization               |
| **Styling**              | TailwindCSS                         | Modern responsive design                   |
| **Containerization**     | Docker Compose                      | Multi-service orchestration                |

---

## 🔐 Authentication

Authentication is handled by **Firebase Authentication**, providing a secure and scalable solution.

-   **Provider**: Google Firebase (supports email/password, Google Sign-In, and other social logins).
-   **Flow**:
    1.  The React frontend uses the Firebase SDK to handle all user sign-up and login flows.
    2.  Upon successful login, the client receives a **Firebase ID Token (JWT)**.
    3.  This token is sent to the Spring Boot backend with every authenticated API request.
-   **Backend**: The backend uses the Firebase Admin SDK to verify the ID token. It does not handle or store any user passwords.
-   **User Profiles**: The `PLAYER` table in the PostgreSQL database stores user profile information, linked to Firebase via the user's unique Firebase ID (UID).

---

## 🎮 Hybrid Gameplay Workflow

### **1. Player Matchmaking Flow (Redis Queue + Kafka Logging)**

1. Player clicks "Find Match" in React UI
2. Frontend sends request to MatchmakingController
3. MatchmakingService processes via Redis:
   - **ZADD** matchmaking:queue {timestamp} {playerId} → Add to FIFO queue
   - **ZPOPMIN** matchmaking:queue 2 → Get two oldest waiting players
   - If pair found: creates match, removes from queue
   - If not found: player remains in queue
4. Match created → Produces event to `match-created` Kafka topic for analytics
5. Players get WebSocket notification of match confirmation

### **2. Player Move Flow (WebSocket Primary + Kafka Logging)**

1. Player clicks board position in React UI
2. Frontend sends move via WebSocket to GameController
3. GameService processes move immediately:
   - Validates move against Redis cached game session
   - Updates int[][] board in Redis
   - Checks win conditions
   - Broadcasts updated board via WebSocket to both players
4. Asynchronously: produces event to `game-move-made` Kafka topic:
   - Event logging for replay capability
   - Move analytics
   - Game history persistence

### **3. AI Opponent Flow (Python Microservice + Same Logging)**

1. After player move, GameService detects AI turn
2. Spring Boot sends HTTP/gRPC request to Python AI service:
   - Passes current board state (int[][])
   - Specifies difficulty level
3. Python AI service calculates move:
   - Loads appropriate PyTorch model (easy/medium/hard)
   - Runs minimax with neural net evaluation
   - Returns optimal move coordinates
4. GameService applies AI move (treated as regular player move):
   - Updates Redis cache
   - Broadcasts to player
   - **Logs to same `game-move-made` topic** (AI moves logged like player moves)

### **4. Game Completion Flow (Immediate + Background)**

1. Win condition detected by GameService
2. Immediate actions via WebSocket/HTTP:
   - Broadcast final results to players
   - Update Redis game session status to COMPLETED
   - Display winner and game summary
3. Background processing via Kafka:
   - Persist final game state to PostgreSQL
   - Update player statistics (wins/losses/MMR)
   - Match history persistence with all moves from replay log

---

## 🤖 Python AI Microservice Architecture

The AI opponent is implemented as a **separate Python microservice** running alongside the Spring Boot backend. This architecture provides:

- **Native PyTorch support**: Full Python ecosystem without JVM limitations
- **Model flexibility**: Easy to train, update, and deploy new models
- **Performance**: Optimized inference without JNI overhead
- **Separation of concerns**: AI logic isolated from game server

### AI Service Structure

```python
# ai-service/app/main.py (FastAPI example)
from fastapi import FastAPI
from pydantic import BaseModel
import torch

app = FastAPI()

class MoveRequest(BaseModel):
    board: list[list[int]]  # 15x15 board state
    difficulty: str          # "easy", "medium", "hard"

@app.post("/calculate-move")
async def calculate_move(request: MoveRequest):
    model = load_model(request.difficulty)
    best_move = ai_engine.evaluate(request.board, model)
    return {"row": best_move[0], "col": best_move[1]}
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

For this project, **HTTP/REST with FastAPI** is recommended for simplicity.

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

### **3-Tier Storage Strategy**

```
┌─────────────────────────────────────────────────────────────────┐
│                    REAL-TIME GAMEPLAY LAYER                    │
├─────────────────────────────────────────────────────────────────┤
│ Redis/In-Memory Cache                                           │
│ • Active games: GameSession objects                             │
│ • Board: int[][] (2D array)                                    │
│ • TTL: 2 hours (auto-expire)                                   │
│ • Purpose: Sub-100ms move validation & broadcasting             │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     EVENT STREAMING LAYER                      │
├─────────────────────────────────────────────────────────────────┤
│ Apache Kafka (Shadow Logging)                                  │
│ • Every move → game-events topic                               │
│ • Board serialized as JSON: "[[0,1,0,...],[2,0,1,...]]"      │
│ • Purpose: Analytics, replay, anti-cheat, audit trail          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼ (On Game Completion)
┌─────────────────────────────────────────────────────────────────┐
│                    PERSISTENCE LAYER                           │
├─────────────────────────────────────────────────────────────────┤
│ PostgreSQL Database                                             │
│ • Final board state as JSON TEXT column                        │
│ • Individual moves in game_moves table                         │
│ • Purpose: Player stats, history, leaderboards                 │
└─────────────────────────────────────────────────────────────────┘
```

## 🎮 Data Flow by Game Type

### **Player vs Player (WebSocket)**
1. **Move Request** → Validate against `Redis int[][]` board
2. **Update Cache** → Modify 2D array, broadcast via WebSocket
3. **Shadow Log** → Serialize to JSON, send to Kafka (non-blocking)
4. **Game End** → Persist final state to PostgreSQL

### **Player vs AI (HTTP)**
1. **Player Move** → Update `Redis int[][]` board
2. **AI Calculation** → Read 2D array, calculate optimal move
3. **AI Move** → Update 2D array, return new board in HTTP response
4. **Shadow Log** → Both moves logged to Kafka
5. **Game End** → Persist to PostgreSQL

## 📊 GameSession Object Structure

```java
// In-Memory Cache Object
public class GameSession {
    private Long gameId;
    private Long player1Id;
    private Long player2Id; // null for AI games
    private int[][] board = new int[15][15];
    private GameStatus status;
    private int currentPlayer; // 1 or 2
    private int moveCount;
    private LocalDateTime lastActivity;
    
    // Fast game logic methods
    public boolean isValidMove(int row, int col);
    public boolean checkWinCondition(int row, int col, int player);
    public void makeMove(int row, int col, int player);
}
```

## 🔄 Serialization Strategy

### **Cache ↔ JSON Conversion**
```java
// For Kafka logging and HTTP responses
ObjectMapper mapper = new ObjectMapper();

// Serialize: int[][] → JSON string
String boardJson = mapper.writeValueAsString(board);
// Result: "[[0,1,0,0...],[0,2,1,0...],...]"

// Deserialize: JSON → int[][]  
int[][] board = mapper.readValue(boardJson, int[][].class);
```

### **Database Storage**
```sql
-- games table
CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    player1_id BIGINT,
    player2_id BIGINT, -- NULL for AI games
    final_board_state TEXT, -- JSON string
    winner_id BIGINT,
    status VARCHAR(20),
    created_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Individual moves for replay/analytics
CREATE TABLE game_moves (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT REFERENCES games(id),
    player_id BIGINT, -- NULL for AI moves
    row INTEGER,
    col INTEGER,
    move_number INTEGER,
    is_ai_move BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP
);
```

## 🚀 Performance Benefits

### **Why This Architecture Works**

1. **Ultra-Fast Gameplay**
   - In-memory 2D array operations: ~1-5ms
   - No database I/O during active gameplay
   - Immediate WebSocket broadcasting

2. **Complete Audit Trail**
   - Every move logged to Kafka in real-time
   - Can replay entire games from event stream
   - Analytics and anti-cheat processing

3. **Scalable Persistence**
   - Database writes only on game completion
   - Bulk move insertion from Kafka consumers
   - Separate read/write workloads

4. **Memory Efficiency**
   - 15×15 int array = ~900 bytes per game
   - Redis can handle 10K+ concurrent games easily
   - Auto-expiry prevents memory leaks

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

### **Win Detection Optimization**
- Only check win condition around the last placed stone
- Check 4 directions: horizontal, vertical, 2 diagonals
- Short-circuit on first 5-in-a-row found

This strategy gives you **real-time performance** for gameplay while maintaining **complete data integrity** and **comprehensive analytics** through the Kafka event stream.

## 🐳 Docker Multi-Service Setup

The application uses Docker Compose to orchestrate multiple services:

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:17
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: gomoku
      POSTGRES_USER: gomoku_user
      POSTGRES_PASSWORD: gomoku_password

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  kafka:
    image: confluentinc/cp-kafka:latest
    ports: ["9092:9092"]
    depends_on: [zookeeper]

  zookeeper:
    image: confluentinc/cp-zookeeper:latest

  backend:
    build: ./backend
    ports: ["8080:8080"]
    depends_on: [postgres, redis, kafka, ai-service]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gomoku
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      AI_SERVICE_URL: http://ai-service:8000

  ai-service:
    build: ./ai-service
    ports: ["8000:8000"]
    volumes:
      - ./ai-service/app/models:/app/models

  frontend:
    build: ./frontend
    ports: ["5173:5173"]
    depends_on: [backend]
```

### Running the Stack

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend ai-service

# Stop all services
docker-compose down
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

### Local Development Setup

1. **Start all services via Docker Compose:**
```bash
docker-compose up -d
```

2. **Verify services are running:**
```bash
docker-compose ps
```

3. **Create Kafka topics:**
```bash
docker-compose exec kafka kafka-topics --create --topic game-move-made --bootstrap-server localhost:9092
docker-compose exec kafka kafka-topics --create --topic match-created --bootstrap-server localhost:9092
```

### Access Application

- **Game UI**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **AI Service**: http://localhost:8000
- **Redis**: localhost:6379
- **PostgreSQL**: localhost:5432 (gomoku_user/gomoku_password)
