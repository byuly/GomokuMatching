# 🐳 Gomoku Development Environment

A Docker-based development environment for a Gomoku game backend featuring PostgreSQL, pgAdmin, and optional Kafka messaging.

## 📋 What's Included

- **PostgreSQL 17**: Primary database with custom schema
- **pgAdmin 4**: Web-based database management interface
- **Apache Kafka**: Message broker for real-time game events (optional)
- **Auto-initialization**: Database schema and sample data setup
- **Pre-configured connections**: pgAdmin automatically connects to PostgreSQL

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Port 5432 (PostgreSQL), 5050 (pgAdmin), and 9092 (Kafka) available

### 1. Start the Environment

**Option A: Database Only (Recommended for beginners)**
```bash
docker-compose up -d postgres pgadmin
```

**Option B: Full Stack with Kafka**
```bash
docker-compose --profile kafka up -d
```

### 2. Verify Everything is Running
```bash
# Check container status
docker-compose ps

# Should show postgres and pgadmin as "running"
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
| **pgAdmin** | http://localhost:5050 | Email: `admin@gomoku.dev`<br>Password: `admin123` |
| **Kafka** | `localhost:9092` | No authentication required |

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
    
  # Kafka configuration (optional)
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: gomoku-app
      auto-offset-reset: latest
```


## 🛠 Development Commands

### Database Operations
```bash
# Connect to database directly
docker exec -it gomoku-postgres psql -U gomoku_user -d gomoku_db

# Inside psql:
\dt gomoku.*              # List tables
SELECT * FROM gomoku.players;  # Query data
\q                        # Exit
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

### Optional Kafka Profile
- Kafka only starts when explicitly requested: `docker-compose --profile kafka up`
- Configured for single-node setup (perfect for development)
- Ready for real-time game events and messaging

## 📝 File Contents

We need to update this when we make changes to our models.
### `db/init/01_create_tables.sql`
*You'll need to create this file with your specific database schema for the Gomoku game.*

## 🤝 Contributing

This is a development environment template. Customize the database schema, Spring Boot configuration, and Docker settings to match your specific Gomoku game requirements.

---

**Happy Coding! 🎮**