import type { ButtonHTMLAttributes } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  isLoading?: boolean;
}

const VARIANT_CLASSES: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary: 'bg-primary text-white hover:opacity-90',
  secondary: 'bg-transparent text-foreground border border-border hover:bg-border/20',
  danger: 'bg-danger text-white hover:opacity-90',
};

export function Button({ variant = 'primary', isLoading, disabled, children, className = '', ...rest }: ButtonProps) {
  return (
    <button
      className={`rounded px-4 py-2 font-medium transition disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 ${VARIANT_CLASSES[variant]} ${className}`}
      disabled={disabled || isLoading}
      {...rest}
    >
      {isLoading ? 'Loading…' : children}
    </button>
  );
}
