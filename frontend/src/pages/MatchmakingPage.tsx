/**
 * MatchmakingPage - Queue and waiting for opponent
 */

import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMatchmaking } from '@/hooks';
import { Button, Card, CardHeader, CardTitle, CardContent, Loader } from '@/components/ui';
import { PageContainer } from '@/components/layout';

export const MatchmakingPage: React.FC = () => {
  const navigate = useNavigate();
  const { queueStatus, loading, error, inQueue, joinQueue, leaveQueue, onMatchFound } = useMatchmaking();
  const [matchFound, setMatchFound] = useState(false);
  const [isJoining, setIsJoining] = useState(true);
  const hasJoinedRef = useRef(false);
  const inQueueRef = useRef(false);

  // Keep ref in sync with inQueue state
  useEffect(() => {
    inQueueRef.current = inQueue;
  }, [inQueue]);

  // Setup match found callback BEFORE joining queue
  useEffect(() => {
    console.log('[MatchmakingPage] Setting up match found callback...');
    onMatchFound((notification) => {
      console.log('[MatchmakingPage] 🎉 MATCH FOUND!!!', notification);
      console.log('[MatchmakingPage] Game ID:', notification.gameId);
      console.log('[MatchmakingPage] Player Number:', notification.playerNumber);
      console.log('[MatchmakingPage] Opponent ID:', notification.opponentId);

      setMatchFound(true);

      // Small delay to show "Match Found!" message
      setTimeout(() => {
        console.log('[MatchmakingPage] Navigating to game...');
        navigate(`/game/${notification.gameId}?new=true`);
      }, 1000);
    });
  }, [onMatchFound, navigate]);

  // Join queue on mount (only once)
  useEffect(() => {
    // Prevent re-running if already joined
    if (hasJoinedRef.current) return;
    hasJoinedRef.current = true;

    const join = async () => {
      try {
        setIsJoining(true);

        console.log('[MatchmakingPage] Joining matchmaking queue...');

        // Join the queue (backend handles "already in queue" case)
        await joinQueue();

        console.log('[MatchmakingPage] ✅ Successfully joined queue, waiting for match...');
      } catch (err) {
        console.error('[MatchmakingPage] ❌ Failed to join queue:', err);
        // Error is already set by useMatchmaking hook
      } finally {
        setIsJoining(false);
      }
    };

    join();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Only run on mount

  const handleCancelSearch = async () => {
    try {
      await leaveQueue();
      navigate('/lobby');
    } catch (err) {
      console.error('Failed to leave queue:', err);
    }
  };

  // Cleanup: Leave queue when component unmounts (user navigates away)
  useEffect(() => {
    return () => {
      // Use ref to check queue status at unmount time (not state which causes re-renders)
      if (inQueueRef.current) {
        console.log('Leaving queue due to navigation away from matchmaking page');
        leaveQueue().catch(err => {
          console.error('Failed to leave queue on unmount:', err);
        });
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Only run cleanup on unmount

  return (
    <PageContainer className="flex items-center justify-center">
      <div className="w-full max-w-2xl mx-auto px-4">
        <div className="text-center mb-8">
          <h1 className="text-[24px] font-[var(--font-pixel)] text-glow-cyan uppercase mb-4">
            Finding Match
          </h1>
          <p className="text-[12px] font-[var(--font-mono)] text-[var(--arcade-white)]">
            Searching for an opponent...
          </p>
        </div>

        {/* Joining Queue Loading */}
        {isJoining && !error && (
          <Card color="cyan" glow className="mb-6">
            <CardHeader>
              <CardTitle glow>Joining Queue...</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex justify-center py-8">
                <Loader size="large" />
              </div>
            </CardContent>
          </Card>
        )}

        {/* Error Display */}
        {error && (
          <Card color="red" glow className="mb-6">
            <CardHeader>
              <CardTitle>Connection Error</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-[12px] font-[var(--font-pixel)] text-[var(--arcade-red)] text-center">
                {error}
              </p>
              <p className="text-[10px] font-[var(--font-mono)] text-[var(--arcade-gray)] text-center">
                Make sure the backend server is running on port 8080
              </p>
              <div className="flex justify-center gap-3">
                <Button variant="primary" onClick={() => window.location.reload()}>
                  Retry
                </Button>
                <Button variant="secondary" onClick={() => navigate('/lobby')}>
                  Return to Lobby
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Match Found Message */}
        {matchFound && (
          <Card color="green" glow className="mb-6">
            <CardHeader>
              <CardTitle glow>Match Found!</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-[14px] font-[var(--font-pixel)] text-[var(--arcade-green)] text-center animate-pulse">
                Connecting to game...
              </p>
            </CardContent>
          </Card>
        )}

        {/* Queue Status Card */}
        {inQueue && !matchFound && (
          <Card color="cyan" glow className="mb-6">
            <CardHeader>
              <CardTitle glow>Queue Status</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Loading Animation */}
              <div className="flex justify-center py-8">
                <div className="relative">
                  <Loader size="large" />
                  <p className="text-[10px] font-[var(--font-pixel)] text-[var(--arcade-cyan)] text-center mt-4 animate-pulse">
                    Searching...
                  </p>
                </div>
              </div>

              {/* Queue Info */}
              {queueStatus && (
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 pixel-border border-[var(--arcade-cyan)] bg-[var(--arcade-dark-gray)]">
                    <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase mb-1 text-center">
                      Your Position
                    </p>
                    <p className="text-[18px] font-[var(--font-pixel)] text-[var(--arcade-cyan)] text-center">
                      {queueStatus.queuePosition}
                    </p>
                  </div>

                  <div className="p-3 pixel-border border-[var(--arcade-yellow)] bg-[var(--arcade-dark-gray)]">
                    <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase mb-1 text-center">
                      Players in Queue
                    </p>
                    <p className="text-[18px] font-[var(--font-pixel)] text-[var(--arcade-yellow)] text-center">
                      {queueStatus.queueSize}
                    </p>
                  </div>

                  {queueStatus.estimatedWaitSeconds > 0 && (
                    <div className="col-span-2 p-3 pixel-border border-[var(--arcade-pink)] bg-[var(--arcade-dark-gray)]">
                      <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase mb-1 text-center">
                        Estimated Wait Time
                      </p>
                      <p className="text-[14px] font-[var(--font-pixel)] text-[var(--arcade-pink)] text-center">
                        ~{queueStatus.estimatedWaitSeconds} seconds
                      </p>
                    </div>
                  )}
                </div>
              )}

              {/* Cancel Button */}
              <div className="flex justify-center pt-4">
                <Button
                  variant="danger"
                  onClick={handleCancelSearch}
                  disabled={loading}
                >
                  Cancel Search
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Instructions */}
        <Card color="yellow">
          <CardHeader>
            <CardTitle>What Happens Next?</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2 text-[10px]">
              <p>• You're now in the matchmaking queue</p>
              <p>• The system will pair you with another player</p>
              <p>• When a match is found, you'll be redirected to the game</p>
              <p>• Make sure to stay on this page while searching</p>
            </div>
          </CardContent>
        </Card>
      </div>
    </PageContainer>
  );
};
