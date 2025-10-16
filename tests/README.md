# WebSocket Gameplay Tests

## test-websocket.html

Interactive WebSocket test for two-player gameplay.

### How to run:
```bash
open tests/test-websocket.html
```

### Steps:
1. Click "1. Create Game"
2. Click "2. Connect Both Players"
3. Click "3. Auto-Play (P2 Wins)"

### What it tests:
- WebSocket connection via STOMP
- Real-time move broadcasting
- Kafka event streaming
- Database persistence

---

## Refresh Tokens

Tokens expire after 24 hours. To refresh them:

```bash
chmod +x tests/refresh-tokens.sh
./tests/refresh-tokens.sh
```

Press `y` when asked to automatically update the HTML file with fresh tokens.
