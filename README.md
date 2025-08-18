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
│                         GOMOKU HYBRID KAFKA ARCHITECTURE                        │
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
┌─────────────────┐                  │           │ • AIService     │             │
│   PostgreSQL    │◄─────────────────┤           │ • PlayerStatsSvc│             │
│   Database      │  Final Results   │           └───────┬─────────┘             │
│                 │                  │                   │                       │
│ • Player Stats  │                  │           ┌───────▼─────────┐             │
│ • Game History  │                  │           │ KAFKA PRODUCERS │             │
│ • MMR Rankings  │                  │           │ (Shadow Paths)  │             │
│ • Match Results │                  │           │ • GameEvents    │             │
└─────────────────┘                  │           │ • Analytics     │             │
                    ┌────────────────►│           │ • Move Logging  │             │
                    │                 │           │ • AI Analytics  │             │
                    │                 │           └─────────────────┘             │
                    │                 └──────────────────┬────────────────────────┘
                    │                                    ▼
                    │                ┌─────────────────────────────────────────────┐
                    │                │              APACHE KAFKA CLUSTER           │
                    │                │ ┌─────────────────────────────────────────┐ │
                    │                │ │             TOPIC: game-events          │ │
                    │                │ │ Shadow logging of all game moves       │ │
                    │                │ └─────────────────────────────────────────┘ │
                    │                │ ┌─────────────────────────────────────────┐ │
                    │                │ │           TOPIC: match-events           │ │
                    │                │ │ Queue management, player pairing       │ │
                    │                │ └─────────────────────────────────────────┘ │
                    │                │ ┌─────────────────────────────────────────┐ │
                    │                │ │             TOPIC: ai-analytics         │ │
                    │                │ │ AI performance metrics and decisions    │ │
                    │                │ └─────────────────────────────────────────┘ │
                    │                │ ┌─────────────────────────────────────────┐ │
                    │                │ │             TOPIC: analytics-events     │ │
                    │                │ │ Game statistics, performance metrics    │ │
                    │                │ └─────────────────────────────────────────┘ │
                    │                └─────────────────────────────────────────────┘
                    │                                    │
                    │                                    ▼
┌───────────────────┴──┐               ┌─────────────────────────────────────────────┐
│  MATCHMAKING SERVICE │               │            KAFKA CONSUMER SERVICES          │
│   (Kafka Consumer)   │               │                                             │
│                      │               │ ┌─────────────────────────────────────────┐ │
│ • Queue Processing   │               │ │        GameEventsConsumer               │ │
│ • Player Matching    │               │ │  • Real-time move analytics             │ │
│ • Room Assignment    │               │ │  • Game replay data collection          │ │
│ • MMR-based Pairing  │◄──────────────┤ │  • Anti-cheat pattern detection        │ │
│                      │  Async Match  │ └─────────────────────────────────────────┘ │
└──────────────────────┘  Creation     │ ┌─────────────────────────────────────────┐ │
                                       │ │        MatchEventsConsumer              │ │
                                       │ │  • Async player queue management        │ │
                                       │ │  • Room creation and lifecycle          │ │
                                       │ │  • Player connection routing            │ │
                                       │ └─────────────────────────────────────────┘ │
                                       │ ┌─────────────────────────────────────────┐ │
                                       │ │        AIAnalyticsConsumer              │ │
                                       │ │  • AI decision pattern analysis        │ │
                                       │ │  • Model performance tracking          │ │
                                       │ │  • AI training data collection         │ │
                                       │ └─────────────────────────────────────────┘ │
                                       │ ┌─────────────────────────────────────────┐ │
                                       │ │        AnalyticsEventsConsumer          │ │
                                       │ │  • Player statistics aggregation       │ │
                                       │ │  • Leaderboard updates                  │ │
                                       │ │  • Match outcome analysis               │ │
                                       │ └─────────────────────────────────────────┘ │
                                       └─────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              COMMUNICATION PATTERNS                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│ PLAYER vs PLAYER: WebSocket bidirectional real-time communication              │
