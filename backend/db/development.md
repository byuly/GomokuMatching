# 🐳 Gomoku Development Environment

A Docker-based development environment for a Gomoku game backend with PostgreSQL, Redis, Kafka, and pgAdmin.

## 📋 What's Included

- **PostgreSQL 17**: Primary database with custom schema
- **Redis 7**: In-memory cache for active game sessions and matchmaking queue
- **Apache Kafka**: Event streaming for game replay logging and analytics
- **pgAdmin 4**: Web-based database management interface
- **Auto-initialization**: Database schema and sample data setup
- **Pre-configured connections**: pgAdmin automatically connects to PostgreSQL

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Ports available: 5432 (PostgreSQL), 6379 (Redis), 9092 (Kafka), 5050 (pgAdmin)

### 1. Start the Full Environment

**Start all services (required for backend):**
```bash
# From project root
docker-compose up -d
```

This starts:
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

## 📁 File Structure

```
gomoku-backend/
├── docker-compose.yml          # Main orchestration file
├── db/
│   └── init/
│       └── 01_create_tables.sql # Database initialization
└── pgadmin/
    └── servers.json            # pgAdmin server pre-configuration
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

## 🌱 Spring Boot Integration

### Application Configuration (`application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gomoku_db
    username: gomoku_user
    password: gomoku_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Use our init script, don't auto-create
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_schema: gomoku

  # Redis configuration (required for game sessions & matchmaking)
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60000

  # Kafka configuration (required for game replay logging)
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: gomoku-app
      auto-offset-reset: latest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```


## 🛠 Development Commands

### Database Operations
```bash
# Connect to database directly
docker exec -it gomoku-postgres psql -U gomoku_user -d gomoku_db

# Inside psql:
\dt gomoku.*              # List tables
SELECT * FROM gomoku.player;  # Query data
\q                        # Exit
```

### Redis Operations
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

### Kafka Operations
```bash
# List topics
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --list --bootstrap-server localhost:9092

# Create topics (if not auto-created)
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --create --topic game-move-made --bootstrap-server localhost:9092
docker exec -it $(docker ps -qf "name=kafka") kafka-topics --create --topic match-created --bootstrap-server localhost:9092

# Consume messages (for debugging)
docker exec -it $(docker ps -qf "name=kafka") kafka-console-consumer --topic game-move-made --from-beginning --bootstrap-server localhost:9092
```

### Backup & Restore
```bash
# Create backup
docker exec gomoku-postgres pg_dump -U gomoku_user gomoku_db > backup.sql

# Restore from backup
docker exec -i gomoku-postgres psql -U gomoku_user -d gomoku_db < backup.sql
```

### Logs & Debugging
```bash
# View PostgreSQL logs
docker-compose logs -f postgres

# View pgAdmin logs
docker-compose logs -f pgadmin

# View all service logs
docker-compose logs
```

### Clean Reset
```bash
# Stop all services
docker-compose down

# Remove all data (⚠️ THIS DELETES EVERYTHING!)
docker volume rm gomoku-backend_postgres_data gomoku-backend_pgadmin_data

# Start fresh
docker-compose up -d postgres pgadmin
```

## 📊 How It Works

### Docker Networking
- Services communicate using Docker service names (e.g., pgAdmin connects to `postgres`, not `localhost`)
- External access uses `localhost` with mapped ports
- Internal Docker network: `gomoku-network`

### Data Persistence
- `postgres_data`: Stores all PostgreSQL database files
- `pgadmin_data`: Stores pgAdmin settings and server configurations
- Data survives container restarts and updates

### Initialization Process
1. PostgreSQL container starts and runs `01_create_tables.sql`
2. Database schema and sample data are created automatically
3. pgAdmin starts and loads pre-configured server from `servers.json`
4. Health checks ensure services start in correct order

### Hybrid Architecture
- **Redis**: Used for active game sessions (`int[][] board`) and matchmaking queue (ZADD/ZPOPMIN)
- **Kafka**: Logs all moves to `game-move-made` and `match-created` topics for game replay
- **PostgreSQL**: Final persistence of game history, player stats, and analytics
- All services are required and start together with `docker-compose up`

## 📝 Database Schema

### `db/init/init.sql`
The database initialization script is located at `db/init/init.sql` and runs automatically when PostgreSQL container starts.

**5 Core Tables (Minimal Schema):**
1. `player` - User profiles (linked to Firebase UID)
2. `player_stats` - Player statistics and MMR
3. `ai_opponent` - AI bot configurations
4. `game` - Completed game records
5. `game_move` - Individual moves for game replay (populated by Kafka consumers)

**Removed Tables (Handled by Redis/Kafka):**
- ~~`matchmaking_queue`~~ → Redis sorted set handles active queue
- ~~`game_session`~~ → Redis caches active WebSocket sessions
- ~~`kafka_event_log`~~ → Kafka IS the event log
- ~~`leaderboard`~~ → Not implemented yet

**Deferred Tables (Future Analytics):**
- `game_analytics` - Post-game analysis (add later)
- `ai_model_performance` - AI performance tracking (add later)
- `player_ai_matchup` - Player vs AI stats (add later)

> **Architecture**: PostgreSQL stores only final persistence. Active games live in Redis, events stream through Kafka.


## 🤝 Contributing

This is a development environment template. Customize the database schema, Spring Boot configuration, and Docker settings to match your specific Gomoku game requirements.

---

**Happy Coding! 🎮**