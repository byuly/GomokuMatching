## development infrastructure

- ports: 5432 (PostgreSQL), 6379 (Redis), 9092 (Kafka), 5050 (pgAdmin)

### 1. Start the Full Environment

**Start all services (required for backend):**
```bash
# From project root
docker-compose up -d
```
starts:
- PostgreSQL (database)
- Redis (game session cache + matchmaking queue)
- Kafka + Zookeeper (event logging)
- pgAdmin (database UI)

### 2. Verify Everything is Running
```bash
# Check container status
docker-compose ps

# Should show: postgres, redis, kafka, zookeeper, pgadmin as "running"

# Test Redis connection
docker exec -it $(docker ps -qf "name=redis") redis-cli ping
# Should return: PONG
```


## 🔗 Service Access

| Service | URL/Connection | Credentials |
|---------|---------------|-------------|
| **PostgreSQL** | `localhost:5432` | User: `gomoku_user`<br>Password: `gomoku_password`<br>Database: `gomoku_db` |
| **Redis** | `localhost:6379` | No password (dev environment) |
| **Kafka** | `localhost:9092` | No authentication required |
| **pgAdmin** | http://localhost:5050 | Email: `admin@gomoku.dev`<br>Password: `admin123` |

## 🔧 Using pgAdmin

1. Navigate to http://localhost:5050
2. Login with `admin@gomoku.dev` / `admin123`
3. The "Gomoku Development Server" should already be configured
4. Click on the server and enter the PostgreSQL password: `gomoku_password`
5. Browse: `gomoku_db` → `Schemas` → `gomoku` → `Tables`

> **Note**: The `servers.json` file automatically configures this connection, so you don't need to manually add the server.

## commands i always forget

### db operations
```bash
# Connect to database directly
docker exec -it gomoku-postgres psql -U gomoku_user -d gomoku_db

# Inside psql:
\dt gomoku.*              # List tables
SELECT * FROM gomoku.player;  # Query data
\q                        # Exit
```

### redis operations
```bash
# Connect to Redis CLI
docker exec -it $(docker ps -qf "name=redis") redis-cli

# Inside redis-cli:
KEYS matchmaking:queue    # View matchmaking queue
KEYS game:*              # View active game sessions
ZRANGE matchmaking:queue 0 -1 WITHSCORES  # View queue with timestamps
GET game:session:{gameId}  # View specific game session
FLUSHDB                  # Clear all keys (⚠️ development only!)
```

### kafka operations
```bash
# List topics
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --list --bootstrap-server localhost:9092

# Create topics (if not auto-created)
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --create --topic game-move-made --bootstrap-server localhost:9092
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --create --topic match-created --bootstrap-server localhost:9092

# Consume messages (for debugging)
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer --topic game-move-made --from-beginning --bootstrap-server localhost:9092
```

### backup & restore db
```bash
# Create backup
docker exec gomoku-postgres pg_dump -U gomoku_user gomoku_db > backup.sql

# Restore from backup
docker exec -i gomoku-postgres psql -U gomoku_user -d gomoku_db < backup.sql
```

### view logs 
```bash
# View PostgreSQL logs
docker-compose logs -f postgres

# View pgAdmin logs
docker-compose logs -f pgadmin

# View all service logs
docker-compose logs
```

### clean reset
```bash
# Stop all services
docker-compose down

# Remove all data (⚠️ THIS DELETES EVERYTHING!)
docker volume rm gomoku-backend_postgres_data gomoku-backend_pgadmin_data

# Start fresh
docker-compose up -d postgres pgadmin
```
