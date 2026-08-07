import type { ReactNode } from 'react';
import { Compass } from 'lucide-react';

interface AuthLayoutProps {
  title: string;
  children: ReactNode;
}

export function AuthLayout({ title, children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen bg-background">
      <div className="relative hidden w-1/2 flex-col justify-between overflow-hidden bg-gradient-to-br from-brand-start to-brand-end p-12 lg:flex">
        <svg
          className="pointer-events-none absolute inset-0 h-full w-full opacity-[0.15]"
          aria-hidden="true"
          preserveAspectRatio="none"
          viewBox="0 0 600 800"
        >
          <path
            d="M -50 650 C 120 550, 180 720, 320 600 S 520 380, 650 420"
            fill="none"
            stroke="rgb(var(--color-primary))"
            strokeWidth="1.5"
            strokeDasharray="2 10"
            strokeLinecap="round"
          />
          <path
            d="M -50 300 C 100 250, 220 420, 380 320 S 560 120, 680 180"
            fill="none"
            stroke="rgb(var(--color-primary))"
            strokeWidth="1.5"
            strokeDasharray="2 10"
            strokeLinecap="round"
          />
        </svg>
        <div className="relative flex items-center gap-2">
          <Compass className="h-7 w-7 text-primary" strokeWidth={2.25} aria-hidden="true" />
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
            <Compass className="h-6 w-6 text-primary" strokeWidth={2.25} aria-hidden="true" />
            <span className="text-lg font-bold tracking-tight text-foreground">ManekPay</span>
          </div>
          <h1 className="mb-6 text-2xl font-bold text-foreground">{title}</h1>
          {children}
        </div>
      </div>
    </div>
  );
}
