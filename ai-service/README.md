# Gomoku AI Microservice

Django REST API microservice providing AI opponent functionality for Gomoku (五目並べ) game.

## Features

- **Machine Learning**: PyTorch CNN with ResNet-style architecture
- **Game AI**: Minimax algorithm with alpha-beta pruning
- **Multiple Difficulty Levels**: Easy, Medium, Hard, Expert
- **REST API**: Clean Django REST Framework endpoints
- **Production Ready**: Docker support, comprehensive tests, split settings

## Architecture

```
ai-service/
├── ai/                      # Main Django app
│   ├── api/                 # REST API layer
│   │   ├── serializers.py   # Request/response serializers
│   │   ├── views.py         # API endpoints
│   │   └── urls.py          # URL routing
│   ├── services/            # Business logic
│   │   ├── ai_service.py    # Main AI orchestration
│   │   └── minimax.py       # Minimax algorithm
│   ├── ml/                  # Machine learning
│   │   └── network.py       # PyTorch neural network
│   ├── game/                # Game logic
│   │   ├── board.py         # Board representation
│   │   └── constants.py     # Game constants
│   └── tests/               # Test suites
│       ├── test_board.py
│       ├── test_minimax.py
│       └── test_api.py
├── gomoku_ai/              # Django project
│   ├── settings/           # Split settings (base/dev/prod)
│   └── urls.py
└── requirements.txt
```

## AI Implementation

### Neural Network
- **Architecture**: Convolutional Neural Network (CNN)
- **Inspiration**: AlphaGo Zero
- **Components**:
  - Input layer: 3 channels (current player, opponent, player indicator)
  - Residual blocks: 5 layers for feature extraction
  - Dual heads:
    - **Policy head**: Move probability distribution
    - **Value head**: Position evaluation (-1 to 1)

### Minimax Algorithm
- **Search depths**:
  - Easy: 1 ply (instant moves)
  - Medium: 2 ply
  - Hard: 3 ply + neural network
  - Expert: 4 ply + neural network
- **Optimizations**:
  - Alpha-beta pruning
  - Smart move ordering (nearby stones only)
  - Transposition detection

## API Endpoints

### Health Check
```bash
GET /api/health/
```

Response:
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "model_loaded": true,
  "device": "cpu"
}
```

### Get AI Move
```bash
POST /api/move/
```

Request:
```json
{
  "board_state": [[0,0,...], ...],  // 15x15 array (0=empty, 1=black, 2=white)
  "current_player": 2,               // 1=black, 2=white
  "difficulty": "medium"             // easy|medium|hard|expert
}
```

Response:
```json
{
  "row": 7,
  "col": 8,
  "difficulty": "medium"
}
```

### Validate Move
```bash
POST /api/validate/
```

Request:
```json
{
  "board_state": [[0,0,...], ...],
  "row": 7,
  "col": 7
}
```

Response:
```json
{
  "is_valid": true
}
```

### Check Game Over
```bash
POST /api/game-over/
```

Request:
```json
{
  "board_state": [[0,0,...], ...],
  "last_move_row": 7,
  "last_move_col": 7
}
```

Response:
```json
{
  "is_over": true,
  "winner": 1,      // 0=draw, 1=black, 2=white, null=ongoing
  "reason": "win"   // "win" | "draw" | null
}
```

### Evaluate Position
```bash
POST /api/evaluate/
```

Request:
```json
{
  "board_state": [[0,0,...], ...],
  "current_player": 1
}
```

Response:
```json
{
  "value": 0.65,
  "top_moves": [
    {"row": 7, "col": 7, "probability": 0.12},
    ...
  ]
}
```

## Setup

### Development

1. Create virtual environment:
```bash
python3 -m venv venv
source venv/bin/activate
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Run migrations:
```bash
python manage.py migrate --settings=gomoku_ai.settings.development
```

4. Run server:
```bash
python manage.py runserver 8001 --settings=gomoku_ai.settings.development
```

### Testing

Run all tests:
```bash
python manage.py test --settings=gomoku_ai.settings.development
```

Run specific test suite:
```bash
python manage.py test ai.tests.test_board --settings=gomoku_ai.settings.development
python manage.py test ai.tests.test_minimax --settings=gomoku_ai.settings.development
python manage.py test ai.tests.test_api --settings=gomoku_ai.settings.development
```

### Docker

Build and run:
```bash
docker build -t gomoku-ai .
docker run -p 8001:8000 gomoku-ai
```

Or use docker-compose from project root:
```bash
docker-compose up ai-service
```

## Configuration

### Environment Variables

Create `.env` file (see `.env.example`):

```bash
DJANGO_SETTINGS_MODULE=gomoku_ai.settings.production
DJANGO_SECRET_KEY=your-secret-key
ALLOWED_HOSTS=localhost,backend
CORS_ORIGINS=http://localhost:8080
```

### Settings

- **Development**: `gomoku_ai.settings.development`
  - SQLite database
  - Debug mode enabled
  - Verbose logging

- **Production**: `gomoku_ai.settings.production`
  - PostgreSQL database
  - Security headers enabled
  - Environment-based configuration

## Integration with Spring Boot

The Spring Boot backend communicates with this service via **HTTP/REST** using `AIServiceClient`:

```java
@Autowired
private AIServiceClient aiServiceClient;

// Get AI move via HTTP POST
AIMoveResponse move = aiServiceClient.getAIMove(
    boardState,
    currentPlayer,
    "medium"
);

// Check game over via HTTP POST
GameOverResponse result = aiServiceClient.checkGameOver(
    boardState,
    lastRow,
    lastCol
);
```

**Communication Protocol**: HTTP/1.1 with JSON payloads

Configure in `application.yml`:
```yaml
ai:
  service:
    url: http://localhost:8001  # HTTP endpoint
```

## Performance

- **Easy**: ~5ms per move
- **Medium**: ~20ms per move
- **Hard**: ~100ms per move (with NN)
- **Expert**: ~300ms per move (with NN)

## Future Enhancements

1. **Model Training**: Implement self-play training pipeline
2. **MCTS**: Add Monte Carlo Tree Search for expert level
3. **Opening Book**: Add opening move database
4. **Caching**: Redis cache for common positions
5. **Async Processing**: Celery for background move calculation
6. **Model Versioning**: Support multiple model versions

## License

Part of Gomoku Matching System project.
