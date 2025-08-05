🏗️ Technology Stack & Responsibilities
Apache Kafka - Central Event Bus

Primary role: All game logic flows through Kafka topics
Handles: Player moves, AI decisions, matchmaking, game state changes
Benefits: Event sourcing, scalability, fault tolerance, game replay capability
Topics: player-moves, ai-moves, game-state-updates, matchmaking-queue, game-completed

WebSockets (STOMP/SockJS) - Real-time Client Communication

Primary role: Bidirectional communication with React frontend
Handles: Receiving player input, broadcasting processed results from Kafka
Benefits: Low-latency UI updates, connection management
Usage: Player move input → Kafka, Kafka processed results → Player display

Spring Boot - Application Backend

Primary role: Kafka consumers/producers, WebSocket management, business logic
Handles: HTTP REST endpoints, WebSocket controllers, Kafka message processing
Benefits: Kafka integration, WebSocket support, dependency injection

PostgreSQL - Persistent Data Storage

Primary role: Long-term game data, player profiles, match history
Handles: Final game results, player statistics, MMR rankings
Benefits: ACID compliance, complex queries for leaderboards

React + Vite - Frontend Interface

Primary role: Game UI, board visualization, player interactions
Handles: WebSocket connections, game board rendering, user input
Benefits: Real-time updates, responsive UI, modern development experience

PyTorch + DJL - AI Opponent System

Primary role: Generate intelligent moves for AI opponents
Handles: Board state evaluation, move calculation, difficulty scaling
Benefits: Machine learning-powered opponents, scalable AI processing


🔄 Complete Game Flow Workflows
1. Player Matchmaking Workflow
1. Player clicks "Find Match" in React UI
2. React sends WebSocket message to Spring Boot
3. Spring Boot produces message to Kafka topic: `matchmaking-queue`
4. Matchmaking Consumer processes queue, finds suitable opponent
5. When match found: Producer sends to `match-created` topic
6. Game Service Consumer creates new game instance
7. WebSocket broadcasts "Match Found" to both players
8. Players redirected to game board
2. Player Move Workflow (Kafka-Primary)
1. Player clicks board position in React UI
2. React sends move via WebSocket to Spring Boot
3. Spring Boot immediately produces to Kafka topic: `player-moves`
4. Move Consumer validates and processes the move
5. Consumer updates game state and produces to: `game-state-updates`
6. Game State Consumer receives update
7. Consumer broadcasts new board state via WebSocket to both players
8. If game complete: produces to `game-completed` topic
3. AI Opponent Move Workflow
1. After player move processed, AI turn detected
2. Game Service produces AI request to: `ai-move-requests`
3. AI Service Consumer (with PyTorch/DJL) receives request
4. AI evaluates board state using ML model
5. AI produces calculated move to: `ai-moves`
6. AI Move Consumer processes and validates AI move
7. Consumer produces to: `game-state-updates` (same as player moves)
8. Game State Consumer broadcasts AI move via WebSocket
4. Game Completion Workflow
1. Win condition detected by Move Consumer
2. Final game state produced to: `game-state-updates`
3. Game completion event produced to: `game-completed`
4. Completion Consumer updates PostgreSQL with results
5. MMR calculations and player stats updated
6. WebSocket broadcasts final results and winner
7. Players can start new game or return to lobby

📊 Data Flow Architecture
React Frontend ↔ WebSocket Layer

Inbound: Player moves, chat messages, lobby actions
Outbound: Game state updates, match notifications, AI moves

WebSocket Layer → Kafka Producer

Transforms: UI events into Kafka messages
Topics: player-moves, matchmaking-queue, chat-messages

Kafka Topics → Spring Boot Consumers

Processing: Game logic, validation, AI triggering
Services: GameService, MatchmakingService, AIService

Spring Boot Services → Kafka Producers

Generates: State updates, AI requests, completion events
Topics: game-state-updates, ai-move-requests, game-completed

Kafka Consumers → WebSocket Broadcasting

Distribution: Processed results back to connected players
Real-time: Immediate UI updates after backend processing


🎯 Key Architectural Benefits
Event Sourcing

Every game move stored in Kafka topics
Complete game replay capability
Audit trail for anti-cheat systems

Scalability

Multiple consumer instances can process moves
AI service scales independently
Database writes decoupled from real-time gameplay

Fault Tolerance

Kafka message persistence survives server restarts
Failed moves can be replayed
No lost game state during deployments

Development Benefits

Clear separation of concerns
Easy to add new features (spectator mode, tournaments)
Testable event-driven architecture


🔧 Local Development Setup
Kafka Ecosystem

Single Kafka broker on localhost:9092
Topics created automatically or via CLI
Kafka UI for message monitoring (optional)

Spring Boot Application

Embedded Kafka consumers/producers
WebSocket endpoints for React connection
REST APIs for game management

React Development Server

Vite dev server with WebSocket connection
Real-time board updates
Player input handling

PostgreSQL Database

Local instance or Docker container
Game history and player data
Analytics queries for testing


📈 Monitoring & Debugging
Kafka Monitoring

Topic message counts and lag
Consumer group processing rates
Failed message handling

WebSocket Monitoring

Active player connections
Message throughput
Connection stability

Game State Tracking

Move validation success rates
AI response times
Game completion statistics
