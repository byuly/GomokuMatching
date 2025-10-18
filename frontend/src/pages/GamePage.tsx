/**
 * GamePage - Active game interface
 */

import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/context';
import { useGame } from '@/hooks';
import { Board, TurnIndicator } from '@/components/game';
import { Button, Card, CardHeader, CardTitle, CardContent, Loader } from '@/components/ui';
import { PageContainer } from '@/components/layout';
import { GameStatus, WinnerType, GameType } from '@/types';

export const GamePage: React.FC = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const newGame = searchParams.get('new') === 'true';

  const { gameState, loading, error, loadGame, makeMove, forfeitGame } = useGame(gameId);

  const [localError, setLocalError] = useState<string | null>(null);

  useEffect(() => {
    if (gameId && !newGame) {
      loadGame(gameId);
    }
  }, [gameId, newGame]);

  const handleCellClick = async (row: number, col: number) => {
    if (!gameState) return;
    if (gameState.status !== GameStatus.IN_PROGRESS) return;

    setLocalError(null);
    try {
      await makeMove(row, col);
    } catch (err) {
      setLocalError('Failed to make move. Please try again.');
    }
  };

  const handleForfeit = async () => {
    if (!window.confirm('Are you sure you want to forfeit this game?')) {
      return;
    }

    try {
      await forfeitGame();
    } catch (err) {
      setLocalError('Failed to forfeit game.');
    }
  };

  const handleReturnToLobby = () => {
    navigate('/lobby');
  };

  if (loading && !gameState) {
    return (
      <PageContainer className="flex items-center justify-center">
        <Loader text="Loading game..." size="large" />
      </PageContainer>
    );
  }

  if (error || !gameState) {
    return (
      <PageContainer className="flex items-center justify-center">
        <Card color="red" glow>
          <CardHeader>
            <CardTitle>Error</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-[12px] mb-4">{error || 'Game not found'}</p>
            <Button variant="primary" onClick={handleReturnToLobby}>
              Return to Lobby
            </Button>
          </CardContent>
        </Card>
      </PageContainer>
    );
  }

  const isMyTurn = gameState.currentPlayer === 1
    ? gameState.player1Id === user?.id
    : gameState.player2Id === user?.id;

  const isGameOver = gameState.status === GameStatus.COMPLETED || gameState.status === GameStatus.ABANDONED;

  // Determine player labels
  const player1Label = 'Player 1 (Black)';
  const player2Label = gameState.gameType === GameType.PVAI ? 'AI (White)' : 'Player 2 (White)';

  // Determine winner text
  const getWinnerText = (): string => {
    switch (gameState.winnerType) {
      case WinnerType.PLAYER1:
        return 'Player 1 (Black) wins!';
      case WinnerType.PLAYER2:
        return gameState.gameType === GameType.PVAI ? 'AI wins!' : 'Player 2 (White) wins!';
      case WinnerType.AI:
        return 'AI wins!';
      case WinnerType.DRAW:
        return "It's a draw!";
      default:
        return 'Game in progress';
    }
  };

  const myPlayerNumber = gameState.player1Id === user?.id ? 1 : 2;

  return (
    <div className="h-[100vh] w-full flex flex-col overflow-hidden bg-[var(--arcade-black)]">
      {/* Header with Player Info Bar */}
      <div className="px-4 py-1 border-b-2 border-[var(--arcade-cyan)] flex-shrink-0">
        <div className="flex items-center justify-between gap-4 max-w-6xl mx-auto">
          {/* Black Player */}
          <div className={`flex items-center gap-3 px-4 py-2 pixel-border ${gameState.currentPlayer === 1 && !isGameOver ? 'border-[var(--arcade-cyan)] bg-[var(--arcade-dark-gray)]' : 'border-[var(--arcade-gray)]'}`}>
            <div className="w-4 h-4 rounded-full bg-[var(--arcade-black)] border-2 border-[var(--arcade-white)]"></div>
            <div>
              <p className="text-[10px] font-[var(--font-pixel)] text-[var(--arcade-cyan)]">{player1Label}</p>
              {gameState.player1Id === user?.id && (
                <p className="text-[8px] text-[var(--arcade-yellow)]">You</p>
              )}
            </div>
          </div>

          {/* Game Info */}
          <div className="text-center">
            <h1 className="text-[16px] font-[var(--font-pixel)] text-glow-cyan uppercase">
              {gameState.gameType === GameType.PVP ? 'PvP' : 'vs AI'}
            </h1>
            <p className="text-[8px] font-[var(--font-mono)] text-[var(--arcade-gray)]">
              Moves: {gameState.moveCount} | {gameState.aiDifficulty || 'Human'}
            </p>
          </div>

          {/* White Player */}
          <div className={`flex items-center gap-3 px-4 py-2 pixel-border ${gameState.currentPlayer === 2 && !isGameOver ? 'border-[var(--arcade-pink)] bg-[var(--arcade-dark-gray)]' : 'border-[var(--arcade-gray)]'}`}>
            <div>
              <p className="text-[10px] font-[var(--font-pixel)] text-[var(--arcade-pink)]">{player2Label}</p>
              {gameState.player2Id === user?.id && (
                <p className="text-[8px] text-[var(--arcade-yellow)]">You</p>
              )}
            </div>
            <div className="w-4 h-4 rounded-full bg-[var(--arcade-white)] border-2 border-[var(--arcade-black)]"></div>
          </div>
        </div>
      </div>

      {/* Main Game Area */}
      <div className="flex-1 min-h-0 flex flex-col items-center justify-center gap-1 py-1">
        <div className="flex-shrink-0 w-full max-w-xl px-4">
          <TurnIndicator
            currentTurn={gameState.currentPlayer}
            player1Username={player1Label}
            player2Username={player2Label}
            isYourTurn={isMyTurn && !isGameOver}
          />
        </div>

        <div className="flex-1 min-h-0 flex items-center justify-center px-1">
          <Board
            board={gameState.board}
            onCellClick={handleCellClick}
            disabled={!isMyTurn || isGameOver}
            winningLine={null}
            lastMove={null}
          />
        </div>

        {/* Action Buttons */}
        <div className="flex gap-3 items-center flex-shrink-0 px-4 pb-1">
          {localError && (
            <div className="p-2 pixel-border border-[var(--arcade-red)] bg-[var(--arcade-black)]">
              <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-red)]">{localError}</p>
            </div>
          )}

          {isGameOver && (
            <div className="flex items-center gap-3">
              <p className="text-[12px] font-[var(--font-pixel)] text-[var(--arcade-green)]">{getWinnerText()}</p>
              <Button variant="primary" onClick={handleReturnToLobby}>
                Return to Lobby
              </Button>
            </div>
          )}

          {!isGameOver && (
            <div className="flex gap-3">
              <Button variant="danger" onClick={handleForfeit}>
                Forfeit Game
              </Button>
              <Button variant="secondary" onClick={handleReturnToLobby}>
                Exit
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