│ PLAYER vs AI: HTTP request/response for moves + game state                     │
│ MATCHMAKING: Fully Kafka-driven async processing                               │
│ ANALYTICS/LOGGING: Kafka shadow paths for all game events                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

--- 

## 📁 Project Structure

```plaintext
gomoku-backend/
├── src/main/java/com/gomoku/
│   ├── GomokuApplication.java
│   ├── controller/
│   │   ├── GameController.java
│   │   ├── MatchmakingController.java
│   │   └── WebSocketController.java
│   ├── service/
│   │   ├── GameService.java                # Core game logic
│   │   ├── MatchmakingService.java         # Player pairing
│   │   ├── AIService.java                  # AI move calculation (internal)
│   │   └── PlayerStatsService.java         # Statistics management
│   ├── kafka/
│   │   ├── producer/
│   │   │   ├── GameEventsProducer.java
│   │   │   ├── MatchEventsProducer.java
│   │   │   ├── AIAnalyticsProducer.java
│   │   │   └── AnalyticsEventsProducer.java
│   │   └── consumer/
│   │       ├── GameEventsConsumer.java
│   │       ├── MatchEventsConsumer.java
│   │       ├── AIAnalyticsConsumer.java
│   │       └── AnalyticsEventsConsumer.java
│   ├── model/
│   │   ├── Player.java                // PLAYER
│   │   ├── PlayerStats.java           // PLAYER_STATS
│   │   ├── AIOpponent.java            // AI_OPPONENT
│   │   ├── Game.java                  // GAME
│   │   ├── GameMove.java              // GAME_MOVE
│   │   ├── MatchmakingQueue.java      // MATCHMAKING_QUEUE
│   │   ├── GameSession.java           // GAME_SESSION
│   │   ├── KafkaEventLog.java         // KAFKA_EVENT_LOG
│   │   ├── GameAnalytics.java         // GAME_ANALYTICS
│   │   ├── Leaderboard.java           // LEADERBOARD
│   │   ├── AIModelPerformance.java    // AI_MODEL_PERFORMANCE
│   │   └── PlayerAIMatchup.java       // PLAYER_AI_MATCHUP
│   ├── repository/
│   │   ├── GameRepository.java
│   │   ├── PlayerRepository.java
│   │   └── GameStatsRepository.java
│   └── config/
│       ├── KafkaConfig.java
│       └── WebSocketConfig.java
├── frontend/                               # React frontend
│   ├── src/
│   │   ├── components/
│   │   ├── services/
│   │   └── pages/
│   ├── package.json
│   └── vite.config.js
├── docker-compose.yml                      # Local development setup
├── pom.xml
└── README.md
```

---

## 🔧 Core Technologies

| Component                | Technology                          | Purpose                                    |
|--------------------------|-------------------------------------|--------------------------------------------|
| **Backend**              | Java Spring Boot                    | Single application with all services      |
| **Authentication**       | Google Firebase                     | User sign-up, login, and token validation |
| **Real-Time Updates**    | Spring WebSockets (STOMP/SockJS)    | Immediate game state broadcasting          |
| **Event Processing**     | Apache Kafka                        | Async processing, analytics, event sourcing |
| **AI Opponent**          | PyTorch (DJL)                       | Machine learning move calculation          |
| **Database**             | PostgreSQL                          | Player data, game history, statistics     |
| **Frontend**             | Vite + React + TypeScript           | Game UI, board visualization               |
| **Styling**              | TailwindCSS                         | Modern responsive design                   |
| **Containerization**     | Docker Compose                      | Local development environment              |

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

### **1. Player Matchmaking Flow (Direct + Kafka)**

1. Player clicks "Find Match" in React UI
2. Frontend sends WebSocket message to MatchmakingController
3. MatchmakingService processes request immediately:
   - Checks waiting queue for suitable opponent
   - If found: creates match directly, updates UI via WebSocket
   - If not found: adds to waiting queue
