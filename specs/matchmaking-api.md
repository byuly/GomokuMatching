# Matchmaking API

Base URL: `http://localhost:8080/api/matchmaking`

All endpoints require JWT authentication.

---

## Matchmaking Flow

```
1. Player joins queue         → POST /api/matchmaking/queue
2. Kafka Streams finds match  → (Real-time, < 100ms when >= 2 players)
3. WebSocket notification     → /user/{userId}/queue/match-found
4. Players connect to game    → WebSocket /topic/game/{gameId}
```

---

## Endpoints

1. [Join Queue](#post-queue)
2. [Leave Queue](#delete-queue)
3. [Get Queue Status](#get-status)
4. [Get Statistics](#get-stats)

---

## POST `/queue`

Join the matchmaking queue. When a match is found, player receives WebSocket notification.

### Request

**Headers:**
```
Authorization: Bearer <access_token>
```

**No body required**

### Response

**Success (200 OK) - Joined:**
```json
{
  "status": "JOINED",
  "message": "Successfully joined matchmaking queue",
  "queuePosition": 0,
  "totalPlayersInQueue": 0,
  "estimatedWaitSeconds": 5,
  "joinedAt": "2025-10-19T01:54:33"
}
```

**Success (200 OK) - Already in queue:**
```json
{
  "status": "ALREADY_IN_QUEUE",
  "message": "You are already in the matchmaking queue",
  "queuePosition": 0,
  "totalPlayersInQueue": 0,
  "estimatedWaitSeconds": 5,
  "joinedAt": "2025-10-19T01:54:33"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `status` | enum | `JOINED` or `ALREADY_IN_QUEUE` |
| `message` | string | Human-readable message |
| `queuePosition` | int | Position in queue (note: current implementation returns 0) |
| `totalPlayersInQueue` | long | Total players in queue (note: current implementation returns 0) |
| `estimatedWaitSeconds` | int | Estimated wait time (5 seconds for <= 2 players) |
| `joinedAt` | timestamp | When player joined queue (ISO 8601 format) |

**Error Responses:**

**401 Unauthorized** - No/invalid token

### Examples

**cURL:**
```bash
curl -X POST http://localhost:8080/api/matchmaking/queue \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript:**
```javascript
async function joinMatchmaking() {
  const response = await fetch('http://localhost:8080/api/matchmaking/queue', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  const data = await response.json();

  console.log(`Status: ${data.status}`);
  console.log(`Message: ${data.message}`);
  console.log(`Joined at: ${data.joinedAt}`);
  console.log(`Estimated wait: ${data.estimatedWaitSeconds} seconds`);

  // Subscribe to WebSocket for match notification
  stompClient.subscribe(`/user/queue/match-found`, (message) => {
    const matchData = JSON.parse(message.body);
    console.log('Match found! Game ID:', matchData.gameId);
    window.location.href = `/game/${matchData.gameId}`;
  });
}
```

---

## DELETE `/queue`

Leave the matchmaking queue.

### Request

**Headers:**
```
Authorization: Bearer <access_token>
```

**No body required**

### Response

**Success (200 OK) - Left:**
```json
{
  "status": "LEFT",
  "message": "You have left the matchmaking queue"
}
```

**Success (200 OK) - Not in queue:**
```json
{
  "status": "NOT_IN_QUEUE",
  "message": "You are not in the queue"
}
```

**Error Responses:**

**401 Unauthorized** - No/invalid token

### Examples

**cURL:**
```bash
curl -X DELETE http://localhost:8080/api/matchmaking/queue \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript:**
```javascript
async function leaveMatchmaking() {
  const response = await fetch('http://localhost:8080/api/matchmaking/queue', {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  const data = await response.json();
  console.log(data.message);
}
```

---

## GET `/status`

Get current queue status for authenticated user.

### Request

**Headers:**
```
Authorization: Bearer <access_token>
```

### Response

**Success (200 OK):**
```json
{
  "status": "NOT_IN_QUEUE",
  "message": "You are not in the matchmaking queue",
  "queuePosition": null,
  "totalPlayersInQueue": null,
  "estimatedWaitSeconds": null,
  "joinedAt": null
}
```

**Note:** The current implementation always returns `NOT_IN_QUEUE` because Kafka Streams maintains the queue state internally. For real-time queue status, clients should rely on WebSocket notifications rather than polling this endpoint.

**Error Responses:**

**401 Unauthorized** - No/invalid token

### Examples

**cURL:**
```bash
curl -X GET http://localhost:8080/api/matchmaking/status \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript:**
```javascript
// Check queue status (note: always returns NOT_IN_QUEUE in current implementation)
const response = await fetch('http://localhost:8080/api/matchmaking/status', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

const data = await response.json();
console.log(`Status: ${data.status}`); // Always "NOT_IN_QUEUE"

// Instead, rely on WebSocket for match notifications
stompClient.subscribe('/user/queue/match-found', (message) => {
  const matchData = JSON.parse(message.body);
  console.log('Match found!', matchData);
});
```

---

## GET `/stats`

Get matchmaking statistics (admin/monitoring endpoint).

### Request

**Headers:**
```
Authorization: Bearer <access_token>
```

**Note**: In production, this should be restricted to admin users only.

### Response

**Success (200 OK):**
```json
{
  "architecture": "event-driven-kafka-streams",
  "message": "detailed stats available via kafka metrics"
}
```

**Note:** The current implementation uses Kafka Streams for event-driven matchmaking. Detailed metrics are available through Kafka's built-in metrics system (JMX). This endpoint is a placeholder for future custom statistics.

**Error Responses:**

**401 Unauthorized** - No/invalid token

### Examples

**cURL:**
```bash
curl -X GET http://localhost:8080/api/matchmaking/stats \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript:**
```javascript
async function getMatchmakingStats() {
  const response = await fetch('http://localhost:8080/api/matchmaking/stats', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  const stats = await response.json();
  console.log('Architecture:', stats.architecture);
  console.log('Message:', stats.message);

  return stats;
}
```

---

## WebSocket Notifications

### Match Found Notification

When a match is found, both players receive a WebSocket notification.

**Topic**: `/user/queue/match-found`

**Message:**
```json
{
  "gameId": "8dbcc33a-0b5e-46a0-bf9f-348f0185bfea",
  "gameType": "HUMAN_VS_HUMAN",
  "opponentId": "c9712dc9-9e80-4be8-8dd2-e41fe3232458",
  "yourPlayerNumber": 1,
  "yourColor": "BLACK",
  "websocketTopic": "/topic/game/8dbcc33a-0b5e-46a0-bf9f-348f0185bfea",
  "message": "Match found! Your opponent is ready."
}
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `gameId` | UUID | Game identifier |
| `gameType` | string | Game type (always "HUMAN_VS_HUMAN" for matchmaking) |
| `opponentId` | UUID | Opponent's player ID |
| `yourPlayerNumber` | int | Your player number (1 or 2) |
| `yourColor` | string | Your piece color ("BLACK" for player 1, "WHITE" for player 2) |
| `websocketTopic` | string | Topic to subscribe for game updates |
| `message` | string | Human-readable notification message |

**JavaScript Example:**
```javascript
// Subscribe to match notifications
stompClient.subscribe('/user/queue/match-found', (message) => {
  const data = JSON.parse(message.body);

  console.log('Match found!');
  console.log('Game ID:', data.gameId);
  console.log('You are Player', data.yourPlayerNumber, `(${data.yourColor})`);
  console.log('Opponent ID:', data.opponentId);

  // Subscribe to game updates
  stompClient.subscribe(data.websocketTopic, handleGameUpdate);

  // Navigate to game page
  window.location.href = `/game/${data.gameId}`;
});
```

---

## Complete Frontend Integration

```javascript
class MatchmakingService {
  constructor(baseURL, authService, stompClient) {
    this.baseURL = baseURL || 'http://localhost:8080/api/matchmaking';
    this.auth = authService;
    this.stomp = stompClient;
    this.inQueue = false;
  }

  getHeaders() {
    return {
      'Authorization': `Bearer ${this.auth.getAccessToken()}`
    };
  }

  async joinQueue() {
    const response = await fetch(`${this.baseURL}/queue`, {
      method: 'POST',
      headers: this.getHeaders()
    });

    if (!response.ok) throw new Error('Failed to join queue');

    const data = await response.json();
    this.inQueue = true;

    console.log(`Joined queue at ${data.joinedAt}`);
    console.log(`Estimated wait: ${data.estimatedWaitSeconds}s`);

    // Subscribe to match notification
    this.subscribeToMatchNotification();

    return data;
  }

  async leaveQueue() {
    const response = await fetch(`${this.baseURL}/queue`, {
      method: 'DELETE',
      headers: this.getHeaders()
    });

    if (!response.ok) throw new Error('Failed to leave queue');

    const data = await response.json();
    this.inQueue = false;

    console.log('Left queue');
    return data;
  }

  subscribeToMatchNotification() {
    this.stomp.subscribe('/user/queue/match-found', (message) => {
      const data = JSON.parse(message.body);

      this.inQueue = false;

      console.log('Match found!', data);
      this.onMatchFound?.(data);
    });
  }

  // Callback
  onMatchFound = null;    // (matchData) => {}
}

// Usage
const matchmaking = new MatchmakingService(
  'http://localhost:8080/api/matchmaking',
  authService,
  stompClient
);

// Set callback
matchmaking.onMatchFound = (matchData) => {
  alert(`Match found! You are Player ${matchData.yourPlayerNumber} (${matchData.yourColor})`);
  window.location.href = `/game/${matchData.gameId}`;
};

// Join queue
await matchmaking.joinQueue();

// Leave queue
await matchmaking.leaveQueue();
```

---

## Matchmaking Algorithm

**Architecture**: Event-Driven with Kafka Streams

**Data Structure**: Kafka Streams State Store (RocksDB)
- Queue state maintained as stateful aggregation
- FIFO ordering with LinkedHashMap
- Deduplication via matched players set

**Matching Process**:
1. Player joins → QueueEvent published to Kafka
2. Kafka Streams aggregates events into MatchmakingState
3. When state has >=2 players → instantly creates match (< 100ms)
4. Match created → GameService creates session
5. WebSocket notifications sent to both players
6. Cleanup events remove players from queue

**Key Benefits**:
- Real-time matching (no polling, no scheduler delays)
- Fault-tolerant state (RocksDB + Kafka changelog)
- Strict FIFO ordering (single partition key)
- Event sourcing pattern

**Future Enhancement**: MMR-based matching
- Partition by MMR range for parallel processing
- Match players with similar skill rating (±200 points)
- Fallback to FIFO if no good match within time window

---

## Testing

**Test Scenario 1: Successful Match**
```javascript
// Player 1
await matchmaking.joinQueue();
// Joined queue, waiting for opponent...

// Player 2 (different browser/user)
await matchmaking.joinQueue();
// Joined queue

// < 100ms later...
// Both receive WebSocket match notification instantly
// Both redirected to game
```

**Test Scenario 2: Leave Queue**
```javascript
// Join
await matchmaking.joinQueue();
console.log('In queue, waiting for match...');

// Wait 5 seconds
await new Promise(resolve => setTimeout(resolve, 5000));

// Leave
const response = await matchmaking.leaveQueue();
console.log(response.message); // "Successfully left matchmaking queue"
```

**Test Scenario 3: Already in Queue**
```javascript
// Join
await matchmaking.joinQueue();

// Try to join again
const response = await matchmaking.joinQueue();
console.log(response.status); // "ALREADY_IN_QUEUE"
```
