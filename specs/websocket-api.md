# WebSocket API

WebSocket endpoint for real-time Player vs Player gameplay.

**Connection URL**: `ws://localhost:8080/ws`

**Protocol**: STOMP over SockJS

---

## Connection Flow

```
1. Client connects to /ws with JWT token in header
2. Server validates JWT via WebSocketAuthInterceptor
3. Client subscribes to topics
4. Client sends messages to /app/* endpoints
5. Server broadcasts to /topic/* endpoints
```

---

## Authentication

**STOMP CONNECT Frame Header:**
```
Authorization: Bearer <access_token>
```

**Important**: JWT token must be included in the CONNECT frame, not in individual messages.

---

## Endpoints

### Subscribe (Receive Messages)

| Topic | Purpose | When to Subscribe |
|-------|---------|-------------------|
| `/topic/game/{gameId}` | Game state updates | After game created/joined |
| `/user/queue/match-found` | Match notification | After joining matchmaking queue |
| `/user/queue/errors` | Error messages | Always (optional but recommended) |

### Send (Send Messages)

| Destination | Purpose | Data |
|-------------|---------|------|
| `/app/game/{gameId}/move` | Make move | `{row, col}` |
| `/app/game/{gameId}/forfeit` | Forfeit game | (no body) |

---

## JavaScript Client Setup

### Using SockJS + StompJS

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

class WebSocketClient {
  constructor(baseURL, accessToken) {
    this.baseURL = baseURL || 'http://localhost:8080';
    this.accessToken = accessToken;
    this.stompClient = null;
    this.connected = false;
  }

  connect() {
    return new Promise((resolve, reject) => {
      // Create SockJS connection
      const socket = new SockJS(`${this.baseURL}/ws`);

      // Create STOMP client
      this.stompClient = Stomp.over(socket);

      // Connect with JWT token
      const headers = {
        'Authorization': `Bearer ${this.accessToken}`
      };

      this.stompClient.connect(
        headers,
        (frame) => {
          console.log('✓ WebSocket connected');
          this.connected = true;
          resolve(frame);
        },
        (error) => {
          console.error('✗ WebSocket connection error:', error);
          this.connected = false;
          reject(error);
        }
      );

      // Disable debug logging (optional)
      this.stompClient.debug = () => {};
    });
  }

  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.disconnect(() => {
        console.log('WebSocket disconnected');
        this.connected = false;
      });
    }
  }

  subscribe(topic, callback) {
    if (!this.connected) {
      throw new Error('WebSocket not connected');
    }

    return this.stompClient.subscribe(topic, (message) => {
      const data = JSON.parse(message.body);
      callback(data);
    });
  }

  send(destination, body) {
    if (!this.connected) {
      throw new Error('WebSocket not connected');
    }

    this.stompClient.send(destination, {}, JSON.stringify(body));
  }

  isConnected() {
    return this.connected;
  }
}

// Usage
const ws = new WebSocketClient('http://localhost:8080', accessToken);
await ws.connect();
```

---

## SEND: Make Move

Send a move to the server (PvP games only).

**Destination**: `/app/game/{gameId}/move`

**Body:**
```json
{
  "row": 7,
  "col": 7
}
```

**Validation:**
- `row`: 0-14 (integer)
- `col`: 0-14 (integer)
- Position must be empty
- Must be your turn

**Broadcast Response:**

Server broadcasts updated game state to `/topic/game/{gameId}` for ALL subscribers (both players).

```json
{
  "gameId": "c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "gameType": "HUMAN_VS_HUMAN",
  "status": "IN_PROGRESS",
  "player1Id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "player2Id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "board": [
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    ...
  ],
  "currentPlayer": 2,
  "moveCount": 5,
  "winnerType": "NONE",
  "winnerId": null,
  "startedAt": "2025-10-17T14:30:00",
  "lastActivity": "2025-10-17T14:32:45"
}
```

**Error Handling:**

If move is invalid (out of bounds, position occupied, not your turn), error sent to `/user/queue/errors` (ONLY to the player who sent the invalid move).

```json
{
  "errorCode": "INVALID_MOVE",
  "message": "Position already occupied",
  "exceptionType": "InvalidMoveException"
}
```

**Example:**

```javascript
// Subscribe to game updates
ws.subscribe(`/topic/game/${gameId}`, (gameState) => {
  console.log('Game updated:', gameState);
  renderBoard(gameState.board);
  updateTurnIndicator(gameState.currentPlayer);

  if (gameState.status === 'COMPLETED') {
    showGameOver(gameState.winnerType);
  }
});

