# Authentication API

Base URL: `http://localhost:8080/api/auth`

All authentication endpoints are **public** (no JWT token required).

---

## Table of Contents

1. [Register User](#post-register)
2. [Login User](#post-login)
3. [Refresh Token](#post-refresh-todo)
4. [Logout](#post-logout-todo)

---

## POST `/register`

Register a new user account.

### Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "username": "player_one",
  "email": "player1@example.com",
  "password": "Password123@"
}
```

**Field Validation:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `username` | string | Yes | 3-50 characters, alphanumeric + underscores only (`^[a-zA-Z0-9_]+$`) |
| `email` | string | Yes | Valid email format, max 255 characters |
| `password` | string | Yes | 8-100 characters, must contain: 1 uppercase, 1 lowercase, 1 digit, 1 special char (@$!%*?&) |

### Response

**Success (201 Created):**
```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": "player_one",
  "email": "player1@example.com",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJh...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJh...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | Unique user identifier |
| `username` | string | Username |
| `email` | string | Email address |
| `accessToken` | string | JWT access token (valid for 15 minutes) |
| `refreshToken` | string | JWT refresh token (valid for 7 days) |
| `tokenType` | string | Always "Bearer" |
| `expiresIn` | number | Seconds until access token expires (900 = 15 minutes) |

**Error Responses:**

**400 Bad Request** - Validation failed:
```json
{
  "timestamp": "2025-10-17T14:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Password must contain at least one uppercase letter",
  "path": "/api/auth/register"
}
```

**409 Conflict** - Username or email already exists:
```json
{
  "timestamp": "2025-10-17T14:30:00.000+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Username is already taken",
  "path": "/api/auth/register"
}
```

### Examples

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "player_one",
    "email": "player1@example.com",
    "password": "Password123@"
  }'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('http://localhost:8080/api/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'player_one',
    email: 'player1@example.com',
    password: 'Password123@'
  })
});

const data = await response.json();

if (response.ok) {
  // Store tokens
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  localStorage.setItem('userId', data.userId);
  console.log('Registered successfully:', data.username);
} else {
  console.error('Registration failed:', data.message);
}
```

**Axios:**
```javascript
try {
  const { data } = await axios.post('http://localhost:8080/api/auth/register', {
    username: 'player_one',
    email: 'player1@example.com',
    password: 'Password123@'
  });

  // Store tokens
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  console.log('Registered:', data.username);
} catch (error) {
  console.error('Error:', error.response.data.message);
}
```

---

## POST `/login`

Authenticate user and receive JWT tokens.

### Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "usernameOrEmail": "player_one",
  "password": "Password123@"
}
```

**Field Validation:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `usernameOrEmail` | string | Yes | Username or email address |
| `password` | string | Yes | User's password |

### Response

**Success (200 OK):**
```json
{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": "player_one",
  "email": "player1@example.com",
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJh...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJh...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Error Responses:**

**400 Bad Request** - Missing fields:
```json
{
  "timestamp": "2025-10-17T14:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Username or email is required",
  "path": "/api/auth/login"
}
```

**401 Unauthorized** - Invalid credentials:
```json
{
  "timestamp": "2025-10-17T14:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/api/auth/login"
}
```

**403 Forbidden** - Account suspended:
```json
{
  "timestamp": "2025-10-17T14:30:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Account is suspended",
  "path": "/api/auth/login"
}
```

### Examples

**cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "player_one",
    "password": "Password123@"
  }'
```

**JavaScript (fetch):**
```javascript
const response = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    usernameOrEmail: 'player_one',
    password: 'Password123@'
  })
});

const data = await response.json();

if (response.ok) {
  // Store tokens
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  localStorage.setItem('userId', data.userId);
  localStorage.setItem('username', data.username);

  console.log('Logged in as:', data.username);

  // Redirect to game lobby
  window.location.href = '/lobby';
} else {
  alert(`Login failed: ${data.message}`);
}
```

**Axios:**
```javascript
try {
  const { data } = await axios.post('http://localhost:8080/api/auth/login', {
    usernameOrEmail: 'player_one',
    password: 'Password123@'
  });

  // Store tokens
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);

  console.log('Logged in:', data.username);
} catch (error) {
  if (error.response.status === 401) {
    console.error('Invalid credentials');
  } else {
    console.error('Error:', error.response.data.message);
  }
}
```

