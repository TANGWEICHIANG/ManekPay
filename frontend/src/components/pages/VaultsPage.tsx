import { Card } from '../atoms/Card';
import { useMyVault } from '../../hooks/useVaults';

export function VaultsPage() {
  const { data: vault, isLoading, isError } = useMyVault();

  if (isLoading) {
    return <p className="text-foreground">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-foreground">Vaults</h1>
      {isError || !vault ? (
        <Card>
          <p className="text-foreground/70">
            No vault yet. Make a transfer in your home currency for a non-whole amount and the
            spare change is automatically rounded up into your vault.
          </p>
        </Card>
      ) : (
        <Card className="flex flex-col gap-1">
          <span className="text-sm text-foreground/70">{vault.currency} vault</span>
          <span className="text-3xl font-semibold text-foreground">{vault.balance.toFixed(2)}</span>
        </Card>
      )}
    </div>
  );
}
