/**
 * WebSocket Context - Global WebSocket connection management
 */

import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { websocketService } from '@/services';
import { authService } from '@/services';
import { useAuth } from './AuthContext';
import type {
  WebSocketGameUpdate,
  WebSocketError,
  MatchFoundNotification,
  WebSocketMoveMessage,
} from '@/types';
import { logError } from '@/utils';

interface WebSocketContextType {
  connected: boolean;
  connect: () => Promise<void>;
  disconnect: () => void;
  subscribeToGame: (
    gameId: number,
    onUpdate: (update: WebSocketGameUpdate) => void,
    onError?: (error: WebSocketError) => void
  ) => void;
  unsubscribeFromGame: (gameId: number) => void;
  subscribeToMatchmaking: (
    onMatchFound: (notification: MatchFoundNotification) => void,
    onError?: (error: WebSocketError) => void
  ) => void;
  unsubscribeFromMatchmaking: () => void;
  sendMove: (gameId: number, move: WebSocketMoveMessage) => void;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

interface WebSocketProviderProps {
  children: ReactNode;
}

export const WebSocketProvider: React.FC<WebSocketProviderProps> = ({ children }) => {
  const [connected, setConnected] = useState<boolean>(false);
  const { isAuthenticated, user } = useAuth();

  // Connect to WebSocket when user is authenticated
  useEffect(() => {
    if (isAuthenticated && !connected) {
      connectWebSocket();
    } else if (!isAuthenticated && connected) {
      disconnectWebSocket();
    }

    return () => {
      if (connected) {
        disconnectWebSocket();
      }
    };
  }, [isAuthenticated]);

  const connectWebSocket = async (): Promise<void> => {
    try {
      const token = authService.getAccessToken();
      if (!token) {
        throw new Error('No access token available');
      }

      console.log('[WebSocketContext] Connecting WebSocket for user:', user?.playerId, user?.username);
      await websocketService.connect(token);
      setConnected(true);
      console.log('[WebSocketContext] ✅ WebSocket connected successfully');

      // Subscribe to global error messages
      websocketService.subscribeToErrors((error: WebSocketError) => {
        console.error('[WebSocketContext] WebSocket error:', error);
        logError(error, 'WebSocket error');
      });
    } catch (err) {
      console.error('[WebSocketContext] ❌ WebSocket connection failed:', err);
      logError(err, 'WebSocket connection');
      setConnected(false);
    }
  };

  const disconnectWebSocket = (): void => {
    websocketService.disconnect();
    setConnected(false);
  };

  const connect = useCallback(async (): Promise<void> => {
    await connectWebSocket();
  }, []);

  const disconnect = useCallback((): void => {
    disconnectWebSocket();
  }, []);

  const subscribeToGame = useCallback(
    (
      gameId: number,
      onUpdate: (update: WebSocketGameUpdate) => void,
      onError?: (error: WebSocketError) => void
    ): void => {
      if (!connected) {
        logError(new Error('WebSocket not connected'), 'subscribeToGame');
        return;
      }
      websocketService.subscribeToGame(gameId, onUpdate, onError);
    },
    [connected]
  );

  const unsubscribeFromGame = useCallback(
    (gameId: number): void => {
      websocketService.unsubscribeFromGame(gameId);
    },
    []
  );

  const subscribeToMatchmaking = useCallback(
    (
      onMatchFound: (notification: MatchFoundNotification) => void,
      onError?: (error: WebSocketError) => void
    ): void => {
      if (!connected) {
        logError(new Error('WebSocket not connected'), 'subscribeToMatchmaking');
        return;
      }
      websocketService.subscribeToMatchmaking(onMatchFound, onError);
    },
    [connected]
  );

  const unsubscribeFromMatchmaking = useCallback((): void => {
    websocketService.unsubscribeFromMatchmaking();
  }, []);

  const sendMove = useCallback(
    (gameId: number, move: WebSocketMoveMessage): void => {
      if (!connected) {
        throw new Error('WebSocket not connected');
      }
      websocketService.sendMove(gameId, move);
    },
    [connected]
  );

  const value: WebSocketContextType = {
    connected,
    connect,
    disconnect,
    subscribeToGame,
    unsubscribeFromGame,
    subscribeToMatchmaking,
    unsubscribeFromMatchmaking,
    sendMove,
  };

  return <WebSocketContext.Provider value={value}>{children}</WebSocketContext.Provider>;
};

export const useWebSocket = (): WebSocketContextType => {
  const context = useContext(WebSocketContext);
  if (context === undefined) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};
