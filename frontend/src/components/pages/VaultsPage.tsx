import { Card } from '../atoms/Card';
import { isVaultNotFound, useMyVault } from '../../hooks/useVaults';

export function VaultsPage() {
  const { data: vault, isLoading, isError, error } = useMyVault();

  if (isLoading) {
    return <p className="text-foreground">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-foreground">Vaults</h1>
      {isError && !isVaultNotFound(error) ? (
        <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
          Could not load your vault. Please try again shortly.
        </p>
      ) : vault ? (
        <Card className="flex flex-col gap-1">
          <span className="text-sm text-foreground/70">{vault.currency} vault</span>
          <span className="text-3xl font-semibold text-foreground">{vault.balance.toFixed(2)}</span>
        </Card>
      ) : (
        <Card>
          <p className="text-foreground/70">
            No vault yet. Make a transfer in your home currency for a non-whole amount and the
            spare change is automatically rounded up into your vault.
          </p>
        </Card>
      )}
    </div>
  );
}
