/**
 * Modal Component - Retro arcade-style modal dialog
 */

import React, { useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from './Card';
import { Button } from './Button';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  showCloseButton?: boolean;
  closeOnBackdrop?: boolean;
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
  showCloseButton = true,
  closeOnBackdrop = true,
}) => {
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (closeOnBackdrop && e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-80 flex items-center justify-center z-50 p-4 crt-effect"
      onClick={handleBackdropClick}
    >
      <div className="w-full max-w-md animate-[arcade-pulse_0.3s_ease-out]">
        <Card glow color="pink">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle glow>{title}</CardTitle>
              {showCloseButton && (
                <button
                  onClick={onClose}
                  className="text-[var(--arcade-pink)] hover:text-[var(--arcade-cyan)] text-[16px] font-[var(--font-pixel)] transition-colors"
                  aria-label="Close modal"
                >
                  X
                </button>
              )}
            </div>
          </CardHeader>
          <CardContent>{children}</CardContent>
        </Card>
      </div>
    </div>
  );
};

export const ModalFooter: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <div className="mt-6 pt-4 border-t-2 border-[var(--arcade-gray)] flex gap-4 justify-end">
      {children}
    </div>
  );
};
