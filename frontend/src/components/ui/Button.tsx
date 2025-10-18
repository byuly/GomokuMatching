/**
 * Button Component - Retro arcade-style button
 */

import React from 'react';

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'success';
export type ButtonSize = 'small' | 'medium' | 'large';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  glow?: boolean;
  pulse?: boolean;
  fullWidth?: boolean;
  children: React.ReactNode;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'text-[var(--arcade-cyan)] border-[var(--arcade-cyan)] hover:bg-[var(--arcade-cyan)] hover:text-[var(--arcade-black)]',
  secondary: 'text-[var(--arcade-pink)] border-[var(--arcade-pink)] hover:bg-[var(--arcade-pink)] hover:text-[var(--arcade-black)]',
  danger: 'text-[var(--arcade-red)] border-[var(--arcade-red)] hover:bg-[var(--arcade-red)] hover:text-[var(--arcade-black)]',
  success: 'text-[var(--arcade-green)] border-[var(--arcade-green)] hover:bg-[var(--arcade-green)] hover:text-[var(--arcade-black)]',
};

const sizeClasses: Record<ButtonSize, string> = {
  small: 'px-4 py-2 text-[8px]',
  medium: 'px-6 py-3 text-[10px]',
  large: 'px-8 py-4 text-[12px]',
};

export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'medium',
  glow = false,
  pulse = false,
  fullWidth = false,
  className = '',
  disabled = false,
  children,
  ...props
}) => {
  const baseClasses = 'font-[var(--font-pixel)] uppercase tracking-wider transition-all duration-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed';
  const borderClasses = glow ? 'pixel-border-glow' : 'pixel-border';
  const pulseClasses = pulse && !disabled ? 'arcade-pulse' : '';
  const widthClasses = fullWidth ? 'w-full' : '';

  return (
    <button
      className={`
        ${baseClasses}
        ${borderClasses}
        ${variantClasses[variant]}
        ${sizeClasses[size]}
        ${pulseClasses}
        ${widthClasses}
        ${className}
      `.trim().replace(/\s+/g, ' ')}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  );
};
