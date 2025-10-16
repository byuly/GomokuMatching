# Gomoku Matched !
## Classic Strategy Board Game with AI Opponents


**Gomoku Matched** is a competitive, real-time strategy board game where two players face off to place five stones in a row on a grid. The game supports both human-vs-human matches and human-vs-AI matches, featuring machine learning-powered AI opponents with varying skill levels.

Built with Spring Boot, Redis, PostgreSQL, and a Flask-based AI service, the application delivers real-time gameplay with WebSocket support and persistent game history.

## architecture!

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
         │                           │ │  • Real-time move analytics (logs only) │ │
         │                           │ │  • Pattern tracking (player & AI)       │ │
         │                           │ │  • Future: Live dashboards & metrics    │ │
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
│    SERVICE      │     HTTP/REST    │          (Same Repository)                  │
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
│ PLAYER vs AI: Spring Boot → HTTP  → Python AI Service                          │
│ MATCHMAKING: Redis queue (ZADD/ZPOPMIN) for FIFO player pairing                │
│ ANALYTICS/LOGGING: Kafka event streams for all game/match events               │
└─────────────────────────────────────────────────────────────────────────────────┘
```
---

## the tech stack

| Component                | Technology                | Purpose                                    |
|--------------------------|---------------------------|--------------------------------------------|
| **Backend**              | Java Spring Boot          | REST API and WebSocket server              |
| **Authentication**       | JWT with Spring Security) | Token-based authentication                 |
| **Real-Time Updates**    | Spring WebSockets (STOMP) | PvP game state broadcasting                |
| **Active Game Cache**    | Redis                     | In-memory sessions with 2-hour TTL         |
| **AI Opponent**          | Flask + Python            | Microservice for move calculation          |
| **AI Communication**     | HTTP REST                 | Spring Boot → Flask                        |
| **Database**             | PostgreSQL 15             | Player data, game history, statistics      |
| **Containerization**     | Docker Compose            | Multi-service orchestration                |

---

## gameplay! 

### **1. Game Creation**

**PvP:**
1. Client POST `/api/game/create` with `gameType: HUMAN_VS_HUMAN` and `player2Id`
2. Backend creates GameSession with unique UUID
3. Session stored in Redis with 2-hour TTL
4. WebSocket rooms created for both players

**PvAI:**
1. Client POST `/api/game/create` with `gameType: HUMAN_VS_AI` and `aiDifficulty` (e.g., "MEDIUM")
2. Backend creates GameSession (player2Id = null, aiDifficulty set)
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
   - HTTP POST to Flask service at `http://ai-service:8000/api/ai/move`
   - Flask loads model based on difficulty
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
3. `saveMoveHistoryToDatabase()` called:
   - Reads moves from Redis `session.moveHistory`
   - Persists each move to `game_move` table
   - Includes player type, stone color, position, and AI difficulty (if applicable)
4. Redis session remains available for retrieval
5. TTL ensures cleanup after 2 hours

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

## ai service!

Separate python microservice handles AI move calculations:

- **Flask**: HTTP endpoint for move requests
- **Isolated service**: AI logic decoupled from game server
- **Future extensibility**: Ready for ML model integration
- **Simple communication**: HTTP REST requests from Spring Boot
---

## storage architecture

### **2-tier storage**

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
│ • game table: Final state, winner, duration, ai_difficulty     │
│ • final_board_state: JSONB snapshot of final board            │
│ • move_sequence: JSONB array [[row,col,player],...]           │
│ • game_move table: Move-by-move history (via saveToDatabase)  │
│ • player_stats: Wins, losses, MMR tracking                     │
└─────────────────────────────────────────────────────────────────┘
```

### **redis data structure example**

```
┌──────────────────────────────────────────────────────┐
│           REDIS (Single Instance)                    │
│           Port: 6379                                 │
├──────────────────────────────────────────────────────┤
│                                                      │
│  STRING (Key-Value)                                  │
│  ├─ game:session:uuid-1 → GameSession object        │
│  ├─ game:session:uuid-2 → GameSession object        │
│  └─ game:session:uuid-3 → GameSession object        │
│                                                      │
│  SORTED SET (Matchmaking Queue)                      │
│  └─ matchmaking:queue                                │
│      ├─ Score: 1697720400000, Member: player-a      │
│      ├─ Score: 1697720405000, Member: player-b      │
│      └─ Score: 1697720410000, Member: player-c      │
│                                                      │
│  (Future) STRING (Token Blacklist)                   │
│  ├─ blacklist:token:abc123 → "revoked"             │
│  └─ blacklist:token:def456 → "revoked"             │
│                                                      │
└──────────────────────────────────────────────────────┘
```

- single Redis instance handles both game sessions and matchmaking queue using different data structures.

## data flow overview~

### **Player vs Player**
1. Move received via WebSocket
2. Validate against Redis GameSession
3. Update int[][] board
4. Broadcast to both players via WebSocket
5. On completion → Save to PostgreSQL

### **Player vs AI**
1. Player move via HTTP POST
2. Validate and update Redis
3. Request AI move from python service
4. Apply AI move to Redis
5. Return updated state
6. On completion → Save to PostgreSQL

## game session object

```java
public class GameSession {
    private UUID gameId;
    private GameType gameType;        // HUMAN_VS_HUMAN, HUMAN_VS_AI
    private GameStatus status;        // IN_PROGRESS, COMPLETED, ABANDONED
    private UUID player1Id;
    private UUID player2Id;           // null for AI games
    private String aiDifficulty;      // "EASY", "MEDIUM", "HARD", "EXPERT" (null for PvP)
    private int[][] board = new int[15][15];
    private int currentPlayer;        // 1 or 2
    private int moveCount;
    private List<int[]> moveHistory;  // [[row, col, player], ...] for replay
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

### **board coordinate system:**
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

### **detecting the winner**
- Check only around last placed stone
- Four directions: horizontal, vertical, two diagonals
- Count consecutive stones in each direction
- Return true if count ≥ 5
---

## getting started!!!

### quick start

```bash
# Clone repository
git clone https://github.com/byuly/GomokuMatching
cd GomokuMatching

# Start all services
docker-compose up -d

# Wait for services to initialize (~30 seconds)
docker-compose logs -f backend

# Verify health
curl http://localhost:8080/actuator/health
```
