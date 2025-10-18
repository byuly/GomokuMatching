/**
 * Authentication Service - User authentication and token management
 */

import { api, apiService } from './api.service';
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  User,
  RefreshTokenRequest,
  RefreshTokenResponse,
} from '@/types';

class AuthService {
  /**
   * Register a new user
   */
  async register(data: RegisterRequest): Promise<User> {
    const response = await api.post<AuthResponse>('/api/auth/register', data);

    // Store tokens and user info
    apiService.setTokens(response.data.accessToken, response.data.refreshToken);
    localStorage.setItem('userId', response.data.userId);
    localStorage.setItem('username', response.data.username);
    localStorage.setItem('email', response.data.email);

    return {
      id: response.data.userId,
      username: response.data.username,
      email: response.data.email,
    };
  }

  /**
   * Login with username/email and password
   */
  async login(data: LoginRequest): Promise<User> {
    const response = await api.post<AuthResponse>('/api/auth/login', data);

    // Store tokens and user info
    apiService.setTokens(response.data.accessToken, response.data.refreshToken);
    localStorage.setItem('userId', response.data.userId);
    localStorage.setItem('username', response.data.username);
    localStorage.setItem('email', response.data.email);

    return {
      id: response.data.userId,
      username: response.data.username,
      email: response.data.email,
    };
  }

  /**
   * Logout - Clear tokens
   */
  logout(): void {
    apiService.clearTokens();
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('email');
  }

  /**
   * Refresh access token using refresh token
   */
  async refreshToken(refreshToken: string): Promise<RefreshTokenResponse> {
    const response = await api.post<RefreshTokenResponse>('/api/auth/refresh', {
      refreshToken,
    } as RefreshTokenRequest);

    // Update stored tokens
    apiService.setTokens(response.data.accessToken, response.data.refreshToken);

    return response.data;
  }

  /**
   * Get current user from localStorage
   * Note: User info is stored during login/register
   */
  getCurrentUser(): User | null {
    const userId = localStorage.getItem('userId');
    const username = localStorage.getItem('username');
    const email = localStorage.getItem('email');

    if (!userId || !username) {
      return null;
    }

    return {
      id: userId,
      username: username,
      email: email || '',
    };
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    const token = localStorage.getItem('accessToken');
    if (!token) return false;

    try {
      // Check if token is expired
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );

      const payload = JSON.parse(jsonPayload);
      const expirationTime = payload.exp * 1000; // Convert to milliseconds

      return Date.now() < expirationTime;
    } catch (error) {
      console.error('Error checking authentication:', error);
      return false;
    }
  }

  /**
   * Get access token from storage
   */
  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  /**
   * Get refresh token from storage
   */
  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }
}

// Export singleton instance
export const authService = new AuthService();
