/**
 * Container Component - Main content wrapper with CRT effect
 */

import React from 'react';

interface ContainerProps {
  children: React.ReactNode;
  className?: string;
}

export const Container: React.FC<ContainerProps> = ({ children, className = '' }) => {
  return (
    <main className={`min-h-screen flex flex-col crt-effect scanlines ${className}`}>
      {children}
    </main>
  );
};

export const PageContainer: React.FC<ContainerProps> = ({ children, className = '' }) => {
  return (
    <div className={`flex-1 w-full max-w-7xl mx-auto px-6 py-8 ${className}`}>
      {children}
    </div>
  );
};
