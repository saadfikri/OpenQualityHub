import type { ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
}

const variants = {
  primary: 'bg-[var(--oqh-primary)] text-white hover:bg-[var(--oqh-primary-hover)] focus:ring-[var(--oqh-blue)]',
  secondary: 'bg-white text-[var(--oqh-text)] border border-[var(--oqh-border)] hover:bg-gray-50 focus:ring-[var(--oqh-blue)]',
  danger: 'bg-[var(--oqh-danger)] text-white hover:bg-red-700 focus:ring-red-500',
  ghost: 'text-[#48586E] hover:bg-gray-100 hover:text-[var(--oqh-text)]',
};

const sizes = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-sm',
  lg: 'px-6 py-3 text-base',
};

export function Button({ variant = 'primary', size = 'md', className, disabled, ...props }: ButtonProps) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors',
        'focus:outline-none focus:ring-2 focus:ring-offset-2',
        'disabled:opacity-50 disabled:cursor-not-allowed',
        variants[variant],
        sizes[size],
        className
      )}
      disabled={disabled}
      aria-disabled={disabled || undefined}
      {...props}
    />
  );
}
