/**
 * HomePage - Landing page with arcade-style intro
 */

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context';
import { Button } from '@/components/ui';
import { PageContainer } from '@/components/layout';

export const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const handleStart = () => {
    if (isAuthenticated) {
      navigate('/lobby');
    } else {
      navigate('/login');
    }
  };

  return (
    <PageContainer className="flex items-center justify-center">
      <div className="text-center max-w-2xl">
        {/* Title */}
        <h1 className="text-[32px] md:text-[48px] font-[var(--font-pixel)] text-glow-cyan uppercase mb-8 leading-relaxed">
          Gomoku
        </h1>

        {/* Subtitle */}
        <p className="text-[14px] md:text-[16px] font-[var(--font-pixel)] text-[var(--arcade-yellow)] uppercase mb-12">
          Five in a Row
        </p>

        {/* Game description */}
        <div className="mb-12 space-y-4">
          <p className="text-[10px] font-[var(--font-mono)] text-[var(--arcade-white)] leading-relaxed">
            Connect five stones in a row - horizontally, vertically, or diagonally.
          </p>
          <p className="text-[10px] font-[var(--font-mono)] text-[var(--arcade-white)] leading-relaxed">
            Play against AI or challenge other players in real-time PvP matches.
          </p>
        </div>

        {/* Start button */}
        <Button
          variant="primary"
          size="large"
          glow
          pulse
          onClick={handleStart}
          className="mb-8"
        >
          Press Start
        </Button>

        {/* Instructions */}
        <div className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase space-y-2">
          <p className="blink">Insert Coin to Continue</p>
        </div>

        {/* Decorative elements */}
        <div className="mt-16 flex justify-center gap-8">
          <div className="w-12 h-12 rounded-full bg-[var(--arcade-cyan)] pixel-border animate-pulse" />
          <div className="w-12 h-12 rounded-full bg-[var(--arcade-pink)] pixel-border animate-pulse" style={{ animationDelay: '0.5s' }} />
          <div className="w-12 h-12 rounded-full bg-[var(--arcade-yellow)] pixel-border animate-pulse" style={{ animationDelay: '1s' }} />
        </div>
      </div>
    </PageContainer>
  );
};
