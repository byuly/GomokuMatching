/**
 * Stone Component - Game piece (black or white)
 */

import React from 'react';

interface StoneProps {
  player: number; // 1 = black, 2 = white
  isWinning?: boolean;
}

export const Stone: React.FC<StoneProps> = ({ player, isWinning = false }) => {
  const isBlack = player === 1;
  const baseColor = isBlack ? 'var(--arcade-white)' : 'var(--arcade-black)';
  const borderColor = isBlack ? 'var(--arcade-cyan)' : 'var(--arcade-pink)';

  return (
    <div
      className={`
        absolute inset-1
        rounded-full
        transition-all duration-300
        ${isWinning ? 'animate-pulse' : ''}
      `.trim().replace(/\s+/g, ' ')}
      style={{
        backgroundColor: baseColor,
        border: `2px solid ${borderColor}`,
        boxShadow: isWinning
          ? `0 0 10px ${borderColor}, 0 0 20px ${borderColor}, 0 0 30px ${borderColor}`
          : `0 0 4px ${borderColor}`,
      }}
    />
  );
};
