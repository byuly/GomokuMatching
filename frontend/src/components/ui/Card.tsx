/**
 * Card Component - Retro arcade-style container
 */

import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  glow?: boolean;
  color?: 'cyan' | 'pink' | 'yellow' | 'green';
}

const colorClasses: Record<string, string> = {
  cyan: 'border-[var(--arcade-cyan)]',
  pink: 'border-[var(--arcade-pink)]',
  yellow: 'border-[var(--arcade-yellow)]',
  green: 'border-[var(--arcade-green)]',
};

export const Card: React.FC<CardProps> = ({
  children,
  className = '',
  glow = false,
  color = 'cyan',
}) => {
  const borderClasses = glow ? 'pixel-border-glow' : 'pixel-border';

  return (
    <div
      className={`
        bg-[var(--arcade-dark-gray)]
        p-6
        ${borderClasses}
        ${colorClasses[color]}
        ${className}
      `.trim().replace(/\s+/g, ' ')}
    >
      {children}
    </div>
  );
};

export const CardHeader: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className = '',
}) => {
  return (
    <div className={`mb-4 pb-4 border-b-2 border-[var(--arcade-gray)] ${className}`}>
      {children}
    </div>
  );
};

export const CardTitle: React.FC<{ children: React.ReactNode; className?: string; glow?: boolean }> = ({
  children,
  className = '',
  glow = false,
}) => {
  return (
    <h2
      className={`
        text-[14px]
        font-[var(--font-pixel)]
        uppercase
        ${glow ? 'text-glow-cyan' : 'text-[var(--arcade-cyan)]'}
        ${className}
      `.trim().replace(/\s+/g, ' ')}
    >
      {children}
    </h2>
  );
};

export const CardContent: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className = '',
}) => {
  return <div className={`font-[var(--font-mono)] text-[14px] ${className}`}>{children}</div>;
};
