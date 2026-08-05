import { NavLink, useNavigate } from 'react-router-dom';
import { Button } from '../atoms/Button';
import { ROUTES } from '../../constants/routes';
import { useLogout } from '../../hooks/useAuth';

const NAV_ITEMS = [
  { to: ROUTES.DASHBOARD, label: 'Dashboard' },
  { to: ROUTES.LEDGER, label: 'Ledger' },
  { to: ROUTES.FX, label: 'FX' },
  { to: ROUTES.VAULTS, label: 'Vaults' },
  { to: ROUTES.RISK, label: 'Risk' },
  { to: ROUTES.WEALTH, label: 'Wealth' },
];

export function Sidebar() {
  const navigate = useNavigate();
  const logout = useLogout();

  const handleLogout = () => {
    logout.mutate(undefined, {
      onSettled: () => navigate(ROUTES.LOGIN),
    });
  };

  return (
    <nav className="flex h-screen w-56 flex-col border-r border-border p-4">
      <span className="mb-6 text-lg font-semibold text-foreground">ManekPay</span>
      <ul className="flex flex-1 flex-col gap-1">
        {NAV_ITEMS.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              end={item.to === ROUTES.DASHBOARD}
              className={({ isActive }) =>
                `block rounded px-3 py-2 text-sm ${isActive ? 'bg-primary text-white' : 'text-foreground hover:bg-border/30'}`
              }
            >
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
      <Button variant="secondary" onClick={handleLogout} isLoading={logout.isPending}>
        Log out
      </Button>
    </nav>
  );
}