// Subscribe to errors
ws.subscribe('/user/queue/errors', (error) => {
  console.error('Error:', error.message);
  alert(error.message);
});

// Send move
ws.send(`/app/game/${gameId}/move`, { row: 7, col: 7 });
```

---

## SEND: Forfeit Game

Forfeit the game. Opponent wins immediately.

**Destination**: `/app/game/{gameId}/forfeit`

**Body**: (empty, no body required)

**Broadcast Response:**

Server broadcasts final game state to `/topic/game/{gameId}` for ALL subscribers.

```json
{
  "gameId": "c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "status": "ABANDONED",
  "winnerType": "PLAYER2",
  "winnerId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "endedAt": "2025-10-17T14:40:00",
  ...
}
```

**Example:**

```javascript
// Forfeit game
ws.send(`/app/game/${gameId}/forfeit`, {});

// Both players receive final state via /topic/game/{gameId} subscription
```

---

## SUBSCRIBE: Game Updates

Receive real-time game state updates.

**Topic**: `/topic/game/{gameId}`

**When to Subscribe**: Immediately after joining/creating a PvP game.

**Message Format:**

See "SEND: Make Move" response format above.

**Example:**

```javascript
// Subscribe when game starts
const subscription = ws.subscribe(`/topic/game/${gameId}`, (gameState) => {
  console.log('Turn:', gameState.currentPlayer);
  console.log('Moves:', gameState.moveCount);

  // Update UI
  renderBoard(gameState.board);

  // Check if my turn
  const isMyTurn = (myPlayerNumber === gameState.currentPlayer);
  if (isMyTurn) {
    enableBoardInput();
  } else {
    disableBoardInput();
  }

  // Check if game ended
  if (gameState.status === 'COMPLETED' || gameState.status === 'ABANDONED') {
    handleGameEnd(gameState);
  }
});

// Unsubscribe when leaving game
subscription.unsubscribe();
```

---

## SUBSCRIBE: Match Found

Receive notification when matchmaking finds a match.

**Topic**: `/user/queue/match-found`

**When to Subscribe**: After joining matchmaking queue.

**Message Format:**

```json
{
  "gameId": "c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "playerNumber": 1,
  "opponentId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "websocketTopic": "/topic/game/c7fd922a-831a-48d2-ab6e-a519628e5e0f"
}
```

**Example:**

```javascript
// Subscribe before joining queue
ws.subscribe('/user/queue/match-found', (matchData) => {
  console.log('Match found!');
  console.log('Game ID:', matchData.gameId);
  console.log('You are Player', matchData.playerNumber);

  // Store player number
  localStorage.setItem('myPlayerNumber', matchData.playerNumber);

  // Subscribe to game updates
  ws.subscribe(matchData.websocketTopic, handleGameUpdate);

  // Navigate to game page
  window.location.href = `/game/${matchData.gameId}`;
});

