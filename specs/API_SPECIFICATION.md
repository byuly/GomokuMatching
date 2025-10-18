# Gomoku Matching - Complete API Specification

**Base URL**: `http://localhost:8080`
**API Version**: 1.0
**Last Updated**: 2025-10-17

---

## Table of Contents

1. [Authentication](#authentication)
2. [Game Management](#game-management)
3. [Matchmaking](#matchmaking)
4. [Player Profiles](#player-profiles)
5. [WebSocket Communication](#websocket-communication)
6. [Error Handling](#error-handling)

---

## Quick Reference

| Category | Endpoint | Method | Auth Required |
|----------|----------|--------|---------------|
| **Authentication** | `/api/auth/register` | POST | No |
| | `/api/auth/login` | POST | No |
| **Game** | `/api/game/create` | POST | Yes |
| | `/api/game/{gameId}` | GET | Yes |
| | `/api/game/{gameId}/move` | POST | Yes (PvAI only) |
| | `/api/game/{gameId}/forfeit` | POST | Yes |
| | `/api/game/{gameId}/moves` | GET | Yes |
| **Matchmaking** | `/api/matchmaking/queue` | POST | Yes |
| | `/api/matchmaking/queue` | DELETE | Yes |
| | `/api/matchmaking/status` | GET | Yes |
| | `/api/matchmaking/stats` | GET | Yes |
| | `/api/matchmaking/health` | GET | Yes |
| **Profile** | `/api/profiles/me` | GET | Yes |
| | `/api/profiles/username` | PUT | Yes |
| **WebSocket** | `/ws` | WS | Yes |
| | `/app/game/{gameId}/move` | SEND | Yes |
| | `/app/game/{gameId}/forfeit` | SEND | Yes |
| | `/topic/game/{gameId}` | SUBSCRIBE | Yes |
| | `/user/queue/match-found` | SUBSCRIBE | Yes |
| | `/user/queue/errors` | SUBSCRIBE | Yes |

---

## Authentication

All authenticated endpoints require a JWT token in the Authorization header:

```
Authorization: Bearer <access_token>
```

### Token Expiration
- **Access Token**: 15 minutes (900 seconds)
- **Refresh Token**: 7 days (604800 seconds)

---

## HTTP Status Codes

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful GET/PUT/DELETE request |
| 201 | Created | Successful POST creating new resource |
| 400 | Bad Request | Invalid request body or parameters |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Valid token but no permission |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists |
| 500 | Internal Server Error | Server error |

---

## Common Error Response Format

All endpoints return errors in this format:

```json
{
  "timestamp": "2025-10-17T14:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Username is already taken",
  "path": "/api/auth/register"
}
```

For detailed error codes and messages, see [error-responses.md](./error-responses.md).

---

## API Endpoints by Category

### Authentication
See [authentication-api.md](./authentication-api.md) for complete details.

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Authenticate user

### Game Management
See [game-api.md](./game-api.md) for complete details.

- `POST /api/game/create` - Create new game (PvP or PvAI)
- `GET /api/game/{gameId}` - Get game state
- `POST /api/game/{gameId}/move` - Make move (PvAI only, PvP uses WebSocket)
- `POST /api/game/{gameId}/forfeit` - Forfeit game
- `GET /api/game/{gameId}/moves` - Get move history for replay

### Matchmaking
See [matchmaking-api.md](./matchmaking-api.md) for complete details.

- `POST /api/matchmaking/queue` - Join matchmaking queue
- `DELETE /api/matchmaking/queue` - Leave matchmaking queue
- `GET /api/matchmaking/status` - Get queue status
- `GET /api/matchmaking/stats` - Get matchmaking statistics
- `GET /api/matchmaking/health` - Health check

### Player Profiles
See [game-api.md](./game-api.md#player-profiles) for complete details.

- `GET /api/profiles/me` - Get current user's profile
- `PUT /api/profiles/username` - Update username

### WebSocket Communication
See [websocket-api.md](./websocket-api.md) for complete details.

Real-time bidirectional communication for Player vs Player games.

---

## Data Types & Enums

### GameType
```
HUMAN_VS_HUMAN
HUMAN_VS_AI
```

### GameStatus
```
IN_PROGRESS
COMPLETED
ABANDONED
```

### WinnerType
```
NONE        // Game in progress
PLAYER1     // Player 1 won
PLAYER2     // Player 2 won
AI          // AI won
DRAW        // Board full, no winner
```

### AIDifficulty
```
EASY
MEDIUM
HARD
EXPERT
```

### MatchmakingStatus
```
JOINED          // Successfully joined queue
ALREADY_IN_QUEUE // Player was already in queue
LEFT            // Successfully left queue
NOT_IN_QUEUE    // Player is not in queue
```

---

## Board Representation

The game board is a 15x15 2D integer array:

```json
{
  "board": [
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0],
    ...
  ]
}
```

**Values:**
- `0` = Empty position
- `1` = Player 1 stone (BLACK, always goes first)
- `2` = Player 2 or AI stone (WHITE)

**Coordinates:**
- Row index: 0-14 (top to bottom)
- Column index: 0-14 (left to right)

---

## Rate Limiting

**Status**: Not yet implemented

**Planned limits** (per user per minute):
- Authentication: 10 requests
- Game moves: 60 requests
- Matchmaking: 20 requests
- Profile: 30 requests

---

## CORS Configuration

**Allowed Origins**: `*` (all origins, for development)

**Production**: Should be restricted to frontend domain only.

**Allowed Methods**: `GET, POST, PUT, DELETE, OPTIONS`

**Allowed Headers**: `Authorization, Content-Type`

---

## Testing the API

### Using cURL

**Register**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "player1",
    "email": "player1@example.com",
    "password": "Password123@"
  }'
```

**Login**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "player1",
    "password": "Password123@"
  }'
```

**Get Profile** (requires token):
```bash
curl -X GET http://localhost:8080/api/profiles/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Using Postman

1. Import the collection (if provided) or create requests manually
2. Set environment variable `BASE_URL` = `http://localhost:8080`
3. Set environment variable `ACCESS_TOKEN` after login
4. Use `{{BASE_URL}}` and `{{ACCESS_TOKEN}}` in requests

### Using Frontend (JavaScript)

See individual endpoint documentation for `fetch` and `axios` examples.

---

## Changelog

### Version 1.0 (2025-10-17)
- Initial API specification
- Authentication endpoints
- Game management endpoints
- Matchmaking system
- WebSocket communication
- Player profiles

---

## Additional Resources

- [Authentication API Details](./authentication-api.md)
- [Game API Details](./game-api.md)
- [Matchmaking API Details](./matchmaking-api.md)
- [WebSocket API Details](./websocket-api.md)
- [Error Response Reference](./error-responses.md)
- [Main README](../README.md)
- [Development Guide](../development_guide.md)
