/**
 * Board Component - Gomoku game board (15x15 grid)
 */

import React from 'react';
import { Cell } from './Cell';
import type { Board as BoardType } from '@/types';

interface BoardProps {
  board: BoardType;
  onCellClick?: (row: number, col: number) => void;
  disabled?: boolean;
  winningLine?: number[][] | null;
  lastMove?: { row: number; col: number } | null;
}

export const Board: React.FC<BoardProps> = ({
  board,
  onCellClick,
  disabled = false,
  winningLine = null,
  lastMove = null,
}) => {
  const isWinningCell = (row: number, col: number): boolean => {
    if (!winningLine) return false;
    return winningLine.some(([r, c]) => r === row && c === col);
  };

  const isLastMove = (row: number, col: number): boolean => {
    if (!lastMove) return false;
    return lastMove.row === row && lastMove.col === col;
  };

  const handleCellClick = (row: number, col: number) => {
    if (disabled || !onCellClick) return;
    if (board[row][col] !== 0) return; // Cell already occupied
    onCellClick(row, col);
  };

  return (
    <div className="bg-[var(--arcade-dark-gray)] p-1 pixel-border-glow border-[var(--arcade-yellow)] aspect-square h-full max-h-full">
      <div className="w-full h-full flex flex-col">
        {/* Board grid */}
        {board.map((row, rowIndex) => (
          <div key={rowIndex} className="flex flex-1">
            {/* Cells */}
            {row.map((cell, colIndex) => (
              <Cell
                key={`${rowIndex}-${colIndex}`}
                value={cell}
                onClick={() => handleCellClick(rowIndex, colIndex)}
                disabled={disabled}
                isWinning={isWinningCell(rowIndex, colIndex)}
                isLastMove={isLastMove(rowIndex, colIndex)}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};
