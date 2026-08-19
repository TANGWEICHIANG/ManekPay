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
        stamp: 'rgb(var(--color-stamp) / <alpha-value>)',
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
        // Every numeral, currency code, and account/reference number renders in this face -
        // the counting frame's rigid column grid depends on tabular, monospaced figures.
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      borderRadius: {
        // Rectilinear on purpose: rods and frames have definite edges, not soft plastic corners.
        sm: '2px',
        md: '3px',
        lg: '5px',
      },
      boxShadow: {
        // Hairline elevation, not soft blur - a counting frame's rods sit flush, they don't float.
        card: '0 0 0 1px rgb(var(--color-border))',
        glow: '0 0 0 1px rgb(var(--color-primary) / 0.5), 0 0 16px -6px rgb(var(--color-primary) / 0.6)',
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