---

## POST `/refresh` (TODO)

**Status**: Not yet implemented

Refresh access token using refresh token.

### Planned Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJh..."
}
```

### Planned Response

**Success (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJh...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJh...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Note**: Will implement token rotation (new refresh token issued, old one blacklisted).

---

## POST `/logout` (TODO)

**Status**: Not yet implemented

Logout user and blacklist tokens.

### Planned Request

**Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJh..."
}
```

### Planned Response

**Success (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

**Note**: Will blacklist both access and refresh tokens in Redis with TTL.

---

## Security Considerations

### Password Requirements

Passwords must contain:
- ✅ At least 8 characters
- ✅ At least 1 uppercase letter (A-Z)
- ✅ At least 1 lowercase letter (a-z)
- ✅ At least 1 digit (0-9)
- ✅ At least 1 special character (@$!%*?&)

**Examples:**
- ✅ `Password123@`
- ✅ `SecureP@ss1`
- ❌ `password` (no uppercase, no digit, no special char)
- ❌ `Pass1@` (too short)
- ❌ `PASSWORD123@` (no lowercase)

### Password Storage

- Passwords are hashed using **BCrypt** with work factor 12
- Never stored in plain text
- Never returned in API responses
- Never logged

### Token Security

**Access Token:**
- Short-lived (15 minutes)
- Used for API authentication
- Stored in memory or localStorage (not cookies for now)

**Refresh Token:**
- Long-lived (7 days)
- Used only for refreshing access tokens
- Should be stored securely (HttpOnly cookie recommended)

**JWT Structure:**
```
Header.Payload.Signature

eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhMWIy...==

Decoded Payload:
{
  "sub": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",  // User ID
  "iat": 1697720400,  // Issued at
  "exp": 1697721300   // Expires at
}
```

### Rate Limiting (Planned)

**Register**: 5 attempts per IP per hour
**Login**: 10 attempts per IP per minute

---

## Frontend Integration

### Complete Auth Flow

```javascript
class AuthService {
  constructor() {
    this.baseURL = 'http://localhost:8080/api/auth';
  }

  async register(username, email, password) {
    try {
      const response = await fetch(`${this.baseURL}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message);
      }

      const data = await response.json();
      this.saveTokens(data);
      return data;
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  }

  async login(usernameOrEmail, password) {
    try {
      const response = await fetch(`${this.baseURL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ usernameOrEmail, password })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message);
      }

      const data = await response.json();
      this.saveTokens(data);
      return data;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }

  saveTokens(data) {
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('userId', data.userId);
    localStorage.setItem('username', data.username);
    localStorage.setItem('tokenExpiry', Date.now() + data.expiresIn * 1000);
  }

  getAccessToken() {
    return localStorage.getItem('accessToken');
  }

  isTokenExpired() {
    const expiry = localStorage.getItem('tokenExpiry');
    return expiry && Date.now() > parseInt(expiry);
  }

  logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('tokenExpiry');
  }

  isAuthenticated() {
    return !!this.getAccessToken() && !this.isTokenExpired();
  }

  // Helper for authenticated API calls
  getAuthHeaders() {
    return {
      'Authorization': `Bearer ${this.getAccessToken()}`,
      'Content-Type': 'application/json'
    };
  }
}

// Usage
const auth = new AuthService();

// Register
await auth.register('player1', 'player1@example.com', 'Password123@');

// Login
await auth.login('player1', 'Password123@');

// Check if authenticated
if (auth.isAuthenticated()) {
  console.log('User is logged in');
}

// Make authenticated request
const headers = auth.getAuthHeaders();
const response = await fetch('http://localhost:8080/api/profiles/me', { headers });
```

---

## Testing

### Valid Test Accounts

For development/testing:

```json
{
  "username": "test_player",
  "email": "test@example.com",
  "password": "TestPassword123@"
}
```

### Postman Collection

**Register Request:**
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "{{USERNAME}}",
  "email": "{{EMAIL}}",
  "password": "{{PASSWORD}}"
}
```

**Set Token After Login:**
```javascript
// In Postman Tests tab
if (pm.response.code === 200) {
  const data = pm.response.json();
  pm.environment.set("ACCESS_TOKEN", data.accessToken);
  pm.environment.set("USER_ID", data.userId);
}
```
