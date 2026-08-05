import type { ReactNode } from 'react';

interface AuthLayoutProps {
  title: string;
  children: ReactNode;
}

export function AuthLayout({ title, children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="w-full max-w-sm rounded border border-border p-8">
        <h1 className="mb-6 text-center text-2xl font-semibold text-foreground">{title}</h1>
        {children}
      </div>
    </div>
  );
}
