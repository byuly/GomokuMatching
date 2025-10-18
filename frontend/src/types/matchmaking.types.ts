/**
 * Matchmaking Types
 */

export enum MatchmakingStatus {
  QUEUED = 'QUEUED',
  MATCHED = 'MATCHED',
  NOT_IN_QUEUE = 'NOT_IN_QUEUE'
}

export interface QueueRequest {
  // Empty body for queue request
}

export interface QueueResponse {
  status: string; // "JOINED" or "ALREADY_IN_QUEUE"
  queuePosition: number;
  queueSize: number;
  estimatedWaitSeconds: number;
  message: string;
}

export interface QueueStatusResponse {
  status: string; // "IN_QUEUE" or "NOT_IN_QUEUE"
  queuePosition: number;
  queueSize: number;
  estimatedWaitSeconds: number;
  message: string;
}

export interface MatchFoundNotification {
  gameId: string; // UUID
  playerNumber: number; // 1 or 2
  opponentId: string; // UUID
  websocketTopic: string;
}

export interface LeaveQueueResponse {
  status: string; // "LEFT" or "NOT_IN_QUEUE"
  message: string;
}
