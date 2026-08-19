import { Link } from 'react-router-dom';
import { ArrowLeftRight, ArrowRight, PiggyBank, ShieldAlert, TrendingUp, Wallet, type LucideIcon } from 'lucide-react';
import { Badge } from '../atoms/Badge';
import { Button } from '../atoms/Button';
import { BeadRow } from '../atoms/BeadRow';
import { useMe } from '../../hooks/useAuth';
import { useMyAccount } from '../../hooks/useLedger';
import { ROUTES } from '../../constants/routes';
import { KycStatus } from '../../constants/enums';

interface ModuleLink {
  to: string;
  label: string;
  icon: LucideIcon;
  accentClass: string;
}

const MODULES: ModuleLink[] = [
  { to: ROUTES.LEDGER, label: 'Ledger', icon: Wallet, accentClass: 'text-ledger' },
  { to: ROUTES.FX, label: 'FX', icon: ArrowLeftRight, accentClass: 'text-fx' },
  { to: ROUTES.VAULTS, label: 'Vaults', icon: PiggyBank, accentClass: 'text-vaults' },
  { to: ROUTES.RISK, label: 'Risk', icon: ShieldAlert, accentClass: 'text-risk' },
  { to: ROUTES.WEALTH, label: 'Wealth', icon: TrendingUp, accentClass: 'text-wealth' },
];

export function DashboardPage() {
  const { data: user, isLoading: userLoading } = useMe();
  const { data: account, isLoading: accountLoading } = useMyAccount();

  if (userLoading || accountLoading || !user) {
    return <p className="text-foreground">Loading…</p>;
  }

  const totalBalance = account?.wallets.reduce((sum, w) => sum + w.balance, 0) ?? 0;

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-4 border-b border-border pb-6 sm:flex-row sm:items-end sm:justify-between">
        <h1 className="text-3xl font-bold text-foreground">Welcome back, {user.fullName}</h1>
        <div className="flex items-center gap-3">
          <Badge status={user.kycStatus} />
          {user.kycStatus !== KycStatus.APPROVED && (
            <Link to={ROUTES.KYC}>
              <Button>Complete KYC</Button>
            </Link>
          )}
        </div>
      </div>

      {account && (
        <div>
          <h2 className="mb-3 text-lg font-medium text-foreground">Wallets</h2>
          {/* The frame: one rod per currency. Balance is the fact, in tabular mono numerals;
              the bead row beneath is the wallet's real share of total balance across currencies
              - a genuine fraction, not a decorative flourish. */}
          <div className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border bg-border sm:grid-cols-3 lg:grid-cols-5">
            {account.wallets.map((wallet) => (
              <div key={wallet.currency} className="flex flex-col gap-3 bg-surface p-4">
                <span className="h-1.5 w-1.5 rounded-full bg-primary" aria-hidden="true" />
                <span className="font-mono text-xs uppercase tracking-wider text-muted">{wallet.currency}</span>
                <span className="font-mono text-xl font-medium tabular-nums text-foreground">{wallet.balance.toFixed(2)}</span>
                <BeadRow
                  fraction={totalBalance > 0 ? wallet.balance / totalBalance : 0}
                  colorClass="bg-primary"
                />
              </div>
            ))}
          </div>
        </div>
      )}

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Modules</h2>
        {/* Quick actions as one rod of function keys rather than a card grid. */}
        <div className="flex flex-col divide-y divide-border overflow-hidden rounded-md border border-border bg-surface sm:flex-row sm:divide-x sm:divide-y-0">
          {MODULES.map((mod) => (
            <Link
              key={mod.to}
              to={mod.to}
              className="group flex flex-1 items-center gap-3 p-4 transition-colors duration-fast hover:bg-surface-hover"
            >
              <mod.icon className={`h-5 w-5 shrink-0 ${mod.accentClass}`} strokeWidth={2} aria-hidden="true" />
              <span className="font-semibold text-foreground">{mod.label}</span>
              <ArrowRight
                className="ml-auto h-4 w-4 shrink-0 text-muted transition-transform duration-fast group-hover:translate-x-0.5 group-hover:text-foreground"
                strokeWidth={2}
                aria-hidden="true"
              />
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
