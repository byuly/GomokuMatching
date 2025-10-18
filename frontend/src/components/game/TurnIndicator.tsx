/**
 * TurnIndicator Component - Shows whose turn it is
 */

import React from 'react';
import { Card, CardContent } from '../ui/Card';
import { Stone } from './Stone';

interface TurnIndicatorProps {
  currentTurn: number; // 1 or 2
  player1Username?: string;
  player2Username?: string;
  isYourTurn?: boolean;
}

export const TurnIndicator: React.FC<TurnIndicatorProps> = ({
  currentTurn,
  player1Username = 'Player 1',
  player2Username = 'Player 2',
  isYourTurn = false,
}) => {
  const currentPlayer = currentTurn === 1 ? player1Username : player2Username;
  const color = currentTurn === 1 ? 'cyan' : 'pink';

  return (
    <Card color={color} glow={isYourTurn} className="w-full">
      <CardContent>
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 relative bg-[var(--arcade-black)] border-2 border-[var(--arcade-gray)] rounded">
            <Stone player={currentTurn} />
          </div>
          <div className="flex-1">
            <p className="text-[10px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase mb-1">
              Current Turn
            </p>
            <p className={`text-[14px] font-[var(--font-pixel)] uppercase ${isYourTurn ? 'text-glow-yellow blink' : ''}`}
              style={{ color: currentTurn === 1 ? 'var(--arcade-cyan)' : 'var(--arcade-pink)' }}
            >
              {currentPlayer}
            </p>
            {isYourTurn && (
              <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-yellow)] uppercase mt-1">
                Your turn!
              </p>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
