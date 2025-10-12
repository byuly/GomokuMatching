# ai service

gomoku ai move calculation.

## stack

- flask: http endpoint
- python: ai logic

## api

### post /api/ai/move

calculates the optimal move.

**request:**
```json
{
  "board": [[0, 0, ...], ...],
  "player": 2,
  "difficulty": "medium"
}
```

**response:**
```json
{
  "row": 7,
  "col": 8
}
```

## running

1. install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

2. run app:
   ```bash
   python app.py
   ```
   the service will be available at `http://localhost:8001`.