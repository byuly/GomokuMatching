/**
 * Header Component - Top navigation bar
 */

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context';
import { Button } from '../ui/Button';

export const Header: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogoClick = () => {
    if (isAuthenticated) {
      navigate('/lobby');
    }
  };

  return (
    <header className="w-full bg-[var(--arcade-dark-gray)] border-b-4 border-[var(--arcade-cyan)] px-6 py-4">
      <div className="max-w-7xl mx-auto flex items-center justify-between">
        {/* Logo */}
        <div className="flex items-center gap-4">
          <h1
            className="text-[16px] font-[var(--font-pixel)] text-glow-cyan uppercase cursor-pointer hover:text-[var(--arcade-white)] transition-colors"
            onClick={handleLogoClick}
          >
            Gomoku
          </h1>
          <div className="hidden sm:block text-[8px] font-[var(--font-pixel)] text-[var(--arcade-yellow)]">
            Five in a Row
          </div>
        </div>

        {/* User Info */}
        {isAuthenticated && user && (
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase">
                Player
              </p>
              <p className="text-[12px] font-[var(--font-pixel)] text-[var(--arcade-pink)]">
                {user.username}
              </p>
            </div>
            <Button variant="danger" size="small" onClick={logout}>
              Logout
            </Button>
          </div>
        )}
      </div>
    </header>
  );
};
