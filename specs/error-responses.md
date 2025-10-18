# Error Response Reference

Complete reference for all error responses in the Gomoku Matching API.

---

## Error Response Format

All HTTP error responses follow this format:

```json
{
  "timestamp": "2025-10-17T14:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Username is already taken",
  "path": "/api/auth/register"
}
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | ISO 8601 | When the error occurred |
| `status` | int | HTTP status code |
| `error` | string | HTTP status text |
| `message` | string | Human-readable error message |
| `path` | string | API endpoint that returned the error |

---

## HTTP Status Codes

| Code | Name | When Used |
|------|------|-----------|
| 400 | Bad Request | Invalid request body, validation failed |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Valid token but no permission for resource |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists (e.g., username taken) |
| 500 | Internal Server Error | Unexpected server error |

---

## Authentication Errors

### POST `/api/auth/register`

**400 Bad Request - Validation Failed:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Username must be between 3 and 50 characters"
}
```

**Validation Messages:**
- `"Username is required"`
- `"Username must be between 3 and 50 characters"`
- `"Username can only contain letters, numbers, and underscores"`
- `"Email is required"`
- `"Email must be valid"`
- `"Password is required"`
- `"Password must be between 8 and 100 characters"`
- `"Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&)"`

**409 Conflict - Duplicate:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Username is already taken"
}
```

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Email is already registered"
}
```

### POST `/api/auth/login`

**400 Bad Request:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Username or email is required"
}
```

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password"
}
```

**403 Forbidden:**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Account is suspended"
}
```

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Account has been deleted"
}
```

---

## Game Errors

### POST `/api/game/create`

**400 Bad Request:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid game request. For PvP games, set player2Id. For PvAI games, set aiDifficulty."
}
```

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**404 Not Found:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Player2 not found"
}
```

### GET `/api/game/{gameId}`

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**403 Forbidden:**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You are not a participant in this game"
}
```

**404 Not Found:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Game not found"
}
```

### POST `/api/game/{gameId}/move`

**400 Bad Request:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Position out of bounds"
}
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Position already occupied"
}
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid move coordinates"
}
```

**403 Forbidden:**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Not your turn"
}
```

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You are not a participant in this game"
}
```

**404 Not Found:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Game not found"
}
```

**409 Conflict:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Game has already ended"
}
```

### POST `/api/game/{gameId}/forfeit`

**403 Forbidden:**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You are not a participant in this game"
}
```

**404 Not Found:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Game not found"
}
```

**409 Conflict:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Game has already ended"
}
```

---

## Matchmaking Errors

### POST `/api/matchmaking/queue`

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**Note**: Returns 200 even if already in queue (see `status` field in response).

### DELETE `/api/matchmaking/queue`

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**Note**: Returns 200 even if not in queue (see `status` field in response).

---

## Profile Errors

### GET `/api/profiles/me`

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**404 Not Found:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Player not found"
}
```

### PUT `/api/profiles/username`

**400 Bad Request:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Username must be between 3 and 50 characters"
}
```

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**409 Conflict:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Username is already taken"
}
```

---

## WebSocket Errors

WebSocket errors are sent to `/user/queue/errors` with this format:

```json
{
  "errorCode": "INVALID_MOVE",
  "message": "Position already occupied",
  "exceptionType": "InvalidMoveException"
}
```

**Error Codes:**

| Code | Message | When |
|------|---------|------|
| `GAME_NOT_FOUND` | "Game not found" | Game ID doesn't exist |
| `INVALID_MOVE` | "Position out of bounds" | Move row/col < 0 or > 14 |
| `INVALID_MOVE` | "Position already occupied" | board[row][col] != 0 |
| `UNAUTHORIZED` | "You are not a participant in this game" | Player not in game |
| `NOT_YOUR_TURN` | "Not your turn" | Wrong player trying to move |
| `GAME_COMPLETED` | "Game has already ended" | Trying to move in completed game |
| `ILLEGAL_STATE` | "Invalid game state" | Game in unexpected state |
| `INVALID_INPUT` | "Invalid request" | Malformed request |
| `INTERNAL_ERROR` | "An error occurred" | Unexpected server error |

---

## JWT Token Errors

### Missing Token

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

### Invalid Token (Malformed)

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid JWT token"
}
```

### Expired Token

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token has expired"
}
```

### Invalid Signature

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT signature validation failed"
}
```

---

## Error Handling in Frontend

### JavaScript Error Handler

```javascript
async function apiCall(url, options = {}) {
  try {
    const response = await fetch(url, options);

    if (!response.ok) {
      const error = await response.json();

      // Handle specific status codes
      switch (response.status) {
        case 400:
          throw new ValidationError(error.message);
        case 401:
          handleUnauthorized(error);
          throw new AuthError(error.message);
        case 403:
          throw new ForbiddenError(error.message);
        case 404:
          throw new NotFoundError(error.message);
        case 409:
          throw new ConflictError(error.message);
        case 500:
          throw new ServerError(error.message);
        default:
          throw new APIError(error.message);
      }
    }

    return await response.json();
  } catch (error) {
    console.error('API call failed:', error);
    throw error;
  }
}

function handleUnauthorized(error) {
  // Token expired or invalid
  console.error('Unauthorized:', error.message);

  // Clear tokens
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');

  // Redirect to login
  window.location.href = '/login';
}

// Custom error classes
class ValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ValidationError';
  }
}

class AuthError extends Error {
  constructor(message) {
    super(message);
    this.name = 'AuthError';
  }
}

class ForbiddenError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ForbiddenError';
  }
}

class NotFoundError extends Error {
  constructor(message) {
    super(message);
    this.name = 'NotFoundError';
  }
}

class ConflictError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ConflictError';
  }
}

class ServerError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ServerError';
  }
}

class APIError extends Error {
  constructor(message) {
    super(message);
    this.name = 'APIError';
  }
}
```

