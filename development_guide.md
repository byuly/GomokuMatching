# Development Guide

## Quick Access

| Tool | URL | Purpose |
|------|-----|---------|
| pgAdmin | http://localhost:5050 | PostgreSQL GUI |
| Redis Commander | http://localhost:8081 | Redis GUI |
| Backend API | http://localhost:8080 | Spring Boot REST API |
| AI Service | http://localhost:8001 | Django AI endpoints |

---

## PostgreSQL Access

### pgAdmin (GUI)

1. Open http://localhost:5050
2. Login:
   - Email: `admin@gomoku.com`
   - Password: `admin`

3. Add Server (first time):
   - Right-click "Servers" → "Register" → "Server"
   - General Tab:
     - Name: `Gomoku Database`
   - Connection Tab:
     - Host: `postgres` (not localhost)
     - Port: `5432`
     - Database: `gomoku_db`
     - Username: `gomoku_user`
     - Password: `gomoku_password`

4. Browse data:
   - Servers → Gomoku Database → Databases → gomoku_db → Schemas → gomoku → Tables
   - Right-click table → "View/Edit Data" → "All Rows"

### Command Line (psql)

```bash
# List tables
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c "\dt gomoku.*"

# View players
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c "SELECT * FROM gomoku.player;"

# Interactive shell
docker exec -it gomoku-postgres psql -U gomoku_user -d gomoku_db
```

### Useful Queries

```sql
-- All users
SELECT player_id, username, email, created_at, elo_rating
FROM gomoku.player
ORDER BY created_at DESC;

-- Completed games
SELECT game_id, game_type, winner_id, move_count, started_at, ended_at
FROM gomoku.game
WHERE status = 'COMPLETED'
ORDER BY ended_at DESC
LIMIT 10;

-- Player statistics
SELECT p.username, ps.total_games, ps.wins, ps.losses, ps.draws, ps.current_mmr, ps.peak_mmr
FROM gomoku.player p
JOIN gomoku.player_stats ps ON p.player_id = ps.player_id
ORDER BY ps.current_mmr DESC;

-- Game moves
SELECT game_id, game_type, winner_type, total_moves, jsonb_pretty(move_sequence) as moves
FROM gomoku.game
WHERE game_id = 'YOUR-GAME-UUID-HERE';
```

---

## Redis Access

### Redis Commander (GUI)

1. Open http://localhost:8081
2. Click `local` connection
3. Browse keys starting with `game:session:`

### Command Line (redis-cli)

```bash
# List all keys
docker exec gomoku-redis redis-cli KEYS "*"

# View game session
docker exec gomoku-redis redis-cli GET "game:session:YOUR-GAME-UUID"

# Get all game sessions
docker exec gomoku-redis redis-cli --scan --pattern "game:session:*"

# Count active games
docker exec gomoku-redis redis-cli KEYS "game:session:*" | wc -l

# Check TTL
docker exec gomoku-redis redis-cli TTL "game:session:YOUR-GAME-UUID"

# Delete game
docker exec gomoku-redis redis-cli DEL "game:session:YOUR-GAME-UUID"

# Clear all data (use carefully)
docker exec gomoku-redis redis-cli FLUSHALL

# Interactive shell
docker exec -it gomoku-redis redis-cli
```

### Redis Data Structure

Active games stored as:
```
Key: game:session:<UUID>
Value: JSON object
TTL: 2 hours (7200 seconds)
```

Example:
```json
{
  "gameId": "c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "gameType": "HUMAN_VS_HUMAN",
  "status": "IN_PROGRESS",
  "player1Id": "d5a8fce4-5b6e-4f68-896e-b80b7b92c786",
  "player2Id": "1bda7d03-481f-4c9e-8997-83a7192173cc",
  "board": [[0,0,0,...], ...],
  "currentPlayer": 1,
  "moveCount": 2,
  "startedAt": "2025-10-04T14:56:25",
  "lastActivity": "2025-10-04T14:57:06"
}
```

---

## Common Debugging

### Where is my game?

**Active (in progress):**
- Redis: `game:session:<gameId>`
- Expires after 2 hours

**Completed:**
- PostgreSQL: `gomoku.game` table

### User login issues

```sql
SELECT username, email, created_at
FROM gomoku.player
WHERE email = 'user@example.com';
```

### Game not found

```bash
# Check Redis
docker exec gomoku-redis redis-cli EXISTS "game:session:<gameId>"

# Check PostgreSQL
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c \
  "SELECT * FROM gomoku.game WHERE game_id = '<gameId>';"
```

---

## Docker Management

```bash
# View containers
docker ps

# Check logs
docker logs gomoku-backend
docker logs gomoku-postgres
docker logs gomoku-redis

# Restart services
docker-compose restart backend
docker-compose restart postgres

# Reset database (deletes all data)
docker-compose down -v
docker-compose up -d
```

---

## Database Schema

```
gomoku.player
├── player_id (UUID, PK)
├── username (unique)
├── email (unique)
├── password_hash
├── created_at
├── last_login
├── is_active
└── account_status (ACTIVE, SUSPENDED, BANNED)

gomoku.ai_opponent
├── ai_id (UUID, PK)
├── name
├── difficulty_level (EASY, MEDIUM, HARD, EXPERT)
├── model_version
├── model_file_path
├── win_rate_target
├── is_active
├── created_at
└── last_updated

gomoku.game
├── game_id (UUID, PK)
├── game_type (HUMAN_VS_HUMAN, HUMAN_VS_AI)
├── game_status (WAITING, IN_PROGRESS, COMPLETED, ABANDONED)
├── player1_id (FK → player)
├── player2_id (FK → player, nullable)
├── ai_opponent_id (FK → ai_opponent, nullable)
├── winner_type (NONE, PLAYER, AI, DRAW)
├── winner_id (FK → player, nullable)
├── total_moves
├── started_at
├── ended_at
├── created_at
├── final_board_state (jsonb)
├── move_sequence (jsonb)
└── game_duration_seconds

gomoku.game_move
├── move_id (UUID, PK)
├── game_id (FK → game)
├── move_number
├── player_type (HUMAN, AI)
├── player_id (FK → player, nullable)
├── ai_opponent_id (FK → ai_opponent, nullable)
├── board_x
├── board_y
├── stone_color (BLACK, WHITE)
├── move_timestamp
├── time_taken_ms
└── board_state_after_move (jsonb)

gomoku.player_stats
├── stats_id (UUID, PK)
├── player_id (FK → player, unique)
├── total_games
├── wins
├── losses
├── draws
├── current_mmr
├── peak_mmr
├── current_streak
├── longest_win_streak
└── last_updated
```

---

## Troubleshooting

**pgAdmin won't connect:**
- Use `postgres` as hostname (not `localhost`)
- Ensure containers on same network (`gomoku-net`)

**Redis Commander empty:**
- Redis may be empty (no active games)
- Create game via API first

**Can't access GUI tools:**
- Check: `docker ps | grep gomoku`
- Restart: `docker-compose restart pgadmin redis-commander`

**Port conflict:**
- Change port in docker-compose.yml
- Example: `"5051:80"` instead of `"5050:80"`
