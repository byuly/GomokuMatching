/**
 * WebSocket Service - STOMP over SockJS for real-time game updates
 */

import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type {
  WebSocketGameUpdate,
  WebSocketMoveMessage,
  WebSocketError,
  MatchFoundNotification,
} from '@/types';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

type MessageHandler<T> = (message: T) => void;
type ErrorHandler = (error: WebSocketError) => void;

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private matchmakingCallback: MessageHandler<MatchFoundNotification> | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private isConnecting = false;

  constructor() {
    this.client = null;
  }

  /**
   * Connect to WebSocket server with JWT authentication
   */
  public connect(accessToken: string): Promise<void> {
    if (this.client?.connected) {
      console.log('WebSocket already connected');
      return Promise.resolve();
    }

    if (this.isConnecting) {
      console.log('WebSocket connection already in progress');
      return Promise.resolve();
    }

    this.isConnecting = true;

    return new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
        connectHeaders: {
          Authorization: `Bearer ${accessToken}`,
        },
        debug: (str) => {
          if (import.meta.env.DEV) {
            console.log('[STOMP Debug]', str);
          }
        },
        reconnectDelay: this.reconnectDelay,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected');
          this.reconnectAttempts = 0;
          this.isConnecting = false;
          resolve();
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame);
          this.isConnecting = false;
          reject(new Error(frame.headers['message'] || 'WebSocket connection failed'));
        },
        onWebSocketError: (error) => {
          console.error('WebSocket error:', error);
          this.isConnecting = false;
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected');
          this.isConnecting = false;
          this.handleReconnect(accessToken);
        },
      });

      this.client.activate();
    });
  }

  /**
   * Handle automatic reconnection
   */
  private handleReconnect(accessToken: string): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Reconnecting... Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);

      setTimeout(() => {
        this.connect(accessToken).catch((error) => {
          console.error('Reconnection failed:', error);
        });
      }, this.reconnectDelay * this.reconnectAttempts);
    } else {
      console.error('Max reconnection attempts reached. Please refresh the page.');
    }
  }

  /**
   * Disconnect from WebSocket server
   */
  public disconnect(): void {
    if (this.client) {
      this.subscriptions.forEach((sub) => sub.unsubscribe());
      this.subscriptions.clear();
      this.client.deactivate();
      this.client = null;
      console.log('WebSocket disconnected manually');
    }
  }

  /**
   * Subscribe to game updates for a specific game
   */
  public subscribeToGame(
    gameId: number,
    onUpdate: MessageHandler<WebSocketGameUpdate>,
    onError?: ErrorHandler
  ): void {
    if (!this.client?.connected) {
      console.error('WebSocket not connected. Cannot subscribe to game updates.');
      return;
    }

    const destination = `/topic/game/${gameId}`;

    if (this.subscriptions.has(destination)) {
      console.log(`Already subscribed to ${destination}`);
      return;
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const update: WebSocketGameUpdate = JSON.parse(message.body);
        onUpdate(update);
      } catch (error) {
        console.error('Error parsing game update:', error);
        if (onError) {
          onError({
            gameId,
            code: 'PARSE_ERROR',
            message: 'Failed to parse game update',
            timestamp: new Date().toISOString(),
          });
        }
      }
    });

    this.subscriptions.set(destination, subscription);
    console.log(`Subscribed to game ${gameId}`);
  }

  /**
   * Subscribe to matchmaking notifications
   * Updates callback if already subscribed
   */
  public subscribeToMatchmaking(
    onMatchFound: MessageHandler<MatchFoundNotification>,
    onError?: ErrorHandler
  ): void {
    if (!this.client?.connected) {
      console.error('WebSocket not connected. Cannot subscribe to matchmaking.');
      return;
    }

    const destination = '/user/queue/match-found';

    // Update callback (allows updating without recreating subscription)
    this.matchmakingCallback = onMatchFound;
    console.log('[Matchmaking] Callback registered');

    // If already subscribed, just update the callback
    if (this.subscriptions.has(destination)) {
      console.log('[Matchmaking] Already subscribed, callback updated');
      return;
    }

    // Create new subscription
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        console.log('[Matchmaking] ✅ Match found notification received!', message.body);
        const notification: MatchFoundNotification = JSON.parse(message.body);

        // Use stored callback (might be updated since subscription)
        if (this.matchmakingCallback) {
          this.matchmakingCallback(notification);
        } else {
          console.error('[Matchmaking] No callback registered!');
        }
      } catch (error) {
        console.error('[Matchmaking] Error parsing notification:', error);
      }
    });

    this.subscriptions.set(destination, subscription);
    console.log('[Matchmaking] ✅ Subscribed to /user/queue/match-found');
  }

  /**
   * Subscribe to error messages
   */
  public subscribeToErrors(onError: ErrorHandler): void {
    if (!this.client?.connected) {
      console.error('WebSocket not connected. Cannot subscribe to errors.');
      return;
    }

    const destination = '/user/queue/errors';

    if (this.subscriptions.has(destination)) {
      console.log('Already subscribed to errors');
      return;
    }

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const error: WebSocketError = JSON.parse(message.body);
        onError(error);
      } catch (err) {
        console.error('Error parsing error message:', err);
      }
    });

    this.subscriptions.set(destination, subscription);
    console.log('Subscribed to error notifications');
  }

  /**
   * Send a move for a specific game
   */
  public sendMove(gameId: number, move: WebSocketMoveMessage): void {
    if (!this.client?.connected) {
      console.error('WebSocket not connected. Cannot send move.');
      throw new Error('WebSocket not connected');
    }

    const destination = `/app/game/${gameId}/move`;
    this.client.publish({
      destination,
      body: JSON.stringify(move),
    });

    console.log(`Sent move to game ${gameId}:`, move);
  }

  /**
   * Unsubscribe from a specific game
   */
  public unsubscribeFromGame(gameId: number): void {
    const destination = `/topic/game/${gameId}`;
    const subscription = this.subscriptions.get(destination);

    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log(`Unsubscribed from game ${gameId}`);
    }
  }

  /**
   * Unsubscribe from matchmaking
   */
  public unsubscribeFromMatchmaking(): void {
    const destination = '/user/queue/match-found';
    const subscription = this.subscriptions.get(destination);

    // Clear callback
    this.matchmakingCallback = null;

    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(destination);
      console.log('[Matchmaking] Unsubscribed from /user/queue/match-found');
    }
  }

  /**
   * Check if WebSocket is connected
   */
  public isConnected(): boolean {
    return this.client?.connected || false;
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();
