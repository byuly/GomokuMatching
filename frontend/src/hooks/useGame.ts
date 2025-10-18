/**
 * useGame Hook - Game state management
 */

import { useState, useEffect, useCallback } from 'react';
import { gameService } from '@/services';
import { useWebSocket } from '@/context';
import { GameType } from '@/types';
import type {
  GameState,
  CreateGameRequest,
  MakeMoveRequest,
  WebSocketGameUpdate,
  WebSocketError,
} from '@/types';
import { getUserFriendlyError, logError } from '@/utils';

interface UseGameReturn {
  gameState: GameState | null;
  loading: boolean;
  error: string | null;
  createGame: (request: CreateGameRequest) => Promise<string>;
  loadGame: (gameId: string) => Promise<void>;
  makeMove: (row: number, col: number) => Promise<void>;
  forfeitGame: () => Promise<void>;
  clearError: () => void;
}

export const useGame = (gameId?: string): UseGameReturn => {
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const { subscribeToGame, unsubscribeFromGame, sendMove, connected } = useWebSocket();

  // Load game state on mount if gameId provided
  useEffect(() => {
    if (gameId) {
      loadGame(gameId);
    }
  }, [gameId]);

  // Subscribe to WebSocket updates for PvP games
  useEffect(() => {
    if (gameId && gameState?.gameType === GameType.PVP && connected) {
      subscribeToGame(
        gameId,
        handleGameUpdate,
        handleWebSocketError
      );

      return () => {
        unsubscribeFromGame(gameId);
      };
    }
  }, [gameId, gameState?.gameType, connected]);

  const handleGameUpdate = useCallback((update: WebSocketGameUpdate): void => {
    setGameState((prevState) => {
      if (!prevState) return null;

      return {
        ...prevState,
        board: update.board,
        currentPlayer: update.currentPlayer,
        status: update.status,
        winnerType: update.winnerType,
        moveCount: update.moveCount,
        lastActivity: new Date().toISOString(),
      };
    });
  }, []);

  const handleWebSocketError = useCallback((wsError: WebSocketError): void => {
    const errorMessage = getUserFriendlyError(wsError);
    setError(errorMessage);
    logError(wsError, 'Game WebSocket');
  }, []);

  const createGame = async (request: CreateGameRequest): Promise<string> => {
    setLoading(true);
    setError(null);

    try {
      const response = await gameService.createGame(request);
      await loadGame(response.gameId);
      return response.gameId;
    } catch (err) {
      const errorMessage = getUserFriendlyError(err);
      setError(errorMessage);
      logError(err, 'Create game');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const loadGame = async (id: string): Promise<void> => {
    setLoading(true);
    setError(null);

    try {
      const state = await gameService.getGameState(id);
      setGameState(state);
    } catch (err) {
      const errorMessage = getUserFriendlyError(err);
      setError(errorMessage);
      logError(err, 'Load game');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const makeMove = async (row: number, col: number): Promise<void> => {
    if (!gameState) {
      throw new Error('No active game');
    }

    setError(null);

    try {
      if (gameState.gameType === GameType.PVAI) {
        // For PvAI, use REST API - backend returns full updated state
        const moveRequest: MakeMoveRequest = { row, col };
        const updatedState = await gameService.makeMove(gameState.gameId, moveRequest);
        setGameState(updatedState);
      } else {
        // For PvP, use WebSocket
        sendMove(gameState.gameId, { row, col });
        // Game state will be updated via WebSocket subscription
      }
    } catch (err) {
      const errorMessage = getUserFriendlyError(err);
      setError(errorMessage);
      logError(err, 'Make move');
      throw err;
    }
  };

  const forfeitGame = async (): Promise<void> => {
    if (!gameState) {
      throw new Error('No active game');
    }

    setLoading(true);
    setError(null);

    try {
      const updatedState = await gameService.forfeitGame(gameState.gameId);
      setGameState(updatedState);
    } catch (err) {
      const errorMessage = getUserFriendlyError(err);
      setError(errorMessage);
      logError(err, 'Forfeit game');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const clearError = (): void => {
    setError(null);
  };

  return {
    gameState,
    loading,
    error,
    createGame,
    loadGame,
    makeMove,
    forfeitGame,
    clearError,
  };
};
