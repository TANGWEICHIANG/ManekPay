import { type FormEvent, useId, useState } from 'react';
import { Card } from '../atoms/Card';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { useAssets, useCreateTrade, useHoldings } from '../../hooks/useWealth';

export function WealthPage() {
  const shariahCheckboxId = useId();
  const symbolSelectId = useId();

  const [shariahOnly, setShariahOnly] = useState(false);
  const { data: assetsData, isLoading: assetsLoading } = useAssets(shariahOnly ? true : undefined);
  const { data: holdingsData } = useHoldings();
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
      <h1 className="text-2xl font-semibold text-foreground">Wealth</h1>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-medium text-foreground">Assets</h2>
          <label htmlFor={shariahCheckboxId} className="flex items-center gap-2 text-sm text-foreground">
            <input
              id={shariahCheckboxId}
              type="checkbox"
              checked={shariahOnly}
              onChange={(e) => setShariahOnly(e.target.checked)}
            />
            Shariah-compliant only
          </label>
        </div>
        {assetsLoading ? (
          <p className="text-foreground/70">Loading…</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
            {assetsData?.assets.map((asset) => (
              <Card key={asset.assetId} className="flex flex-col gap-1">
                <span className="text-sm text-foreground/70">{asset.symbol}</span>
                <span className="text-foreground">{asset.name}</span>
                <span className="font-semibold text-foreground">{asset.pricePerShare.toFixed(2)}</span>
              </Card>
            ))}
          </div>
        )}
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <h2 className="text-lg font-medium text-foreground">Buy</h2>
          {createTrade.isError && (
            <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
              {createTrade.error instanceof Error ? createTrade.error.message : 'Trade failed'}
            </p>
          )}
          <div className="flex flex-col gap-1">
            <label htmlFor={symbolSelectId} className="text-sm font-medium text-foreground">
              Symbol
            </label>
            <select
              id={symbolSelectId}
              value={assetSymbol}
              onChange={(e) => setAssetSymbol(e.target.value)}
              className="rounded border border-border bg-background px-3 py-2 text-foreground"
              required
            >
              <option value="" disabled>
                Select an asset
              </option>
              {assetsData?.assets.map((asset) => (
                <option key={asset.assetId} value={asset.symbol}>
                  {asset.symbol} — {asset.name}
                </option>
              ))}
            </select>
          </div>
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
        {holdingsData && holdingsData.holdings.length > 0 ? (
          <div className="flex flex-col gap-2">
            {holdingsData.holdings.map((h) => (
              <Card key={h.assetSymbol} className="flex items-center justify-between">
                <span className="text-foreground">
                  {h.assetSymbol} — {h.assetName}
                </span>
                <span className="text-foreground">{h.shares.toFixed(4)} shares</span>
              </Card>
            ))}
          </div>
        ) : (
          <p className="text-foreground/70">No holdings yet.</p>
        )}
      </div>
    </div>
  );
}
