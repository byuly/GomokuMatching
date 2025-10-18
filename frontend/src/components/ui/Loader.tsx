/**
 * Loader Component - Retro arcade-style loading indicator
 */

import React from 'react';

interface LoaderProps {
  text?: string;
  size?: 'small' | 'medium' | 'large';
}

const sizeClasses = {
  small: 'w-8 h-8 text-[8px]',
  medium: 'w-16 h-16 text-[10px]',
  large: 'w-24 h-24 text-[12px]',
};

export const Loader: React.FC<LoaderProps> = ({ text = 'Loading...', size = 'medium' }) => {
  return (
    <div className="flex flex-col items-center justify-center gap-4 p-8">
      <div className={`${sizeClasses[size]} relative`}>
        <div className="absolute inset-0 border-4 border-[var(--arcade-cyan)] border-t-transparent animate-spin pixel-border" />
        <div className="absolute inset-2 border-4 border-[var(--arcade-pink)] border-b-transparent animate-spin pixel-border" style={{ animationDirection: 'reverse', animationDuration: '1.5s' }} />
      </div>
      <p className={`${sizeClasses[size].split(' ')[2]} font-[var(--font-pixel)] text-[var(--arcade-cyan)] text-glow-cyan uppercase blink`}>
        {text}
      </p>
    </div>
  );
};

export const FullScreenLoader: React.FC<LoaderProps> = ({ text }) => {
  return (
    <div className="fixed inset-0 bg-[var(--arcade-black)] flex items-center justify-center z-50 crt-effect scanlines">
      <Loader text={text} size="large" />
    </div>
  );
};
