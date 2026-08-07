/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        background: 'rgb(var(--color-background) / <alpha-value>)',
        surface: 'rgb(var(--color-surface) / <alpha-value>)',
        'surface-hover': 'rgb(var(--color-surface-hover) / <alpha-value>)',
        foreground: 'rgb(var(--color-foreground) / <alpha-value>)',
        muted: 'rgb(var(--color-muted) / <alpha-value>)',
        border: 'rgb(var(--color-border) / <alpha-value>)',
        primary: 'rgb(var(--color-primary) / <alpha-value>)',
        success: 'rgb(var(--color-success) / <alpha-value>)',
        warning: 'rgb(var(--color-warning) / <alpha-value>)',
        danger: 'rgb(var(--color-danger) / <alpha-value>)',
        ledger: 'rgb(var(--color-ledger) / <alpha-value>)',
        fx: 'rgb(var(--color-fx) / <alpha-value>)',
        vaults: 'rgb(var(--color-vaults) / <alpha-value>)',
        risk: 'rgb(var(--color-risk) / <alpha-value>)',
        wealth: 'rgb(var(--color-wealth) / <alpha-value>)',
        'brand-start': 'rgb(var(--color-brand-start) / <alpha-value>)',
        'brand-end': 'rgb(var(--color-brand-end) / <alpha-value>)',
        'brand-foreground': 'rgb(var(--color-brand-foreground) / <alpha-value>)',
        'brand-muted': 'rgb(var(--color-brand-muted) / <alpha-value>)',
      },
      fontFamily: {
        sans: ['"Rethink Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        sm: '6px',
        md: '10px',
        lg: '16px',
      },
      boxShadow: {
        card: '0 1px 2px rgb(0 0 0 / 0.16), 0 1px 1px rgb(0 0 0 / 0.08)',
        glow: '0 0 0 1px rgb(var(--color-primary) / 0.4), 0 0 24px -6px rgb(var(--color-primary) / 0.5)',
      },
      transitionDuration: {
        fast: '120ms',
        base: '200ms',
        slow: '350ms',
      },
      transitionTimingFunction: {
        brand: 'cubic-bezier(0.4, 0, 0.2, 1)',
      },
    },
  },
  plugins: [],
};
