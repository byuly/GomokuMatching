/**
 * LobbyPage - Game mode selection lobby
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context';
import { useGame } from '@/hooks';
import { Button, Card, CardHeader, CardTitle, CardContent, Loader } from '@/components/ui';
import { PageContainer } from '@/components/layout';
import { GameType, AIDifficulty } from '@/types';

export const LobbyPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { createGame } = useGame();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCreateAIGame = async (difficulty: AIDifficulty) => {
    setLoading(true);
    setError(null);

    try {
      const gameId = await createGame({
        gameType: GameType.PVAI,
        aiDifficulty: difficulty,
      });

      navigate(`/game/${gameId}?new=true`);
    } catch (err) {
      setError('Failed to create game. Please try again.');
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <PageContainer className="flex items-center justify-center">
        <Loader text="Creating game..." size="large" />
      </PageContainer>
    );
  }

  return (
    <PageContainer className="flex items-center justify-center">
      <div className="w-full max-w-3xl mx-auto px-4">
        {/* Welcome Message */}
        <div className="text-center mb-8">
          <h1 className="text-[24px] font-[var(--font-pixel)] text-glow-cyan uppercase mb-4">
            Game Lobby
          </h1>
          <p className="text-[12px] font-[var(--font-mono)] text-[var(--arcade-white)]">
            Welcome back, {user?.username}!
          </p>
        </div>

        {/* Error Display */}
        {error && (
          <div className="mb-6 p-4 pixel-border border-[var(--arcade-red)] bg-[var(--arcade-black)]">
            <p className="text-[10px] font-[var(--font-pixel)] text-[var(--arcade-red)] text-center">
              {error}
            </p>
          </div>
        )}

        {/* Game Mode Selection */}
        <div className="grid md:grid-cols-2 gap-6">
          {/* PvP Mode */}
          <Card glow color="cyan">
            <CardHeader>
              <CardTitle glow>Player vs Player</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-[10px] leading-relaxed">
                Challenge other players in real-time matches. Join the matchmaking queue and get paired with an opponent of similar skill.
              </p>
              <div className="flex justify-center">
                <Button
                  variant="primary"
                  size="medium"
                  glow
                  pulse
                  onClick={() => navigate('/matchmaking')}
                >
                  Find Match
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* PvAI Mode */}
          <Card glow color="pink">
            <CardHeader>
              <CardTitle glow>Player vs AI</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-[10px] leading-relaxed">
                Practice your skills against AI opponents. Choose from three difficulty levels: Easy, Medium, or Hard.
              </p>
              <div className="flex gap-2 justify-center flex-wrap">
                <Button
                  variant="success"
                  size="small"
                  onClick={() => handleCreateAIGame(AIDifficulty.EASY)}
                  disabled={loading}
                >
                  Easy
                </Button>
                <Button
                  variant="secondary"
                  size="small"
                  onClick={() => handleCreateAIGame(AIDifficulty.MEDIUM)}
                  disabled={loading}
                >
                  Medium
                </Button>
                <Button
                  variant="danger"
                  size="small"
                  onClick={() => handleCreateAIGame(AIDifficulty.HARD)}
                  disabled={loading}
                >
                  Hard
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* How to Play */}
        <Card color="yellow" className="mt-6">
          <CardHeader>
            <CardTitle>How to Play</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2 text-[10px]">
              <p>1. Choose your game mode: PvP or PvAI</p>
              <p>2. Place your stones on the 15x15 board</p>
              <p>3. Connect five stones in a row to win</p>
              <p>4. Horizontal, vertical, or diagonal lines count</p>
              <p>5. Black always goes first</p>
            </div>
          </CardContent>
        </Card>
      </div>
    </PageContainer>
  );
};
