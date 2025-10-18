/**
 * Matchmaking Service - Queue management for PvP matchmaking
 */

import { api } from './api.service';
import type {
  QueueResponse,
  QueueStatusResponse,
  LeaveQueueResponse,
} from '@/types';

class MatchmakingService {
  /**
   * Join the matchmaking queue
   */
  async joinQueue(): Promise<QueueResponse> {
    const response = await api.post<QueueResponse>('/api/matchmaking/queue', {});
    return response.data;
  }

  /**
   * Leave the matchmaking queue
   */
  async leaveQueue(): Promise<LeaveQueueResponse> {
    const response = await api.delete<LeaveQueueResponse>('/api/matchmaking/queue');
    return response.data;
  }

  /**
   * Get current queue status
   */
  async getQueueStatus(): Promise<QueueStatusResponse> {
    const response = await api.get<QueueStatusResponse>('/api/matchmaking/status');
    return response.data;
  }

  /**
   * Poll queue status (for estimated wait time updates)
   */
  startPolling(
    callback: (status: QueueStatusResponse) => void,
    interval: number = 5000
  ): NodeJS.Timeout {
    let isPolling = false;

    const pollInterval = setInterval(async () => {
      // Skip if previous request is still in progress
      if (isPolling) {
        console.log('Skipping poll - previous request still in progress');
        return;
      }

      try {
        isPolling = true;

        // Add timeout to prevent hanging requests
        const timeoutPromise = new Promise((_, reject) => {
          setTimeout(() => reject(new Error('Polling request timeout')), 10000);
        });

        const statusPromise = this.getQueueStatus();
        const status = await Promise.race([statusPromise, timeoutPromise]) as QueueStatusResponse;

        callback(status);
      } catch (error) {
        console.error('Error polling queue status:', error);
        // Reset polling flag even on error
      } finally {
        isPolling = false;
      }
    }, interval);

    return pollInterval;
  }

  /**
   * Stop polling queue status
   */
  stopPolling(pollInterval: NodeJS.Timeout): void {
    clearInterval(pollInterval);
  }
}

// Export singleton instance
export const matchmakingService = new MatchmakingService();
