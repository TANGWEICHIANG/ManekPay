import { Link } from 'react-router-dom';
import { ArrowLeftRight, ArrowRight, PiggyBank, ShieldAlert, TrendingUp, Wallet, type LucideIcon } from 'lucide-react';
import { Card } from '../atoms/Card';
import { Badge } from '../atoms/Badge';
import { Button } from '../atoms/Button';
import { useMe } from '../../hooks/useAuth';
import { ROUTES } from '../../constants/routes';
import { KycStatus } from '../../constants/enums';

interface ModuleLink {
  to: string;
  label: string;
  description: string;
  icon: LucideIcon;
  iconWrapClasses: string;
  hoverBorderClass: string;
}

const MODULES: ModuleLink[] = [
  {
    to: ROUTES.LEDGER,
    label: 'Ledger',
    description: 'Wallets & transfers',
    icon: Wallet,
    iconWrapClasses: 'text-ledger bg-ledger/10',
    hoverBorderClass: 'hover:border-ledger/50',
  },
  {
    to: ROUTES.FX,
    label: 'FX',
    description: 'Live rates & locks',
    icon: ArrowLeftRight,
    iconWrapClasses: 'text-fx bg-fx/10',
    hoverBorderClass: 'hover:border-fx/50',
  },
  {
    to: ROUTES.VAULTS,
    label: 'Vaults',
    description: 'Spare-change savings',
    icon: PiggyBank,
    iconWrapClasses: 'text-vaults bg-vaults/10',
    hoverBorderClass: 'hover:border-vaults/50',
  },
  {
    to: ROUTES.RISK,
    label: 'Risk',
    description: 'Account protection',
    icon: ShieldAlert,
    iconWrapClasses: 'text-risk bg-risk/10',
    hoverBorderClass: 'hover:border-risk/50',
  },
  {
    to: ROUTES.WEALTH,
    label: 'Wealth',
    description: 'Fractional investing',
    icon: TrendingUp,
    iconWrapClasses: 'text-wealth bg-wealth/10',
    hoverBorderClass: 'hover:border-wealth/50',
  },
];

export function DashboardPage() {
  const { data: user, isLoading } = useMe();

  if (isLoading || !user) {
    return <p className="text-foreground">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-8">
      <div>
        <p className="text-sm font-medium text-muted">Welcome back</p>
        <h1 className="text-3xl font-bold text-foreground">{user.fullName}</h1>
      </div>

      <Card className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-col gap-1">
          <span className="text-sm text-muted">Account</span>
          <span className="text-foreground">{user.email}</span>
        </div>
        <div className="flex items-center gap-3">
          <Badge status={user.kycStatus} />
          {user.kycStatus !== KycStatus.APPROVED && (
            <Link to={ROUTES.KYC}>
              <Button>Complete KYC</Button>
            </Link>
          )}
        </div>
      </Card>

      <div>
        <h2 className="mb-3 text-lg font-semibold text-foreground">Modules</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {MODULES.map((mod) => (
            <Link key={mod.to} to={mod.to}>
              <Card
                className={`group flex h-full flex-col gap-4 transition-all duration-base ease-brand hover:-translate-y-0.5 ${mod.hoverBorderClass}`}
              >
                <div className={`flex h-10 w-10 items-center justify-center rounded-md ${mod.iconWrapClasses}`}>
                  <mod.icon className="h-5 w-5" strokeWidth={2} aria-hidden="true" />
                </div>
                <div className="flex flex-col gap-1">
                  <span className="font-semibold text-foreground">{mod.label}</span>
                  <span className="text-sm text-muted">{mod.description}</span>
                </div>
                <span className="mt-auto flex items-center gap-1 text-sm font-medium text-muted transition-colors duration-fast group-hover:text-foreground">
                  Open
                  <ArrowRight className="h-3.5 w-3.5" strokeWidth={2} aria-hidden="true" />
                </span>
              </Card>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
