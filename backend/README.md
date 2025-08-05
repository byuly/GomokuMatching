** Hybrid Kafka Approach **
```
Player Move → WebSocket → Direct Service Call → Immediate UI Update
                              ↓ (async)
                         Kafka Topic → Analytics/Logging
```
- Critical actions happen directly (moves, AI calculations)
- Non-critical actions go through Kafka (analytics, logging, history)
- Lower latency for gameplay
- Simpler development
