/**
 * Footer Component - Bottom info bar
 */

import React from 'react';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full bg-[var(--arcade-dark-gray)] border-t-4 border-[var(--arcade-cyan)] px-6 py-4 mt-auto">
      <div className="max-w-7xl mx-auto text-center">
        <p className="text-[8px] font-[var(--font-pixel)] text-[var(--arcade-gray)] uppercase">
          Gomoku Matching System v1.0.0
        </p>
        <p className="text-[6px] font-[var(--font-mono)] text-[var(--arcade-gray)] mt-1">
          Press START to begin
        </p>
      </div>
    </footer>
  );
};
