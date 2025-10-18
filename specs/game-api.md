# Game API

Base URL: `http://localhost:8080/api/game`

All endpoints require JWT authentication.

---

## Endpoints

1. [Create Game](#post-create)
2. [Get Game State](#get-gameid)
3. [Make Move (PvAI)](#post-gameidmove)
4. [Forfeit Game](#post-gameidforfeit)
5. [Get Move History](#get-gameidmoves)

---

## POST `/create`

Create a new game (PvP or PvAI).

### Request

**Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Body (PvP Game):**
```json
{
  "gameType": "HUMAN_VS_HUMAN",
  "player2Id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "aiDifficulty": null
}
```

**Body (PvAI Game):**
```json
{
  "gameType": "HUMAN_VS_AI",
  "player2Id": null,
  "aiDifficulty": "MEDIUM"
}
```

**Validation Rules:**
- For `HUMAN_VS_HUMAN`: `player2Id` must be set, `aiDifficulty` must be null
- For `HUMAN_VS_AI`: `aiDifficulty` must be set, `player2Id` must be null
- Valid AI difficulties: `EASY`, `MEDIUM`, `HARD`, `EXPERT`

### Response

**Success (201 Created) - PvP:**
```json
{
  "gameId": "c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "gameType": "HUMAN_VS_HUMAN",
  "websocketTopic": "/topic/game/c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "message": "Game created. Connect to WebSocket for real-time updates."
}
```

**Success (201 Created) - PvAI:**
```json
{
  "gameId": "d8ge033b-942b-59e3-bc7f-b620739f6f1g",
  "gameType": "HUMAN_VS_AI",
  "message": "Game created. Use REST API to make moves."
}
```

**Error Responses:**

**400 Bad Request** - Invalid request:
```json
{
  "message": "Invalid game request. For PvP games, set player2Id. For PvAI games, set aiDifficulty."
}
```

**401 Unauthorized** - No/invalid token

**404 Not Found** - Player2 doesn't exist (PvP)

### Examples

**cURL (PvP):**
```bash
curl -X POST http://localhost:8080/api/game/create \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "gameType": "HUMAN_VS_HUMAN",
    "player2Id": "b2c3d4e5-f6a7-8901-bcde-f12345678901"
  }'
```

**cURL (PvAI):**
```bash
curl -X POST http://localhost:8080/api/game/create \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "gameType": "HUMAN_VS_AI",
    "aiDifficulty": "MEDIUM"
  }'
```

**JavaScript:**
```javascript
// Create PvAI game
const response = await fetch('http://localhost:8080/api/game/create', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    gameType: 'HUMAN_VS_AI',
    aiDifficulty: 'MEDIUM'
  })
});

const data = await response.json();
console.log('Game ID:', data.gameId);
```

---

## GET `/{gameId}`

Get current game state.

### Request

**Headers:**
```
Authorization: Bearer <access_token>
```

**Path Parameters:**
- `gameId` (UUID): Game identifier

### Response

**Success (200 OK):**
```json
{
  "gameId": "c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "gameType": "HUMAN_VS_HUMAN",
  "status": "IN_PROGRESS",
  "player1Id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "player2Id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "aiDifficulty": null,
  "board": [
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    [0,0,0,0,0,0,1,0,0,0,0,0,0,0,0],
    [0,0,0,0,0,0,2,1,0,0,0,0,0,0,0],
    ...
  ],
  "currentPlayer": 1,
  "moveCount": 4,
  "winnerType": "NONE",
  "winnerId": null,
  "startedAt": "2025-10-17T14:30:00",
  "endedAt": null,
  "lastActivity": "2025-10-17T14:32:15"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `gameId` | UUID | Unique game identifier |
| `gameType` | enum | `HUMAN_VS_HUMAN` or `HUMAN_VS_AI` |
| `status` | enum | `IN_PROGRESS`, `COMPLETED`, or `ABANDONED` |
| `player1Id` | UUID | Player 1 ID (BLACK stones, goes first) |
| `player2Id` | UUID\|null | Player 2 ID (null for AI games) |
| `aiDifficulty` | string\|null | AI difficulty (null for PvP games) |
| `board` | int[][] | 15x15 board (0=empty, 1=player1, 2=player2/AI) |
| `currentPlayer` | int | Current turn (1 or 2) |
| `moveCount` | int | Total moves made |
| `winnerType` | enum | `NONE`, `PLAYER1`, `PLAYER2`, `AI`, or `DRAW` |
| `winnerId` | UUID\|null | Winner's player ID (null if AI won, draw, or in progress) |
| `startedAt` | timestamp | Game start time |
| `endedAt` | timestamp\|null | Game end time (null if in progress) |
| `lastActivity` | timestamp | Last move time |

**Error Responses:**

**401 Unauthorized** - No/invalid token

**403 Forbidden** - Not a participant in this game

**404 Not Found** - Game doesn't exist

### Examples

**cURL:**
```bash
curl -X GET http://localhost:8080/api/game/c7fd922a-831a-48d2-ab6e-a519628e5e0f \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript:**
```javascript
const response = await fetch(`http://localhost:8080/api/game/${gameId}`, {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

const gameState = await response.json();
console.log('Current player:', gameState.currentPlayer);
console.log('Move count:', gameState.moveCount);
```

---

## POST `/{gameId}/move`

Make a move in a **PvAI game only**. For PvP games, use WebSocket instead.

### Request

**Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

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
- Position must be empty (board[row][col] == 0)
- Must be player's turn

### Response

**Success (200 OK):**

Returns updated game state after both player move AND AI response.

```json
{
  "gameId": "d8ge033b-942b-59e3-bc7f-b620739f6f1g",
  "gameType": "HUMAN_VS_AI",
  "status": "IN_PROGRESS",
  "player1Id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "player2Id": null,
  "aiDifficulty": "MEDIUM",
  "board": [
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],
    ...
    [0,0,0,0,0,0,1,2,0,0,0,0,0,0,0],
    ...
  ],
  "currentPlayer": 1,
  "moveCount": 6,
  "winnerType": "NONE",
  "winnerId": null,
  "startedAt": "2025-10-17T14:30:00",
  "endedAt": null,
  "lastActivity": "2025-10-17T14:35:22"
}
```

**Error Responses:**

**400 Bad Request** - Invalid move:
```json
{
  "message": "Position out of bounds"
}
```

```json
{
  "message": "Position already occupied"
}
```

**401 Unauthorized** - No/invalid token

**403 Forbidden** - Not your turn or not PvAI game:
```json
{
  "message": "Not your turn"
}
```

**404 Not Found** - Game doesn't exist

### Examples

**cURL:**
```bash
curl -X POST http://localhost:8080/api/game/d8ge033b-942b-59e3-bc7f-b620739f6f1g/move \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "row": 7,
    "col": 7
  }'
```

**JavaScript:**
```javascript
async function makeMove(gameId, row, col) {
  const response = await fetch(`http://localhost:8080/api/game/${gameId}/move`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ row, col })
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  const gameState = await response.json();

  // Check if game ended
  if (gameState.status === 'COMPLETED') {
    if (gameState.winnerType === 'PLAYER1') {
      console.log('You won!');
    } else if (gameState.winnerType === 'AI') {
      console.log('AI won!');
    } else if (gameState.winnerType === 'DRAW') {
      console.log('Draw!');
    }
  }

  return gameState;
}

// Usage
await makeMove(gameId, 7, 7);
```

---

## POST `/{gameId}/forfeit`

Forfeit the game. Opponent wins automatically.

### Request

**Headers:**
```
Authorization: Bearer <access_token>
```

**Path Parameters:**
- `gameId` (UUID): Game identifier

### Response

**Success (200 OK):**

Returns final game state with status `ABANDONED`.

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

**Error Responses:**

**401 Unauthorized** - No/invalid token

**403 Forbidden** - Not a participant

**404 Not Found** - Game doesn't exist

**409 Conflict** - Game already completed:
```json
{
  "message": "Game has already ended"
}
```

### Examples

**cURL:**
```bash
curl -X POST http://localhost:8080/api/game/c7fd922a-831a-48d2-ab6e-a519628e5e0f/forfeit \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript:**
```javascript
async function forfeitGame(gameId) {
  const response = await fetch(`http://localhost:8080/api/game/${gameId}/forfeit`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  const gameState = await response.json();
  console.log('Game forfeited. Winner:', gameState.winnerType);
}
```

---

## GET `/{gameId}/moves`

Get complete move history for game replay.

### Request

**Headers:**
```
Authorization: Bearer <access_token>
```

**Path Parameters:**
- `gameId` (UUID): Game identifier

### Response

**Success (200 OK):**

Array of all moves in chronological order.

```json
[
  {
    "moveNumber": 1,
    "playerType": "HUMAN",
    "playerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "boardX": 7,
    "boardY": 7,
    "stoneColor": "BLACK",
    "timestamp": "2025-10-17T14:30:15"
  },
  {
    "moveNumber": 2,
    "playerType": "AI",
    "playerId": null,
    "boardX": 8,
    "boardY": 7,
    "stoneColor": "WHITE",
    "timestamp": "2025-10-17T14:30:17"
  },
  ...
]
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `moveNumber` | int | Move sequence (1-indexed) |
| `playerType` | enum | `HUMAN` or `AI` |
| `playerId` | UUID\|null | Player ID (null for AI moves) |
| `boardX` | int | Row (0-14) |
| `boardY` | int | Column (0-14) |
| `stoneColor` | enum | `BLACK` or `WHITE` |
| `timestamp` | timestamp | When move was made |

**Error Responses:**

**401 Unauthorized** - No/invalid token

**403 Forbidden** - Not a participant

**404 Not Found** - Game doesn't exist

### Examples

**cURL:**
```bash
curl -X GET http://localhost:8080/api/game/c7fd922a-831a-48d2-ab6e-a519628e5e0f/moves \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript (Game Replay):**
```javascript
async function replayGame(gameId) {
  const response = await fetch(`http://localhost:8080/api/game/${gameId}/moves`, {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  const moves = await response.json();

  // Replay moves with animation
  const board = Array(15).fill(null).map(() => Array(15).fill(0));

  for (const move of moves) {
    await new Promise(resolve => setTimeout(resolve, 500)); // 500ms delay

    const player = move.stoneColor === 'BLACK' ? 1 : 2;
    board[move.boardX][move.boardY] = player;

    console.log(`Move ${move.moveNumber}: ${move.playerType} at (${move.boardX}, ${move.boardY})`);
    renderBoard(board);
  }

  console.log('Replay complete');
}
```

---

## Player Profiles

Base URL: `http://localhost:8080/api/profiles`

### GET `/me`

Get current user's profile.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response (200 OK):**
```json
{
  "playerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": "player_one",
  "email": "player1@example.com",
  "createdAt": "2025-10-15T10:00:00",
  "lastLogin": "2025-10-17T14:25:00"
}
```

### PUT `/username`

Update username.

**Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Body:**
```json
{
  "username": "new_username"
}
```

**Validation:**
- 3-50 characters
- Alphanumeric + underscores only
- Must be unique

**Response (200 OK):**
```json
{
  "message": "Username updated successfully"
}
```

---

## Complete Frontend Integration Example

```javascript
class GameAPI {
  constructor(baseURL, authService) {
    this.baseURL = baseURL || 'http://localhost:8080/api/game';
    this.auth = authService;
  }

  getHeaders() {
    return {
      'Authorization': `Bearer ${this.auth.getAccessToken()}`,
      'Content-Type': 'application/json'
    };
  }

  async createPvAIGame(difficulty = 'MEDIUM') {
    const response = await fetch(`${this.baseURL}/create`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({
        gameType: 'HUMAN_VS_AI',
        aiDifficulty: difficulty
      })
    });

    if (!response.ok) throw new Error('Failed to create game');
    return await response.json();
  }

  async getGameState(gameId) {
    const response = await fetch(`${this.baseURL}/${gameId}`, {
      headers: this.getHeaders()
    });

    if (!response.ok) throw new Error('Failed to get game state');
    return await response.json();
  }

  async makeMove(gameId, row, col) {
    const response = await fetch(`${this.baseURL}/${gameId}/move`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({ row, col })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    return await response.json();
  }

  async forfeit(gameId) {
    const response = await fetch(`${this.baseURL}/${gameId}/forfeit`, {
      method: 'POST',
      headers: this.getHeaders()
    });

    if (!response.ok) throw new Error('Failed to forfeit');
    return await response.json();
  }

  async getMoveHistory(gameId) {
    const response = await fetch(`${this.baseURL}/${gameId}/moves`, {
      headers: this.getHeaders()
    });

    if (!response.ok) throw new Error('Failed to get moves');
    return await response.json();
  }
}

// Usage
const gameAPI = new GameAPI('http://localhost:8080/api/game', authService);

// Create game
const game = await gameAPI.createPvAIGame('HARD');
console.log('Game created:', game.gameId);

// Make move
const updatedState = await gameAPI.makeMove(game.gameId, 7, 7);
console.log('Move count:', updatedState.moveCount);

// Get current state
const state = await gameAPI.getGameState(game.gameId);

// Get move history for replay
const moves = await gameAPI.getMoveHistory(game.gameId);
```
