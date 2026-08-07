import { type FormEvent, useId, useState } from 'react';
import { TrendingUp } from 'lucide-react';
import { Card } from '../atoms/Card';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { Select } from '../atoms/Select';
import { PageHeader } from '../atoms/PageHeader';
import { useAssets, useCreateTrade, useHoldings } from '../../hooks/useWealth';

export function WealthPage() {
  const shariahCheckboxId = useId();

  const [shariahOnly, setShariahOnly] = useState(false);
  const { data: assetsData, isLoading: assetsLoading, isError: assetsError } = useAssets(shariahOnly ? true : undefined);
  const { data: holdingsData, isLoading: holdingsLoading, isError: holdingsError } = useHoldings();
  const createTrade = useCreateTrade();

  const [assetSymbol, setAssetSymbol] = useState('');
  const [amount, setAmount] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    createTrade.mutate(
      { request: { assetSymbol, amount }, idempotencyKey },
      {
        onSuccess: () => {
          setAmount('');
          setIdempotencyKey(crypto.randomUUID());
        },
      }
    );
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader icon={TrendingUp} iconClasses="bg-wealth/10 text-wealth" title="Wealth" />

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-medium text-foreground">Assets</h2>
          <label htmlFor={shariahCheckboxId} className="flex items-center gap-2 text-sm text-foreground">
            <input
              id={shariahCheckboxId}
              type="checkbox"
              checked={shariahOnly}
              onChange={(e) => setShariahOnly(e.target.checked)}
              className="h-4 w-4 accent-wealth"
            />
            Shariah-compliant only
          </label>
        </div>
        {assetsError ? (
          <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
            Could not load assets. Please try again shortly.
          </p>
        ) : assetsLoading ? (
          <p className="text-muted">Loading…</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
            {assetsData?.assets.map((asset) => (
              <Card key={asset.assetId} className="flex flex-col gap-1">
                <span className="text-sm text-muted">{asset.symbol}</span>
                <span className="text-foreground">{asset.name}</span>
                <span className="font-semibold tabular-nums text-foreground">{asset.pricePerShare.toFixed(2)}</span>
              </Card>
            ))}
          </div>
        )}
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <h2 className="text-lg font-medium text-foreground">Buy</h2>
          {createTrade.isError && (
            <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
              {createTrade.error instanceof Error ? createTrade.error.message : 'Trade failed'}
            </p>
          )}
          <Select label="Symbol" value={assetSymbol} onChange={(e) => setAssetSymbol(e.target.value)} required>
            <option value="" disabled>
              Select an asset
            </option>
            {assetsData?.assets.map((asset) => (
              <option key={asset.assetId} value={asset.symbol}>
                {asset.symbol} — {asset.name}
              </option>
            ))}
          </Select>
          <Input
            label="Amount"
            type="number"
            step="0.01"
            min="0.01"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <Button type="submit" isLoading={createTrade.isPending}>
            Buy
          </Button>
        </form>
      </Card>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Holdings</h2>
        <div aria-live="polite">
          {holdingsError ? (
            <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
              Could not load your holdings. Please try again shortly.
            </p>
          ) : holdingsLoading ? (
            <p className="text-muted">Loading…</p>
          ) : holdingsData && holdingsData.holdings.length > 0 ? (
            <div className="flex flex-col gap-2">
              {holdingsData.holdings.map((h) => (
                <Card key={h.assetSymbol} className="flex items-center justify-between">
                  <span className="text-foreground">
                    {h.assetSymbol} — {h.assetName}
                  </span>
                  <span className="tabular-nums text-foreground">{h.shares.toFixed(4)} shares</span>
                </Card>
              ))}
            </div>
          ) : (
            <p className="text-muted">No holdings yet.</p>
          )}
        </div>
      </div>
    </div>
  );
}
