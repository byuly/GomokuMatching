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
[ Players (Clients) ] 
       │
       ▼
[ Matchmaker Backend (Kafka, Spring Boot) ]  
       │
       ├─> Matches players or pairs player with AI
       ├─> Manages game state and turn order
       ├─> Sends moves and board updates to clients
       └─> Receives moves from players or AI agent
               │
               ▼
[ AI Opponent Service (ML Model) ]
       │
       └─> Evaluates board, generates AI moves
               │
               ▼
[ Live Dashboard (React + WebSockets) ]

```

---

## 🔧 Core Technologies
| Component           | Technology                                   |
| ------------------- | -------------------------------------------- |
| Backend API         | Java Spring Boot                             |
| MMR-based Match Making Queue | Apache Kafka                                 |
| Real-Time Updates   | Spring WebSockets (STOMP/SockJS)             |
| Database            | PostgreSQL                                   |
| Frontend UI         | Vite + React + TailwindCSS                   |
| AI Opponent         | Machine Learning Models (PyTorch) |
| Infrastructure      | Docker Compose      |

---

🎮 Gameplay Flow
Player Queuing: Players join matchmaking queues via REST API & Apache Kafka.

Match Creation: Matchmaker pairs two players or assigns an AI opponent.

Game Start: Board state initialized and broadcast to clients.

Turns: Players (or AI) alternately place stones on the board.

Win Detection: Backend checks for five in a row to determine winner.

Score Broadcast: Current game state and final results pushed live via WebSocket to clients and dashboard.

Repeat: Players can start new matches or adjust AI difficulty levels.


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
Use DJL’s Java APIs to load pre-trained PyTorch models or export PyTorch models from Python and run inference inside your Spring Boot service.

This approach avoids managing a separate Python service and simplifies deployment.

Ideal for the AI Opponent service in Gomoku to evaluate board states and generate moves using ML models.

