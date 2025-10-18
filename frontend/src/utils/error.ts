/**
 * Error Handling Utilities
 */

import type { ApiError } from '@/types';

/**
 * Map API error messages to user-friendly messages
 */
export const getUserFriendlyError = (error: ApiError | unknown): string => {
  if (!error) return 'An unexpected error occurred';

  const apiError = error as ApiError;

  // Map specific error messages
  const errorMap: Record<string, string> = {
    'Username already exists': 'This username is already taken',
    'Email already registered': 'This email is already registered',
    'Invalid credentials': 'Invalid username/email or password',
    'User not found': 'User not found',
    'Invalid token': 'Your session has expired. Please log in again',
    'Token expired': 'Your session has expired. Please log in again',
    'Game not found': 'Game not found',
    'Not your turn': "It's not your turn",
    'Invalid move': 'Invalid move',
    'Cell already occupied': 'This cell is already occupied',
    'Game already finished': 'This game has already finished',
    'Already in queue': 'You are already in the matchmaking queue',
    'Not in queue': 'You are not in the matchmaking queue',
  };

  if (apiError.message && errorMap[apiError.message]) {
    return errorMap[apiError.message];
  }

  // Check for common HTTP status codes
  if (apiError.status) {
    switch (apiError.status) {
      case 400:
        return 'Invalid request. Please check your input';
      case 401:
        return 'You need to log in to access this';
      case 403:
        return 'You do not have permission to do this';
      case 404:
        return 'The requested resource was not found';
      case 409:
        return 'A conflict occurred. Please try again';
      case 500:
        return 'Server error. Please try again later';
      default:
        return apiError.message || 'An error occurred';
    }
  }

  // If we have a message, use it
  if (apiError.message) {
    return apiError.message;
  }

  return 'An unexpected error occurred';
};

/**
 * Log error to console (in development) and error tracking service (in production)
 */
export const logError = (error: unknown, context?: string): void => {
  if (import.meta.env.DEV) {
    console.error(`[Error${context ? ` - ${context}` : ''}]:`, error);
  } else {
    // In production, send to error tracking service (e.g., Sentry)
    // Example: Sentry.captureException(error);
    console.error('Error occurred:', error);
  }
};

/**
 * Create a promise that rejects after a timeout
 */
export const createTimeout = (ms: number): Promise<never> => {
  return new Promise((_, reject) => {
    setTimeout(() => reject(new Error('Request timeout')), ms);
  });
};

/**
 * Retry a promise with exponential backoff
 */
export const retryWithBackoff = async <T>(
  fn: () => Promise<T>,
  maxRetries: number = 3,
  baseDelay: number = 1000
): Promise<T> => {
  let lastError: unknown;

  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      if (i < maxRetries - 1) {
        const delay = baseDelay * Math.pow(2, i);
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
    }
  }

  throw lastError;
};
