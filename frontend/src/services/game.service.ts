/**
 * Game Service - Game creation, moves, and state management
 */

import { api } from './api.service';
import type {
  CreateGameRequest,
  CreateGameResponse,
  GameState,
  MakeMoveRequest,
  MoveHistoryItem,
} from '@/types';

class GameService {
  /**
   * Create a new game (PvP or PvAI)
   */
  async createGame(data: CreateGameRequest): Promise<CreateGameResponse> {
    const response = await api.post<CreateGameResponse>('/api/game/create', data);
    return response.data;
  }

  /**
   * Get current game state
   */
  async getGameState(gameId: string): Promise<GameState> {
    const response = await api.get<GameState>(`/api/game/${gameId}`);
    return response.data;
  }

  /**
   * Make a move (for PvAI games only - PvP uses WebSocket)
   * Returns full updated GameState
   */
  async makeMove(gameId: string, move: MakeMoveRequest): Promise<GameState> {
    const response = await api.post<GameState>(`/api/game/${gameId}/move`, move);
    return response.data;
  }

  /**
   * Forfeit the game
   */
  async forfeitGame(gameId: string): Promise<GameState> {
    const response = await api.post<GameState>(`/api/game/${gameId}/forfeit`);
    return response.data;
  }

  /**
   * Get move history for a game
   */
  async getMoveHistory(gameId: string): Promise<MoveHistoryItem[]> {
    const response = await api.get<MoveHistoryItem[]>(`/api/game/${gameId}/moves`);
    return response.data;
  }
}

// Export singleton instance
export const gameService = new GameService();
