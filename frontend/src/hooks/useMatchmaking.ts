/**
 * useMatchmaking Hook - Matchmaking queue management
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { matchmakingService } from '@/services';
import { useWebSocket } from '@/context';
import type {
  QueueStatusResponse,
  MatchFoundNotification,
} from '@/types';
import { getUserFriendlyError, logError } from '@/utils';

interface UseMatchmakingReturn {
  queueStatus: QueueStatusResponse | null;
  loading: boolean;
  error: string | null;
  inQueue: boolean;
  joinQueue: () => Promise<void>;
  leaveQueue: () => Promise<void>;
  onMatchFound: (callback: (notification: MatchFoundNotification) => void) => void;
  clearError: () => void;
}

export const useMatchmaking = (): UseMatchmakingReturn => {
  const [queueStatus, setQueueStatus] = useState<QueueStatusResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [inQueue, setInQueue] = useState<boolean>(false);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const matchFoundCallbackRef = useRef<((notification: MatchFoundNotification) => void) | null>(null);
  const isSubscribedRef = useRef<boolean>(false);

  const { subscribeToMatchmaking, unsubscribeFromMatchmaking, connected } = useWebSocket();

  // Subscribe to matchmaking WebSocket as soon as connected (BEFORE joining queue)
  // This ensures we don't miss rapid match notifications
  useEffect(() => {
    if (connected && !isSubscribedRef.current) {
      console.log('[useMatchmaking] WebSocket connected, subscribing to match notifications...');
      isSubscribedRef.current = true;

      subscribeToMatchmaking((notification: MatchFoundNotification) => {
        console.log('[useMatchmaking] ✅ Match found callback triggered!', notification);

        // Stop polling
        if (pollIntervalRef.current) {
          matchmakingService.stopPolling(pollIntervalRef.current);
          pollIntervalRef.current = null;
        }

        // Update status
        setInQueue(false);

        // Call user callback
        if (matchFoundCallbackRef.current) {
          console.log('[useMatchmaking] Calling user callback...');
          matchFoundCallbackRef.current(notification);
        } else {
          console.error('[useMatchmaking] ❌ No user callback registered!');
        }
      });
    }

    // Only unsubscribe on unmount
    return () => {
      if (isSubscribedRef.current) {
        console.log('[useMatchmaking] Component unmounting, unsubscribing...');
        unsubscribeFromMatchmaking();
        isSubscribedRef.current = false;
      }
    };
  }, [connected, subscribeToMatchmaking, unsubscribeFromMatchmaking]);

  // Clean up polling on unmount
  useEffect(() => {
    return () => {
      if (pollIntervalRef.current) {
        matchmakingService.stopPolling(pollIntervalRef.current);
      }
    };
  }, []);

  const joinQueue = useCallback(async (): Promise<void> => {
    console.log('[useMatchmaking] Joining queue...');
    setLoading(true);
    setError(null);

    try {
      const response = await matchmakingService.joinQueue();
      console.log('[useMatchmaking] Join queue response:', response);
      setQueueStatus(response);

      // Set inQueue to true for both JOINED and ALREADY_IN_QUEUE status
      if (response.status === 'JOINED' || response.status === 'ALREADY_IN_QUEUE') {
        setInQueue(true);
        console.log('[useMatchmaking] ✅ In queue, waiting for match...');
      }

      // Don't poll - rely on WebSocket for match notification
      // Status polling can be added later if needed
    } catch (err) {
      console.error('[useMatchmaking] ❌ Failed to join queue:', err);
      const errorMessage = getUserFriendlyError(err);
      setError(errorMessage);
      logError(err, 'Join queue');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const leaveQueue = useCallback(async (): Promise<void> => {
    console.log('[useMatchmaking] Leaving queue...');
    setLoading(true);
    setError(null);

    try {
      await matchmakingService.leaveQueue();
      setInQueue(false);
      setQueueStatus(null);
      console.log('[useMatchmaking] ✅ Left queue');

      // Stop polling
      if (pollIntervalRef.current) {
        matchmakingService.stopPolling(pollIntervalRef.current);
        pollIntervalRef.current = null;
      }

      // Don't unsubscribe - keep WebSocket subscription active for next queue join
    } catch (err) {
      console.error('[useMatchmaking] ❌ Failed to leave queue:', err);
      const errorMessage = getUserFriendlyError(err);
      setError(errorMessage);
      logError(err, 'Leave queue');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const onMatchFound = useCallback((callback: (notification: MatchFoundNotification) => void): void => {
    matchFoundCallbackRef.current = callback;
  }, []);

  const clearError = (): void => {
    setError(null);
  };

  return {
    queueStatus,
    loading,
    error,
    inQueue,
    joinQueue,
    leaveQueue,
    onMatchFound,
    clearError,
  };
};
