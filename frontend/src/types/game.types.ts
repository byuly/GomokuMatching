/**
 * Game Types - Aligned with Backend API Specs
 */

// Enums (from backend)
export enum GameType {
  PVP = 'HUMAN_VS_HUMAN',
  PVAI = 'HUMAN_VS_AI'
}

export enum GameStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  ABANDONED = 'ABANDONED'
}

export enum WinnerType {
  PLAYER1 = 'PLAYER1',
  PLAYER2 = 'PLAYER2',
  AI = 'AI',
  DRAW = 'DRAW',
  NONE = 'NONE'
}

export enum AIDifficulty {
  EASY = 'EASY',
  MEDIUM = 'MEDIUM',
  HARD = 'HARD',
  EXPERT = 'EXPERT'
}

export enum StoneColor {
  BLACK = 'BLACK',
  WHITE = 'WHITE'
}

// Core Game Types
export type Board = number[][]; // 15x15 2D array (0 = empty, 1 = player1/black, 2 = player2/white)

export interface Stone {
  row: number;
  col: number;
  player: number; // 1 or 2
  color: StoneColor;
}

// GameState from backend GET /{gameId}
export interface GameState {
  gameId: string; // UUID
  gameType: GameType;
  status: GameStatus;
  player1Id: string; // UUID
  player2Id: string | null; // UUID or null for AI
  aiDifficulty: AIDifficulty | null;
  board: Board;
  currentPlayer: number; // 1 or 2
  moveCount: number;
  winnerType: WinnerType;
  winnerId: string | null; // UUID
  startedAt: string;
  endedAt: string | null;
  lastActivity: string;
}

// Request/Response Types
export interface CreateGameRequest {
  gameType: GameType;
  player2Id?: string | null;
  aiDifficulty?: AIDifficulty | null;
}

export interface CreateGameResponse {
  gameId: string; // UUID
  gameType: GameType;
  websocketTopic?: string;
  message: string;
}

export interface MakeMoveRequest {
  row: number;
  col: number;
}

// WebSocket Message Types
export interface WebSocketMoveMessage {
  row: number;
  col: number;
}

export interface WebSocketGameUpdate {
  gameId: string;
  board: Board;
  currentPlayer: number;
  status: GameStatus;
  winnerType: WinnerType;
  winningLine: number[][] | null;
  lastMove: {
    row: number;
    col: number;
    playerId: string;
    playerUsername: string;
  };
  moveCount: number;
}

export interface WebSocketError {
  gameId: string;
  code: string;
  message: string;
  timestamp: string;
}

// Move history
export interface MoveHistoryItem {
  moveNumber: number;
  playerType: 'HUMAN' | 'AI';
  playerId: string | null;
  boardX: number;
  boardY: number;
  stoneColor: StoneColor;
  timestamp: string;
}
