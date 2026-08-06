import { type FormEvent, useState } from 'react';
import { Card } from '../atoms/Card';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { useCreateFxLock, useFxRate } from '../../hooks/useFx';
import { Currency } from '../../constants/enums';

const CURRENCIES: Currency[] = [Currency.MYR, Currency.SGD, Currency.USD, Currency.EUR, Currency.GBP];

export function FxPage() {
  const [from, setFrom] = useState<Currency>(Currency.MYR);
  const [to, setTo] = useState<Currency>(Currency.SGD);
  const [amount, setAmount] = useState('100');

  const { data: rate, isLoading: rateLoading } = useFxRate(from, to !== from ? to : null);
  const createLock = useCreateFxLock();

  const handleLock = (e: FormEvent) => {
    e.preventDefault();
    createLock.mutate({ from, to });
  };

  const converted = rate && amount ? (Number(amount) * rate.rate).toFixed(2) : null;

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-foreground">FX</h1>

      <Card className="flex flex-col gap-4">
        <h2 className="text-lg font-medium text-foreground">Convert</h2>
        <div className="grid grid-cols-2 gap-4">
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-foreground">From</label>
            <select
              value={from}
              onChange={(e) => setFrom(e.target.value as Currency)}
              className="rounded border border-border bg-background px-3 py-2 text-foreground"
            >
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-foreground">To</label>
            <select
              value={to}
              onChange={(e) => setTo(e.target.value as Currency)}
              className="rounded border border-border bg-background px-3 py-2 text-foreground"
            >
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
        </div>
        <Input
          label="Amount"
          type="number"
          step="0.01"
          min="0"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
        {to === from ? (
          <p className="text-sm text-foreground/70">Pick two different currencies to see a rate.</p>
        ) : rateLoading ? (
          <p className="text-sm text-foreground/70">Loading rate…</p>
        ) : rate ? (
          <p className="text-foreground">
            1 {from} = {rate.rate} {to} — {amount || 0} {from} ≈ {converted} {to}
          </p>
        ) : null}

        <form onSubmit={handleLock} className="flex flex-col gap-2">
          {createLock.isError && (
            <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
              {createLock.error instanceof Error ? createLock.error.message : 'Could not lock rate'}
            </p>
          )}
          <Button type="submit" isLoading={createLock.isPending} disabled={to === from}>
            Lock this rate for 15s
          </Button>
          {createLock.data && (
            <p className="text-sm text-foreground/70">
              Locked {createLock.data.rate} {to} per {from} until{' '}
              {new Date(createLock.data.expiresAt).toLocaleTimeString()} (lock{' '}
              {createLock.data.lockId.slice(0, 8)})
            </p>
          )}
        </form>
      </Card>
    </div>
  );
}
