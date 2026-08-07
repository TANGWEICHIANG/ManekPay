import { ShieldAlert } from 'lucide-react';
import { Card } from '../atoms/Card';
import { PageHeader } from '../atoms/PageHeader';
import { useMyFlags } from '../../hooks/useRisk';

export function RiskPage() {
  const { data, isLoading, isError } = useMyFlags();

  if (isLoading) {
    return <p className="text-foreground">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader icon={ShieldAlert} iconClasses="bg-risk/10 text-risk" title="Risk" />
      {isError ? (
        <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
          Could not load your risk flags. Please try again shortly.
        </p>
      ) : data && data.flags.length > 0 ? (
        <div className="flex flex-col gap-2">
          {data.flags.map((flag) => (
            <Card key={flag.flagId} className="flex flex-col gap-1">
              <div className="flex items-center justify-between">
                <span className="font-medium text-foreground">{flag.rule}</span>
                <span className="text-sm text-muted">{new Date(flag.createdAt).toLocaleString()}</span>
              </div>
              <span className="text-sm text-muted">{flag.detail}</span>
            </Card>
          ))}
        </div>
      ) : (
        <Card>
          <p className="text-muted">No risk flags. Nothing to see here.</p>
        </Card>
      )}
    </div>
  );
}
