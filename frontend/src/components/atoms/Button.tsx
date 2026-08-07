import type { ButtonHTMLAttributes } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  isLoading?: boolean;
}

const VARIANT_CLASSES: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary: 'bg-primary text-background hover:shadow-glow',
  secondary: 'bg-transparent text-foreground border border-border hover:bg-surface-hover hover:border-primary/40',
  danger: 'bg-danger text-background hover:shadow-glow',
};

export function Button({ variant = 'primary', isLoading, disabled, children, className = '', ...rest }: ButtonProps) {
  return (
    <button
      className={`rounded-md px-4 py-2 font-semibold transition-all duration-base ease-brand active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100 ${VARIANT_CLASSES[variant]} ${className}`}
      disabled={disabled || isLoading}
      {...rest}
    >
      {isLoading ? 'Loading…' : children}
    </button>
  );
}