// Join matchmaking queue
await fetch('http://localhost:8080/api/matchmaking/queue', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

---

## SUBSCRIBE: Error Messages

Receive error messages for invalid operations.

**Topic**: `/user/queue/errors`

**When to Subscribe**: Always (recommended for better error handling).

**Message Format:**

```json
{
  "errorCode": "INVALID_MOVE",
  "message": "Position already occupied",
  "exceptionType": "InvalidMoveException"
}
```

**Error Codes:**

| Code | Description |
|------|-------------|
| `GAME_NOT_FOUND` | Game doesn't exist |
| `INVALID_MOVE` | Move is invalid (out of bounds, occupied, etc.) |
| `UNAUTHORIZED` | Not a participant in this game |
| `GAME_COMPLETED` | Game has already ended |
| `NOT_YOUR_TURN` | Not your turn to move |
| `ILLEGAL_STATE` | Invalid game state |
| `INTERNAL_ERROR` | Server error |

**Example:**

```javascript
ws.subscribe('/user/queue/errors', (error) => {
  console.error(`[${error.errorCode}] ${error.message}`);

  // Show user-friendly error message
  switch (error.errorCode) {
    case 'INVALID_MOVE':
      showError('Invalid move. Please try again.');
      break;
    case 'NOT_YOUR_TURN':
      showError("It's not your turn yet!");
      break;
    case 'GAME_COMPLETED':
      showError('This game has already ended.');
      break;
    default:
      showError('An error occurred. Please try again.');
  }
});
```

---

## Complete Example: PvP Game

```javascript
class GomokuGame {
  constructor(gameId, myPlayerNumber, wsClient) {
    this.gameId = gameId;
    this.myPlayerNumber = myPlayerNumber;
    this.ws = wsClient;
    this.gameState = null;
    this.gameSubscription = null;
  }

  start() {
    // Subscribe to game updates
    this.gameSubscription = this.ws.subscribe(
      `/topic/game/${this.gameId}`,
      (gameState) => this.handleGameUpdate(gameState)
    );

    // Subscribe to errors
    this.ws.subscribe('/user/queue/errors', (error) => {
      this.handleError(error);
    });

    console.log('Game started. Waiting for updates...');
  }

  handleGameUpdate(gameState) {
    this.gameState = gameState;

    console.log(`Move ${gameState.moveCount}:`);
    console.log(`Current player: ${gameState.currentPlayer}`);

    // Render board
    this.renderBoard(gameState.board);

    // Update turn indicator
    const isMyTurn = (this.myPlayerNumber === gameState.currentPlayer);
    this.updateTurnIndicator(isMyTurn);

    // Check if game ended
    if (gameState.status === 'COMPLETED') {
      this.handleGameEnd(gameState);
    } else if (gameState.status === 'ABANDONED') {
      this.handleGameAbandoned(gameState);
    }
  }

  makeMove(row, col) {
    if (!this.isMyTurn()) {
      alert("It's not your turn!");
      return;
    }

    console.log(`Sending move: (${row}, ${col})`);

    this.ws.send(`/app/game/${this.gameId}/move`, { row, col });
  }

  forfeit() {
    if (confirm('Are you sure you want to forfeit?')) {
      this.ws.send(`/app/game/${this.gameId}/forfeit`, {});
    }
  }

  isMyTurn() {
    return this.gameState &&
           this.gameState.currentPlayer === this.myPlayerNumber &&
           this.gameState.status === 'IN_PROGRESS';
  }

  renderBoard(board) {
    // Render 15x15 board
    const boardElement = document.getElementById('game-board');
    boardElement.innerHTML = '';

    for (let row = 0; row < 15; row++) {
      const rowElement = document.createElement('div');
      rowElement.className = 'board-row';

      for (let col = 0; col < 15; col++) {
        const cellElement = document.createElement('div');
        cellElement.className = 'board-cell';
        cellElement.dataset.row = row;
        cellElement.dataset.col = col;

        if (board[row][col] === 1) {
          cellElement.classList.add('black-stone');
        } else if (board[row][col] === 2) {
          cellElement.classList.add('white-stone');
        } else if (this.isMyTurn()) {
          cellElement.classList.add('clickable');
          cellElement.addEventListener('click', () => {
            this.makeMove(row, col);
          });
        }

        rowElement.appendChild(cellElement);
      }

      boardElement.appendChild(rowElement);
    }
  }

  updateTurnIndicator(isMyTurn) {
    const indicator = document.getElementById('turn-indicator');
    indicator.textContent = isMyTurn ? 'Your turn!' : "Opponent's turn";
    indicator.className = isMyTurn ? 'my-turn' : 'opponent-turn';
  }

  handleGameEnd(gameState) {
    console.log('Game ended!');
    console.log('Winner:', gameState.winnerType);

    let message;
    if (gameState.winnerType === 'DRAW') {
      message = 'Game ended in a draw!';
    } else {
      const iWon = (
        (gameState.winnerType === 'PLAYER1' && this.myPlayerNumber === 1) ||
        (gameState.winnerType === 'PLAYER2' && this.myPlayerNumber === 2)
      );
      message = iWon ? 'You won! 🎉' : 'You lost. Better luck next time!';
    }

    alert(message);

    // Disable board interaction
    document.getElementById('game-board').classList.add('game-over');
  }

  handleGameAbandoned(gameState) {
    alert('Opponent forfeited. You win!');
  }

  handleError(error) {
    console.error('Game error:', error);
    alert(`Error: ${error.message}`);
  }

  stop() {
    if (this.gameSubscription) {
      this.gameSubscription.unsubscribe();
    }
  }
}

// Usage
const ws = new WebSocketClient('http://localhost:8080', accessToken);
await ws.connect();

const game = new GomokuGame(gameId, myPlayerNumber, ws);
game.start();

// When player clicks forfeit button
document.getElementById('forfeit-btn').addEventListener('click', () => {
  game.forfeit();
});

// When leaving page
window.addEventListener('beforeunload', () => {
  game.stop();
  ws.disconnect();
});
```

---

## Reconnection Handling

```javascript
class RobustWebSocketClient extends WebSocketClient {
  constructor(baseURL, accessToken, options = {}) {
    super(baseURL, accessToken);
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = options.maxReconnectAttempts || 5;
    this.reconnectDelay = options.reconnectDelay || 2000;
    this.onReconnect = options.onReconnect || (() => {});
  }

  connect() {
    return super.connect().catch((error) => {
      console.error('Connection failed:', error);
      this.attemptReconnect();
      throw error;
    });
  }

  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * this.reconnectAttempts;

    console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})...`);

    setTimeout(() => {
      this.connect()
        .then(() => {
          console.log('Reconnected successfully');
          this.reconnectAttempts = 0;
          this.onReconnect();
        })
        .catch(() => {
          this.attemptReconnect();
        });
    }, delay);
  }
}

// Usage
const ws = new RobustWebSocketClient('http://localhost:8080', accessToken, {
  maxReconnectAttempts: 5,
  reconnectDelay: 2000,
  onReconnect: () => {
    // Re-subscribe to topics
    ws.subscribe(`/topic/game/${gameId}`, handleGameUpdate);
    ws.subscribe('/user/queue/errors', handleError);
  }
});

await ws.connect();
```

---

## Testing

**Test WebSocket Connection:**
```javascript
// 1. Connect
const ws = new WebSocketClient('http://localhost:8080', accessToken);
await ws.connect();
console.log('Connected:', ws.isConnected());

// 2. Subscribe to test topic
ws.subscribe('/user/queue/errors', (msg) => {
  console.log('Received:', msg);
});

// 3. Trigger error (send invalid move)
ws.send(`/app/game/invalid-game-id/move`, { row: 7, col: 7 });

// 4. Should receive error message
// Expected: {errorCode: "GAME_NOT_FOUND", ...}
```

**Test PvP Game:**
```
1. Open two browser windows
2. Login as different users in each
3. Create PvP game in Window 1
4. Connect to same game in Window 2
5. Make moves alternately
6. Verify both windows update in real-time
```
