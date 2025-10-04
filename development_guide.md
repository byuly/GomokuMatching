# Debugging Guide - PostgreSQL & Redis

## 🎯 Quick Access

| Tool | URL | Purpose |
|------|-----|---------|
| **pgAdmin** | http://localhost:5050 | PostgreSQL GUI |
| **Redis Commander** | http://localhost:8081 | Redis GUI |
| **Backend API** | http://localhost:8080 | Spring Boot REST API |
| **AI Service** | http://localhost:8001 | Django AI endpoints |

---

## 🐘 PostgreSQL Access

### Option 1: pgAdmin (GUI) ✨ Recommended

1. **Open**: http://localhost:5050
2. **Login**:
   - Email: `admin@gomoku.com`
   - Password: `admin`

3. **Add Server** (First time only):
   - Right-click "Servers" → "Register" → "Server"
   - **General Tab**:
     - Name: `Gomoku Database`
   - **Connection Tab**:
     - Host: `postgres` (not localhost!)
     - Port: `5432`
     - Database: `gomoku_db`
     - Username: `gomoku_user`
     - Password: `gomoku_password`
   - Click "Save"

4. **Browse Data**:
   - Servers → Gomoku Database → Databases → gomoku_db → Schemas → gomoku → Tables
   - Right-click any table → "View/Edit Data" → "All Rows"

### Option 2: Command Line (psql)

```bash
# List all tables
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c "\dt gomoku.*"

# View players
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c "SELECT * FROM gomoku.player;"

# View AI opponents
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c "SELECT name, difficulty, rating FROM gomoku.ai_opponent;"

# Count completed games
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c "SELECT COUNT(*) FROM gomoku.game;"

# Interactive psql shell
docker exec -it gomoku-postgres psql -U gomoku_user -d gomoku_db
# Then run queries:
# \dt gomoku.*          -- List tables
# \d gomoku.player      -- Describe player table
# SELECT * FROM gomoku.player LIMIT 5;
# \q                    -- Quit
```

### Useful Queries

```sql
-- See all users
SELECT player_id, username, email, created_at, elo_rating
FROM gomoku.player
ORDER BY created_at DESC;

-- See completed games
SELECT
    game_id,
    game_type,
    winner_id,
    move_count,
    started_at,
    ended_at
FROM gomoku.game
WHERE status = 'COMPLETED'
ORDER BY ended_at DESC
LIMIT 10;

-- Player statistics
SELECT
    p.username,
    ps.total_games,
    ps.wins,
    ps.losses,
    ps.draws,
    ps.elo_rating
FROM gomoku.player p
JOIN gomoku.player_stats ps ON p.player_id = ps.player_id
ORDER BY ps.elo_rating DESC;

-- Game move sequence (JSONB format)
SELECT
    game_id,
    game_type,
    winner_type,
    total_moves,
    jsonb_pretty(move_sequence) as moves
FROM gomoku.game
WHERE game_id = 'YOUR-GAME-UUID-HERE';

-- Replay game move by move
SELECT
    game_id,
    elem->0 as row,
    elem->1 as col,
    elem->2 as player,
    ordinality as move_number
FROM gomoku.game,
     jsonb_array_elements(move_sequence) WITH ORDINALITY AS elem
WHERE game_id = 'YOUR-GAME-UUID-HERE';
```

---

## 🔴 Redis Access

### Option 1: Redis Commander (GUI) ✨ Recommended

1. **Open**: http://localhost:8081
2. **No login required** - auto-connected!
3. **Browse**:
   - Click on `local` connection
   - See all keys starting with `game:session:`
   - Click any key to view JSON data

### Option 2: Command Line (redis-cli)

```bash
# List all keys
docker exec gomoku-redis redis-cli KEYS "*"

# View specific game session
docker exec gomoku-redis redis-cli GET "game:session:YOUR-GAME-UUID"

# Get all game sessions
docker exec gomoku-redis redis-cli --scan --pattern "game:session:*"

# Count active games
docker exec gomoku-redis redis-cli KEYS "game:session:*" | wc -l

# Check TTL (time to live) of a key
docker exec gomoku-redis redis-cli TTL "game:session:YOUR-GAME-UUID"

# Delete specific game
docker exec gomoku-redis redis-cli DEL "game:session:YOUR-GAME-UUID"

# Clear ALL Redis data (⚠️ Use carefully!)
docker exec gomoku-redis redis-cli FLUSHALL

# Interactive redis-cli shell
docker exec -it gomoku-redis redis-cli
# Then run commands:
# KEYS *
# GET "game:session:c7fd922a-831a-48d2-ab6e-a519628e5e0f"
# INFO
# exit
```

### Understanding Redis Data

Active games are stored as:
```
Key: game:session:<UUID>
Value: JSON object with game state
TTL: 2 hours (7200 seconds)
```