### Usage Example

```javascript
try {
  // Register user
  const response = await apiCall('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'player1',
      email: 'player1@example.com',
      password: 'Password123@'
    })
  });

  console.log('Registration successful:', response);

} catch (error) {
  if (error instanceof ValidationError) {
    showValidationError(error.message);
  } else if (error instanceof ConflictError) {
    showError('Username or email already exists');
  } else if (error instanceof ServerError) {
    showError('Server error. Please try again later.');
  } else {
    showError('An unexpected error occurred');
  }
}
```

### React Error Boundary

```javascript
import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-container">
          <h1>Something went wrong</h1>
          <p>{this.state.error?.message}</p>
          <button onClick={() => window.location.reload()}>
            Reload Page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
```

---

## User-Friendly Error Messages

Map technical errors to user-friendly messages:

```javascript
const ERROR_MESSAGES = {
  // Authentication
  'Invalid username or password': 'The username or password you entered is incorrect.',
  'Username is already taken': 'This username is already in use. Please choose another.',
  'Email is already registered': 'An account with this email already exists.',
  'Account is suspended': 'Your account has been suspended. Please contact support.',

  // Game
  'Position out of bounds': 'Invalid position. Please click within the board.',
  'Position already occupied': 'This position already has a stone. Choose an empty position.',
  'Not your turn': "Please wait for your opponent's move.",
  'Game not found': 'This game no longer exists.',
  'Game has already ended': 'This game has ended. Start a new game to play again.',

  // Matchmaking
  'You are already in the queue': 'You are already waiting for a match.',
  'You are not in the queue': 'You are not currently in the matchmaking queue.',

  // Token
  'JWT token has expired': 'Your session has expired. Please log in again.',
  'Invalid JWT token': 'Authentication failed. Please log in again.',
  'Full authentication is required': 'Please log in to access this feature.',

  // Generic
  'An error occurred': 'Something went wrong. Please try again.',
};

function getUserFriendlyMessage(technicalMessage) {
  return ERROR_MESSAGES[technicalMessage] || technicalMessage;
}

// Usage
try {
  await makeMove(gameId, 7, 7);
} catch (error) {
  const friendlyMessage = getUserFriendlyMessage(error.message);
  alert(friendlyMessage);
}
```

---

## Debugging Tips

### Enable Detailed Logging

```javascript
// In development
const DEBUG = process.env.NODE_ENV === 'development';

async function apiCall(url, options = {}) {
  if (DEBUG) {
    console.log('→ Request:', url, options);
  }

  try {
    const response = await fetch(url, options);

    if (DEBUG) {
      console.log('← Response:', response.status, response.statusText);
    }

    if (!response.ok) {
      const error = await response.json();

      if (DEBUG) {
        console.error('✗ Error:', error);
      }

      throw error;
    }

    const data = await response.json();

    if (DEBUG) {
      console.log('✓ Data:', data);
    }

    return data;
  } catch (error) {
    if (DEBUG) {
      console.error('✗ Exception:', error);
    }
    throw error;
  }
}
```

### Test Error Scenarios

```bash
# Missing token
curl -X GET http://localhost:8080/api/profiles/me
# Expected: 401 Unauthorized

# Invalid token
curl -X GET http://localhost:8080/api/profiles/me \
  -H "Authorization: Bearer invalid_token"
# Expected: 401 Unauthorized

# Expired token
curl -X GET http://localhost:8080/api/profiles/me \
  -H "Authorization: Bearer eyJhbGci..."
# Expected: 401 "JWT token has expired"

# Invalid move
curl -X POST http://localhost:8080/api/game/{gameId}/move \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"row": 20, "col": 7}'
# Expected: 400 "Position out of bounds"

# Duplicate username
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "existing_user",
    "email": "new@example.com",
    "password": "Password123@"
  }'
# Expected: 409 "Username is already taken"
```

---

## Error Logging & Monitoring

### Log Errors to Server

```javascript
async function logError(error, context) {
  try {
    await fetch('http://localhost:8080/api/errors/log', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: error.message,
        stack: error.stack,
        context,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href
      })
    });
  } catch (e) {
    console.error('Failed to log error:', e);
  }
}

// Usage
try {
  await makeMove(gameId, 7, 7);
} catch (error) {
  logError(error, {
    action: 'makeMove',
    gameId,
    userId: auth.getUserId()
  });
  throw error;
}
```

### Integrate with Error Tracking

```javascript
// Sentry example
import * as Sentry from '@sentry/browser';

Sentry.init({
  dsn: 'YOUR_SENTRY_DSN',
  environment: process.env.NODE_ENV
});

// Catch and report errors
try {
  await apiCall(url, options);
} catch (error) {
  Sentry.captureException(error, {
    tags: {
      api_endpoint: url,
      http_status: error.status
    },
    user: {
      id: auth.getUserId(),
      username: auth.getUsername()
    }
  });

  throw error;
}
```
