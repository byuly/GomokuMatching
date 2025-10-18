/**
 * Input Component - Retro arcade-style text input
 */

import React, { forwardRef } from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  glow?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, glow = false, className = '', ...props }, ref) => {
    const borderClasses = glow ? 'pixel-border-glow' : 'pixel-border';

    return (
      <div className="w-full">
        {label && (
          <label className="block text-[10px] font-[var(--font-pixel)] text-[var(--arcade-cyan)] mb-2 uppercase">
            {label}
          </label>
        )}
        <input
          ref={ref}
          className={`
            w-full
            px-4 py-3
            bg-[var(--arcade-dark-gray)]
            text-[var(--arcade-white)]
            text-[12px]
            font-[var(--font-mono)]
            ${borderClasses}
            border-[var(--arcade-cyan)]
            focus:outline-none
            focus:border-[var(--arcade-pink)]
            focus:shadow-[0_0_20px_var(--arcade-pink)]
            disabled:opacity-50
            disabled:cursor-not-allowed
            placeholder:text-[var(--arcade-light-gray)]
            ${className}
          `.trim().replace(/\s+/g, ' ')}
          {...props}
        />
        {error && (
          <p className="mt-2 text-[8px] font-[var(--font-pixel)] text-[var(--arcade-red)] text-glow-red">
            {error}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
