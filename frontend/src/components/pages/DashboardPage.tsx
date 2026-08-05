import { Link } from 'react-router-dom';
import { Card } from '../atoms/Card';
import { Badge } from '../atoms/Badge';
import { Button } from '../atoms/Button';
import { useMe } from '../../hooks/useAuth';
import { ROUTES } from '../../constants/routes';
import { KycStatus } from '../../constants/enums';

export function DashboardPage() {
  const { data: user, isLoading } = useMe();

  if (isLoading || !user) {
    return <p className="text-foreground">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-foreground">Welcome, {user.fullName}</h1>
      <Card className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <span className="text-foreground">{user.email}</span>
          <Badge status={user.kycStatus} />
        </div>
        {user.kycStatus !== KycStatus.APPROVED && (
          <Link to={ROUTES.KYC}>
            <Button>Complete KYC</Button>
          </Link>
        )}
      </Card>
    </div>
  );
}
