import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

let inputCounter = 0;

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, className, id, ...props }, ref) => {
    const inputId = id || `input-${++inputCounter}`;
    const errorId = error ? `${inputId}-error` : undefined;
    return (
      <div>
        {label && (
          <label htmlFor={inputId} className="block text-sm font-medium text-[var(--oqh-text)] mb-1">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={errorId}
          className={cn(
            'block w-full rounded-lg border border-[var(--oqh-border)] px-3 py-2 text-sm',
            'placeholder:text-[#9BA8B9]',
            'focus:border-[var(--oqh-blue)] focus:outline-none focus:ring-1 focus:ring-[var(--oqh-blue)]',
            error && 'border-red-500 focus:border-red-500 focus:ring-red-500',
            className
          )}
          {...props}
        />
        {error && <p id={errorId} className="mt-1 text-sm text-red-600" role="alert">{error}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';
