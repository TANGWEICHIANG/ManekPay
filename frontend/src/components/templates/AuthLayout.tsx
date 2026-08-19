import type { ReactNode } from 'react';
import { Columns4 } from 'lucide-react';

interface AuthLayoutProps {
  title: string;
  children: ReactNode;
}

export function AuthLayout({ title, children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen bg-background">
      <div className="relative hidden w-1/2 flex-col justify-between overflow-hidden bg-gradient-to-br from-brand-start to-brand-end p-12 lg:flex">
        {/* The frame's rods, faint and evenly spaced - the same rhythm the dashboard's wallet
            rods use, established here as the brand's first mark rather than an abstract flourish. */}
        <svg
          className="pointer-events-none absolute inset-0 h-full w-full opacity-[0.1]"
          aria-hidden="true"
          preserveAspectRatio="none"
          viewBox="0 0 600 800"
        >
          {Array.from({ length: 7 }, (_, i) => (
            <line
              key={i}
              x1={40 + i * 90}
              y1="60"
              x2={40 + i * 90}
              y2="740"
              stroke="rgb(var(--color-primary))"
              strokeWidth="1.5"
            />
          ))}
          {Array.from({ length: 7 }, (_, i) => (
            <circle key={`bead-${i}`} cx={40 + i * 90} cy={220 + (i % 3) * 140} r="7" fill="rgb(var(--color-primary))" />
          ))}
        </svg>
        <div className="relative flex items-center gap-2">
          <Columns4 className="h-7 w-7 text-primary" strokeWidth={2.25} aria-hidden="true" />
          <span className="text-xl font-bold tracking-tight text-brand-foreground">ManekPay</span>
        </div>
        <div className="relative flex flex-col gap-4">
          <p className="text-4xl font-bold leading-tight text-brand-foreground">
            One account.
            <br />
            Every currency.
            <br />
            Every crossing.
          </p>
          <p className="max-w-sm text-sm text-brand-muted">
            Multi-currency wallets, live FX, and behavioral savings vaults — built for money that
            moves.
          </p>
        </div>
      </div>
      <div className="flex w-full flex-1 items-center justify-center px-4 py-12 lg:w-1/2">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2 lg:hidden">
            <Columns4 className="h-6 w-6 text-primary" strokeWidth={2.25} aria-hidden="true" />
            <span className="text-lg font-bold tracking-tight text-foreground">ManekPay</span>
          </div>
          <h1 className="mb-6 text-2xl font-bold text-foreground">{title}</h1>
          {children}
        </div>
      </div>
    </div>
  );
}