Example game session structure:
```json
{
  "gameId": "c7fd922a-831a-48d2-ab6e-a519628e5e0f",
  "gameType": "HUMAN_VS_HUMAN",
  "status": "IN_PROGRESS",
  "player1Id": "d5a8fce4-5b6e-4f68-896e-b80b7b92c786",
  "player2Id": "1bda7d03-481f-4c9e-8997-83a7192173cc",
  "board": [[0,0,0,...], ...],  // 15x15 array
  "currentPlayer": 1,
  "moveCount": 2,
  "startedAt": "2025-10-04T14:56:25",
  "lastActivity": "2025-10-04T14:57:06"
}
```

---

## 🔍 Common Debugging Scenarios

### "Where is my game stored?"

**Active game** (in progress):
- Check Redis: `game:session:<gameId>`
- Will disappear after 2 hours of inactivity

**Completed game**:
- Check PostgreSQL: `gomoku.game` table
- Permanent storage

### "User can't login"

```sql
-- Check if user exists
SELECT username, email, created_at
FROM gomoku.player
WHERE email = 'user@example.com';

-- Check password hash (should start with $2a$)
SELECT username, password_hash
FROM gomoku.player
WHERE email = 'user@example.com';
```

### "Game not found error"

```bash
# Check if game exists in Redis
docker exec gomoku-redis redis-cli EXISTS "game:session:<gameId>"

# If returns 0, game expired or completed
# Check PostgreSQL for completed games
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c \
  "SELECT * FROM gomoku.game WHERE game_id = '<gameId>';"
```

### "How many active games right now?"

```bash
# Redis (active)
docker exec gomoku-redis redis-cli KEYS "game:session:*" | wc -l

# PostgreSQL (all time)
docker exec gomoku-postgres psql -U gomoku_user -d gomoku_db -c \
  "SELECT COUNT(*) FROM gomoku.game;"
```

---

## 🛠️ Docker Management Commands

```bash
# View all running containers
docker ps

# Check logs
docker logs gomoku-backend
docker logs gomoku-postgres
docker logs gomoku-redis

# Restart services
docker-compose restart backend
docker-compose restart postgres
docker-compose restart redis

# Stop GUI tools (save resources)
docker stop gomoku-pgadmin gomoku-redis-commander

# Start GUI tools
docker-compose up -d pgadmin redis-commander

# Reset database (⚠️ DELETES ALL DATA)
docker-compose down -v  # Deletes volumes
docker-compose up -d    # Recreates fresh database
```

---

## 📊 Database Schema

```
gomoku.player
├── player_id (UUID, PK)
├── username
├── email
├── password_hash
├── elo_rating
└── created_at

gomoku.ai_opponent
├── ai_id (UUID, PK)
├── name
├── difficulty (EASY, MEDIUM, HARD, EXPERT)
├── rating
└── algorithm_type (MINIMAX, MCTS)

gomoku.game
├── game_id (UUID, PK)
├── game_type (HUMAN_VS_HUMAN, HUMAN_VS_AI)
├── player1_id (FK → player)
├── player2_id (FK → player, nullable)
├── ai_opponent_id (FK → ai_opponent, nullable)
├── winner_id (FK → player, nullable)
├── status (WAITING, IN_PROGRESS, COMPLETED, ABANDONED)
├── move_count
└── timestamps

gomoku.game_move
├── move_id (UUID, PK)
├── game_id (FK → game)
├── player_id (FK → player)
├── move_number
├── row_pos
├── col_pos
└── timestamp

gomoku.player_stats
├── stat_id (UUID, PK)
├── player_id (FK → player)
├── total_games
├── wins
├── losses
├── draws
└── elo_rating
```

---

## 🎮 Testing Workflow

1. **Register user** → Check `gomoku.player` in pgAdmin
2. **Create game** → Check Redis Commander for `game:session:<id>`
3. **Make moves** → Watch board update in Redis
4. **Complete game** → Check `gomoku.game` in PostgreSQL
5. **View stats** → Check `gomoku.player_stats`

---

## 💡 Tips

- **pgAdmin**: Great for complex queries and schema visualization
- **Redis Commander**: Real-time view of active game sessions
- **Use TTL wisely**: Redis auto-expires games after 2 hours
- **Check both**: Active games in Redis, completed games in PostgreSQL
- **Connection issues**: Use container name (`postgres`, `redis`) not `localhost` when connecting from other containers

---

## 🚨 Troubleshooting

**pgAdmin won't connect to postgres:**
- Use `postgres` as hostname (not `localhost`)
- Make sure containers are on same network (`gomoku-net`)

**Redis Commander shows nothing:**
- Redis might be empty (no active games)
- Create a game via API first

**Can't access GUI tools:**
- Check containers are running: `docker ps | grep gomoku`
- Restart: `docker-compose restart pgadmin redis-commander`

**Port already in use:**
- Change port in docker-compose.yml
- Example: `"5051:80"` instead of `"5050:80"`
