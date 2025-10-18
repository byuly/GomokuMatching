# Matchmaking API

Base URL: `http://localhost:8080/api/matchmaking`

All endpoints require JWT authentication.

---

## Matchmaking Flow

```
1. Player joins queue      → POST /api/matchmaking/queue
2. Scheduler finds match    → (Automatic, every 2 seconds)
3. WebSocket notification   → /user/{userId}/queue/match-found
4. Players connect to game  → WebSocket /topic/game/{gameId}
```

---

## Endpoints

1. [Join Queue](#post-queue)
2. [Leave Queue](#delete-queue)
3. [Get Queue Status](#get-status)
4. [Get Statistics](#get-stats)
5. [Health Check](#get-health)

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
  "queuePosition": 3,
  "queueSize": 5,
  "estimatedWaitSeconds": 6,
  "message": "You have joined the matchmaking queue"
}
```

**Success (200 OK) - Already in queue:**
```json
{
  "status": "ALREADY_IN_QUEUE",
  "queuePosition": 2,
  "queueSize": 4,
  "estimatedWaitSeconds": 4,
  "message": "You are already in the queue"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `status` | enum | `JOINED` or `ALREADY_IN_QUEUE` |
| `queuePosition` | int | Your position in queue (1-indexed) |
| `queueSize` | int | Total players in queue |
| `estimatedWaitSeconds` | int | Estimated time until match (2 * queuePosition) |
| `message` | string | Human-readable message |

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
  console.log(`Position: ${data.queuePosition} of ${data.queueSize}`);
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

**Success (200 OK) - In queue:**
```json
{
  "status": "IN_QUEUE",
  "queuePosition": 2,
  "queueSize": 5,
  "estimatedWaitSeconds": 4,
  "message": "You are in the queue"
}
```

**Success (200 OK) - Not in queue:**
```json
{
  "status": "NOT_IN_QUEUE",
  "queuePosition": 0,
  "queueSize": 5,
  "estimatedWaitSeconds": 0,
  "message": "You are not in the queue"
}
```

**Error Responses:**

**401 Unauthorized** - No/invalid token

### Examples

**cURL:**
```bash
curl -X GET http://localhost:8080/api/matchmaking/status \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript (Polling):**
```javascript
// Poll queue status every 2 seconds
const pollInterval = setInterval(async () => {
  const response = await fetch('http://localhost:8080/api/matchmaking/status', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  const data = await response.json();

  if (data.status === 'IN_QUEUE') {
    console.log(`Position: ${data.queuePosition}, Wait: ${data.estimatedWaitSeconds}s`);
    updateQueueUI(data);
  } else {
    console.log('Not in queue');
    clearInterval(pollInterval);
  }
}, 2000);

// Stop polling when match found
stompClient.subscribe('/user/queue/match-found', () => {
  clearInterval(pollInterval);
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
  "totalMatchesCreated": 1523,
  "currentQueueSize": 7,
  "lastRunTimestamp": "2025-10-17T14:35:42",
  "schedulerEnabled": true,
  "averageMatchTime": 3.2
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `totalMatchesCreated` | int | Total matches created since server start |
| `currentQueueSize` | int | Current number of players in queue |
| `lastRunTimestamp` | timestamp | Last time scheduler ran |
| `schedulerEnabled` | boolean | Is scheduler running |
| `averageMatchTime` | float | Average time to match (seconds) |

**Error Responses:**

**401 Unauthorized** - No/invalid token

### Examples

**cURL:**
```bash
curl -X GET http://localhost:8080/api/matchmaking/stats \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript (Admin Dashboard):**
```javascript
async function getMatchmakingStats() {
  const response = await fetch('http://localhost:8080/api/matchmaking/stats', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  const stats = await response.json();

  console.log('=== Matchmaking Statistics ===');
  console.log(`Total matches: ${stats.totalMatchesCreated}`);
  console.log(`Queue size: ${stats.currentQueueSize}`);
  console.log(`Avg match time: ${stats.averageMatchTime}s`);
  console.log(`Last run: ${stats.lastRunTimestamp}`);

  return stats;
}

// Refresh stats every 5 seconds
setInterval(getMatchmakingStats, 5000);
```

---

## GET `/health`

Health check endpoint for matchmaking system.

### Request

**Headers:**
```
Authorization: Bearer <access_token>
```

### Response

**Success (200 OK):**
```json
{
  "status": "UP",
  "currentQueueSize": 7,
  "totalMatchesCreated": 1523,
  "lastSchedulerRun": "2025-10-17T14:35:42"
}
```

**Error Responses:**

**401 Unauthorized** - No/invalid token

**503 Service Unavailable** - Scheduler not running:
```json
{
  "status": "DOWN",
  "message": "Matchmaking scheduler is not running"
}
```

### Examples

**cURL:**
```bash
curl -X GET http://localhost:8080/api/matchmaking/health \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**JavaScript (Monitoring):**
```javascript
async function checkMatchmakingHealth() {
  try {
    const response = await fetch('http://localhost:8080/api/matchmaking/health', {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });

    if (response.ok) {
      const health = await response.json();
      console.log('✓ Matchmaking is healthy');
      return true;
    } else {
      console.error('✗ Matchmaking is down');
      return false;
    }
  } catch (error) {
    console.error('✗ Cannot reach matchmaking service');
    return false;
  }
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
  "gameId": "c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "playerNumber": 1,
  "opponentId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "websocketTopic": "/topic/game/c7fd922a-831a-48d2-ab6e-a519628e5e0f"
}
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `gameId` | UUID | Game identifier |
| `playerNumber` | int | Your player number (1 or 2) |
| `opponentId` | UUID | Opponent's player ID |
| `websocketTopic` | string | Topic to subscribe for game updates |

**JavaScript Example:**
```javascript
// Subscribe to match notifications
stompClient.subscribe('/user/queue/match-found', (message) => {
  const data = JSON.parse(message.body);

  console.log('Match found!');
  console.log('Game ID:', data.gameId);
  console.log('You are Player', data.playerNumber);

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
    this.statusPollInterval = null;
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

    console.log(`Joined queue: Position ${data.queuePosition}/${data.queueSize}`);

    // Start polling status
    this.startStatusPolling();

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

    // Stop polling
    this.stopStatusPolling();

    console.log('Left queue');
    return data;
  }

  async getStatus() {
    const response = await fetch(`${this.baseURL}/status`, {
      headers: this.getHeaders()
    });

    if (!response.ok) throw new Error('Failed to get status');
    return await response.json();
  }

  startStatusPolling() {
    this.statusPollInterval = setInterval(async () => {
      try {
        const status = await this.getStatus();

        if (status.status === 'IN_QUEUE') {
          this.onStatusUpdate?.(status);
        } else {
          this.stopStatusPolling();
        }
      } catch (error) {
        console.error('Status poll error:', error);
      }
    }, 2000);
  }

  stopStatusPolling() {
    if (this.statusPollInterval) {
      clearInterval(this.statusPollInterval);
      this.statusPollInterval = null;
    }
  }

  subscribeToMatchNotification() {
    this.stomp.subscribe('/user/queue/match-found', (message) => {
      const data = JSON.parse(message.body);

      this.inQueue = false;
      this.stopStatusPolling();

      console.log('Match found!', data);
      this.onMatchFound?.(data);
    });
  }

  // Callbacks
  onStatusUpdate = null;  // (status) => {}
  onMatchFound = null;    // (matchData) => {}
}

// Usage
const matchmaking = new MatchmakingService(
  'http://localhost:8080/api/matchmaking',
  authService,
  stompClient
);

// Set callbacks
matchmaking.onStatusUpdate = (status) => {
  document.getElementById('queue-position').textContent = status.queuePosition;
  document.getElementById('queue-size').textContent = status.queueSize;
  document.getElementById('wait-time').textContent = `${status.estimatedWaitSeconds}s`;
};

matchmaking.onMatchFound = (matchData) => {
  alert(`Match found! You are Player ${matchData.playerNumber}`);
  window.location.href = `/game/${matchData.gameId}`;
};

// Join queue
await matchmaking.joinQueue();

// Leave queue
await matchmaking.leaveQueue();
```

---

## Matchmaking Algorithm

**Current**: FIFO (First In, First Out)

**Data Structure**: Redis Sorted Set
- Members: Player IDs
- Scores: Join timestamp (milliseconds)

**Matching Process**:
1. Scheduler runs every 2 seconds
2. Checks if queue size >= 2
3. Pops 2 oldest players (ZPOPMIN)
4. Creates PvP game
5. Sends WebSocket notifications
6. Publishes Kafka event

**Future Enhancement**: MMR-based matching
- Sort by skill rating instead of timestamp
- Match players with similar MMR (±200 points)
- Fallback to FIFO if no good match after 30 seconds

---

## Testing

**Test Scenario 1: Successful Match**
```javascript
// Player 1
await matchmaking.joinQueue();
// Position: 1, Size: 1

// Player 2 (different browser/user)
await matchmaking.joinQueue();
// Position: 2, Size: 2

// Wait 2-4 seconds...
// Both receive match notification
// Both redirected to game
```

**Test Scenario 2: Leave Queue**
```javascript
// Join
await matchmaking.joinQueue();

// Wait 5 seconds
await new Promise(resolve => setTimeout(resolve, 5000));

// Leave
await matchmaking.leaveQueue();

// Check status
const status = await matchmaking.getStatus();
console.log(status.status); // "NOT_IN_QUEUE"
```

**Test Scenario 3: Already in Queue**
```javascript
// Join
await matchmaking.joinQueue();

// Try to join again
const response = await matchmaking.joinQueue();
console.log(response.status); // "ALREADY_IN_QUEUE"
```
