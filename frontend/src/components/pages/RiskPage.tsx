import { Card } from '../atoms/Card';
import { useMyFlags } from '../../hooks/useRisk';

export function RiskPage() {
  const { data, isLoading, isError } = useMyFlags();

  if (isLoading) {
    return <p className="text-foreground">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-foreground">Risk</h1>
      {isError ? (
        <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
          Could not load your risk flags. Please try again shortly.
        </p>
      ) : data && data.flags.length > 0 ? (
        <div className="flex flex-col gap-2">
          {data.flags.map((flag) => (
            <Card key={flag.flagId} className="flex flex-col gap-1">
              <div className="flex items-center justify-between">
                <span className="font-medium text-foreground">{flag.rule}</span>
                <span className="text-sm text-foreground/70">{new Date(flag.createdAt).toLocaleString()}</span>
              </div>
              <span className="text-sm text-foreground/70">{flag.detail}</span>
            </Card>
          ))}
        </div>
      ) : (
        <Card>
          <p className="text-foreground/70">No risk flags. Nothing to see here.</p>
        </Card>
      )}
    </div>
  );
}