4. Simultaneously: produces MatchEvent to `match-events` topic for analytics
5. Players get immediate match confirmation or queue status

### **2. Player Move Flow (WebSocket Primary + Kafka Shadow)**

1. Player clicks board position in React UI
2. Frontend sends move via WebSocket to GameController
3. GameService processes move immediately:
   - Validates move and updates game state
   - Checks win conditions
   - Broadcasts updated board via WebSocket to both players
4. Asynchronously: produces GameEvent to `game-events` topic:
   - Event logging for replay capability
   - Anti-cheat validation
   - Game history persistence

### **3. AI Opponent Flow (Internal Processing + Kafka Analytics)**

1. After player move, GameService detects AI turn
2. AIService calculates move internally using PyTorch/DJL:
   - Loads appropriate difficulty model
   - Evaluates board state
   - Generates optimal move
3. AI move applied immediately, broadcast via WebSocket/HTTP response
4. Simultaneously: produces AIAnalytics to `ai-analytics` topic:
   - ML training data collection
   - AI performance analytics
   - Model improvement insights

### **4. Game Completion Flow (Immediate + Background)**

1. Win condition detected by GameService
2. Immediate actions via WebSocket/HTTP:
   - Broadcast final results to players
   - Update basic player statistics
   - Display winner and game summary
3. Background processing via Kafka:
   - Detailed MMR calculations
   - Complex statistics aggregation
   - Leaderboard updates
   - Match history persistence

---

## 🤖 Integrating PyTorch with Spring Boot for AI Opponent

While PyTorch primarily targets Python, you can integrate PyTorch-powered AI models into Java applications, including Spring Boot, using the **Deep Java Library (DJL)**. DJL acts as a bridge supporting multiple deep learning engines, including PyTorch.

### Adding Dependencies

Add the following DJL and PyTorch dependencies to your Spring Boot project to enable PyTorch model inference:

**For Maven (`pom.xml`):**

```xml
<dependency>
    <groupId>ai.djl</groupId>
    <artifactId>api</artifactId>
    <version>0.14.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-engine</artifactId>
    <version>0.14.0</version>
</dependency>
<dependency>
    <groupId>ai.djl.pytorch</groupId>
    <artifactId>pytorch-native-auto</artifactId>
    <version>0.14.0</version>
</dependency>
```

Use DJL's Java APIs to load pre-trained PyTorch models or export PyTorch models from Python and run inference inside your Spring Boot service.

This approach avoids managing a separate Python service and simplifies deployment.

Ideal for the AI Opponent service in Gomoku to evaluate board states and generate moves using ML models.

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

## 🐳 Docker Development Setup

Local development environment using Docker Compose with PostgreSQL 17, pgAdmin 4, and optional Kafka. Run `docker-compose up -d postgres pgadmin` for basic setup or `docker-compose --profile kafka up -d` for full stack. Access pgAdmin at http://localhost:5050 (admin@gomoku.dev/admin123) with auto-configured PostgreSQL connection (gomoku_user/gomoku_password). Data persists in Docker volumes.

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

### Local Development Setup

1. **Start infrastructure services:**
```bash
docker-compose up -d kafka postgres
```

2. **Start Spring Boot Application:**
```bash
./mvnw spring-boot:run
```

3. **Start Frontend:**
```bash
cd frontend
npm install && npm run dev
```

### Access Application

- Game UI: http://localhost:5173
- Backend API: http://localhost:8080
- Kafka UI (optional): http://localhost:8081

### Kafka Topics Creation

```bash
# Create required topics
kafka-topics --create --topic game-events --bootstrap-server localhost:9092
kafka-topics --create --topic match-events --bootstrap-server localhost:9092
kafka-topics --create --topic ai-analytics --bootstrap-server localhost:9092
kafka-topics --create --topic analytics-events --bootstrap-server localhost:9092
```
