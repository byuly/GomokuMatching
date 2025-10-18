/**
 * Game Utilities - Board helpers and game logic
 */

import type { Board, WinnerType, StoneColor } from '@/types';

/**
 * Create an empty 15x15 board
 */
export const createEmptyBoard = (): Board => {
  return Array(15)
    .fill(null)
    .map(() => Array(15).fill(0));
};

/**
 * Check if a position is valid on the board
 */
export const isValidPosition = (row: number, col: number): boolean => {
  return row >= 0 && row < 15 && col >= 0 && col < 15;
};

/**
 * Check if a cell is empty
 */
export const isCellEmpty = (board: Board, row: number, col: number): boolean => {
  return isValidPosition(row, col) && board[row][col] === 0;
};

/**
 * Get stone color from player number
 */
export const getStoneColor = (player: number): StoneColor => {
  return player === 1 ? StoneColor.BLACK : StoneColor.WHITE;
};

/**
 * Get player number from stone color
 */
export const getPlayerFromColor = (color: StoneColor): number => {
  return color === StoneColor.BLACK ? 1 : 2;
};

/**
 * Format winner type to display string
 */
export const formatWinner = (winner: WinnerType, player1Username?: string, player2Username?: string): string => {
  switch (winner) {
    case WinnerType.PLAYER1:
      return player1Username ? `${player1Username} wins!` : 'Black wins!';
    case WinnerType.PLAYER2:
      return player2Username ? `${player2Username} wins!` : 'White wins!';
    case WinnerType.AI:
      return 'AI wins!';
    case WinnerType.DRAW:
      return "It's a draw!";
    case WinnerType.NONE:
      return 'Game in progress';
    default:
      return 'Unknown';
  }
};

/**
 * Get next player turn
 */
export const getNextPlayer = (currentPlayer: number): number => {
  return currentPlayer === 1 ? 2 : 1;
};

/**
 * Count total moves on the board
 */
export const countMoves = (board: Board): number => {
  let count = 0;
  for (let i = 0; i < 15; i++) {
    for (let j = 0; j < 15; j++) {
      if (board[i][j] !== 0) count++;
    }
  }
  return count;
};

/**
 * Check if board is full
 */
export const isBoardFull = (board: Board): boolean => {
  return countMoves(board) === 225; // 15x15 = 225
};

/**
 * Convert board position to coordinate string (e.g., "A1", "B2")
 */
export const positionToCoordinate = (row: number, col: number): string => {
  const letters = 'ABCDEFGHIJKLMNO';
  return `${letters[col]}${row + 1}`;
};

/**
 * Convert coordinate string to board position
 */
export const coordinateToPosition = (coordinate: string): { row: number; col: number } | null => {
  if (coordinate.length < 2) return null;

  const letters = 'ABCDEFGHIJKLMNO';
  const col = letters.indexOf(coordinate[0].toUpperCase());
  const row = parseInt(coordinate.substring(1)) - 1;

  if (col === -1 || !isValidPosition(row, col)) return null;

  return { row, col };
};

/**
 * Create a deep copy of the board
 */
export const cloneBoard = (board: Board): Board => {
  return board.map((row) => [...row]);
};

/**
 * Get a formatted time string from ISO date
 */
export const formatGameTime = (isoDate: string): string => {
  const date = new Date(isoDate);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);

  if (diffMins < 1) return 'Just now';
  if (diffMins === 1) return '1 minute ago';
  if (diffMins < 60) return `${diffMins} minutes ago`;

  const diffHours = Math.floor(diffMins / 60);
  if (diffHours === 1) return '1 hour ago';
  if (diffHours < 24) return `${diffHours} hours ago`;

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) return '1 day ago';
  return `${diffDays} days ago`;
};
