import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import {
  ArrowLeftRight,
  LayoutDashboard,
  LogOut,
  Menu,
  PiggyBank,
  ShieldAlert,
  TrendingUp,
  Wallet,
  type LucideIcon,
} from 'lucide-react';
import { Button } from '../atoms/Button';
import { ROUTES } from '../../constants/routes';
import { useLogout } from '../../hooks/useAuth';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  activeClasses: string;
}

const NAV_ITEMS: NavItem[] = [
  { to: ROUTES.DASHBOARD, label: 'Dashboard', icon: LayoutDashboard, activeClasses: 'bg-primary/10 text-primary border-primary' },
  { to: ROUTES.LEDGER, label: 'Ledger', icon: Wallet, activeClasses: 'bg-ledger/10 text-ledger border-ledger' },
  { to: ROUTES.FX, label: 'FX', icon: ArrowLeftRight, activeClasses: 'bg-fx/10 text-fx border-fx' },
  { to: ROUTES.VAULTS, label: 'Vaults', icon: PiggyBank, activeClasses: 'bg-vaults/10 text-vaults border-vaults' },
  { to: ROUTES.RISK, label: 'Risk', icon: ShieldAlert, activeClasses: 'bg-risk/10 text-risk border-risk' },
  { to: ROUTES.WEALTH, label: 'Wealth', icon: TrendingUp, activeClasses: 'bg-wealth/10 text-wealth border-wealth' },
];

interface SidebarProps {
  mobileOpen: boolean;
  onMobileOpenChange: (open: boolean) => void;
}

export function Sidebar({ mobileOpen, onMobileOpenChange }: SidebarProps) {
  const navigate = useNavigate();
  const logout = useLogout();
  const [isOpen, setIsOpen] = useState(true);

  const handleLogout = () => {
    logout.mutate(undefined, {
      onSettled: () => navigate(ROUTES.LOGIN),
    });
  };

  const toggle = () => {
    setIsOpen((prev) => !prev);
    onMobileOpenChange(!mobileOpen);
  };

  // Below lg, the rail becomes an off-canvas drawer at fixed width, always showing
  // labels; the isOpen collapse (icon-only rail) is a desktop-only affordance.
  return (
    <nav
      className={`fixed inset-y-0 left-0 z-40 flex h-screen w-64 shrink-0 -translate-x-full flex-col border-r border-border bg-surface p-4 transition-transform duration-base ease-brand lg:static lg:translate-x-0 lg:transition-[width] ${
        mobileOpen ? 'translate-x-0' : ''
      } ${isOpen ? 'lg:w-60' : 'lg:w-20'}`}
    >
      <div className="mb-8 flex items-center justify-between gap-2">
        <Link
          to={ROUTES.DASHBOARD}
          className={`flex min-w-0 items-center text-lg font-extrabold tracking-[-0.03em] transition-opacity duration-fast hover:opacity-80 ${isOpen ? '' : 'lg:hidden'}`}
        >
          <span className="text-foreground">Manek</span>
          <span className="text-primary">Pay</span>
        </Link>
        <button
          type="button"
          onClick={toggle}
          aria-label={mobileOpen || isOpen ? 'Collapse sidebar' : 'Expand sidebar'}
          className={`flex items-center rounded-md px-3 py-2.5 text-muted transition-colors duration-fast hover:bg-surface-hover hover:text-foreground ${isOpen ? 'shrink-0' : 'lg:w-full'}`}
        >
          <Menu className="h-4 w-4 shrink-0" strokeWidth={2} aria-hidden="true" />
        </button>
      </div>
      <ul className="flex flex-1 flex-col gap-1">
        {NAV_ITEMS.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              end={item.to === ROUTES.DASHBOARD}
              title={item.label}
              onClick={() => onMobileOpenChange(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-md border-l-2 px-3 py-2.5 text-sm font-medium transition-colors duration-fast ${
                  isActive ? item.activeClasses : 'border-transparent text-muted hover:bg-surface-hover hover:text-foreground'
                }`
              }
            >
              <item.icon className="h-4 w-4 shrink-0" strokeWidth={2} aria-hidden="true" />
              <span className={isOpen ? '' : 'lg:hidden'}>{item.label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
      <Button
        variant="secondary"
        onClick={handleLogout}
        isLoading={logout.isPending}
        title="Log out"
        className="flex items-center justify-center gap-2"
      >
        <LogOut className="h-4 w-4" strokeWidth={2} aria-hidden="true" />
        <span className={isOpen ? '' : 'lg:hidden'}>Log out</span>
      </Button>
    </nav>
  );
}
