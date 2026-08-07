import { type FormEvent, useState } from 'react';
import { ArrowLeftRight } from 'lucide-react';
import { Card } from '../atoms/Card';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { Select } from '../atoms/Select';
import { PageHeader } from '../atoms/PageHeader';
import { useCreateFxLock, useFxRate } from '../../hooks/useFx';
import { Currency } from '../../constants/enums';

const CURRENCIES: Currency[] = [Currency.MYR, Currency.SGD, Currency.USD, Currency.EUR, Currency.GBP];

export function FxPage() {
  const [from, setFrom] = useState<Currency>(Currency.MYR);
  const [to, setTo] = useState<Currency>(Currency.SGD);
  const [amount, setAmount] = useState('100');

  const { data: rate, isLoading: rateLoading, isError: rateIsError } = useFxRate(from, to !== from ? to : null);
  const createLock = useCreateFxLock();

  const handleLock = (e: FormEvent) => {
    e.preventDefault();
    createLock.mutate({ from, to });
  };

  const converted = rate && amount ? (Number(amount) * rate.rate).toFixed(2) : null;

  return (
    <div className="flex flex-col gap-6">
      <PageHeader icon={ArrowLeftRight} iconClasses="bg-fx/10 text-fx" title="FX" />

      <Card className="flex flex-col gap-4">
        <h2 className="text-lg font-medium text-foreground">Convert</h2>
        <div className="grid grid-cols-2 gap-4">
          <Select label="From" value={from} onChange={(e) => setFrom(e.target.value as Currency)}>
            {CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
          <Select label="To" value={to} onChange={(e) => setTo(e.target.value as Currency)}>
            {CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
        </div>
        <Input
          label="Amount"
          type="number"
          step="0.01"
          min="0"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
        <div aria-live="polite">
          {to === from ? (
            <p className="text-sm text-muted">Pick two different currencies to see a rate.</p>
          ) : rateLoading ? (
            <p className="text-sm text-muted">Loading rate…</p>
          ) : rateIsError ? (
            <p role="alert" className="text-sm text-danger">
              Could not load the current rate. Please try again shortly.
            </p>
          ) : rate ? (
            <p className="tabular-nums text-foreground">
              1 {from} = {rate.rate} {to} — {amount || 0} {from} ≈ {converted} {to}
            </p>
          ) : null}
        </div>

        <form onSubmit={handleLock} className="flex flex-col gap-2">
          {createLock.isError && (
            <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
              {createLock.error instanceof Error ? createLock.error.message : 'Could not lock rate'}
            </p>
          )}
          <Button type="submit" isLoading={createLock.isPending} disabled={to === from}>
            Lock this rate for 15s
          </Button>
          <div aria-live="polite">
            {createLock.data && (
              <p className="text-sm tabular-nums text-muted">
                Locked {createLock.data.rate} {createLock.data.to} per {createLock.data.from} until{' '}
                {new Date(createLock.data.expiresAt).toLocaleTimeString()} (lock{' '}
                {createLock.data.lockId.slice(0, 8)})
              </p>
            )}
          </div>
        </form>
      </Card>
    </div>
  );
}
