/**
 * Cell Component - Individual cell on the Gomoku board
 */

import React from 'react';
import { Stone } from './Stone';

interface CellProps {
  value: number; // 0 = empty, 1 = black, 2 = white
  onClick: () => void;
  disabled?: boolean;
  isWinning?: boolean;
  isLastMove?: boolean;
}

export const Cell: React.FC<CellProps> = ({
  value,
  onClick,
  disabled = false,
  isWinning = false,
  isLastMove = false,
}) => {
  const isEmpty = value === 0;
  const canClick = !disabled && isEmpty;

  return (
    <button
      onClick={onClick}
      disabled={!canClick}
      className={`
        flex-1
        relative
        border border-[var(--arcade-gray)]
        bg-[var(--arcade-black)]
        transition-all duration-150
        ${canClick ? 'hover:bg-[var(--arcade-light-gray)] cursor-pointer' : ''}
        ${isLastMove ? 'bg-[var(--arcade-light-gray)]' : ''}
        ${disabled ? 'cursor-not-allowed' : ''}
      `.trim().replace(/\s+/g, ' ')}
    >
      {!isEmpty && <Stone player={value} isWinning={isWinning} />}
      {isEmpty && canClick && (
        <div className="absolute inset-0 flex items-center justify-center opacity-0 hover:opacity-30 transition-opacity">
          <div className="w-[20%] aspect-square rounded-full bg-[var(--arcade-cyan)]" />
        </div>
      )}
    </button>
  );
};
