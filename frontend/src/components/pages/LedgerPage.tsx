import { type FormEvent, useState } from 'react';
import { Card } from '../atoms/Card';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { useMyAccount, useFxRate, useCreateTransfer, useTransfers } from '../../hooks/useLedger';
import { Currency } from '../../constants/enums';
import type { RecipientType } from '../../store/models/ledger.model';

const RECIPIENT_TYPES: { value: RecipientType; label: string }[] = [
  { value: 'ACCOUNT_NUMBER', label: 'Account Number' },
  { value: 'NRIC', label: 'NRIC' },
  { value: 'MOBILE', label: 'Mobile Number' },
];

const CURRENCIES: Currency[] = [Currency.MYR, Currency.SGD, Currency.USD, Currency.EUR, Currency.GBP];

export function LedgerPage() {
  const { data: account, isLoading: accountLoading } = useMyAccount();
  const { data: transfersData } = useTransfers();
  const createTransfer = useCreateTransfer();

  const [recipientType, setRecipientType] = useState<RecipientType>('ACCOUNT_NUMBER');
  const [recipientValue, setRecipientValue] = useState('');
  const [sourceCurrency, setSourceCurrency] = useState<Currency>(Currency.MYR);
  const [destCurrency, setDestCurrency] = useState<Currency>(Currency.MYR);
  const [amount, setAmount] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());

  const { data: fxRate } = useFxRate(sourceCurrency, destCurrency !== sourceCurrency ? destCurrency : null);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    createTransfer.mutate(
      {
        request: {
          recipient: { type: recipientType, value: recipientValue },
          sourceCurrency,
          destCurrency,
          amount,
        },
        idempotencyKey,
      },
      {
        onSuccess: () => {
          setRecipientValue('');
          setAmount('');
          setIdempotencyKey(crypto.randomUUID());
        },
      }
    );
  };

  if (accountLoading || !account) {
    return <p className="text-foreground">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold text-foreground">Ledger</h1>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Balances</h2>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
          {account.wallets.map((wallet) => (
            <Card key={wallet.currency} className="flex flex-col gap-1">
              <span className="text-sm text-foreground/70">{wallet.currency}</span>
              <span className="text-xl font-semibold text-foreground">{wallet.balance.toFixed(2)}</span>
            </Card>
          ))}
        </div>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <h2 className="text-lg font-medium text-foreground">Send Money</h2>
          {createTransfer.isError && (
            <p role="alert" className="rounded bg-danger/10 px-3 py-2 text-sm text-danger">
              {createTransfer.error instanceof Error ? createTransfer.error.message : 'Transfer failed'}
            </p>
          )}
          <div className="flex flex-col gap-1">
            <label className="text-sm font-medium text-foreground">Recipient type</label>
            <select
              value={recipientType}
              onChange={(e) => setRecipientType(e.target.value as RecipientType)}
              className="rounded border border-border bg-background px-3 py-2 text-foreground"
            >
              {RECIPIENT_TYPES.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>
          <Input
            label="Recipient"
            required
            value={recipientValue}
            onChange={(e) => setRecipientValue(e.target.value)}
          />
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-foreground">Send from</label>
              <select
                value={sourceCurrency}
                onChange={(e) => setSourceCurrency(e.target.value as Currency)}
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
              <label className="text-sm font-medium text-foreground">Recipient receives</label>
              <select
                value={destCurrency}
                onChange={(e) => setDestCurrency(e.target.value as Currency)}
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
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          {fxRate && destCurrency !== sourceCurrency && amount && (
            <p className="text-sm text-foreground/70">
              Recipient gets ≈ {(Number(amount) * fxRate.rate).toFixed(2)} {destCurrency}
            </p>
          )}
          <Button type="submit" isLoading={createTransfer.isPending}>
            Send
          </Button>
        </form>
      </Card>

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Transaction History</h2>
        {transfersData && transfersData.transfers.length > 0 ? (
          <div className="flex flex-col gap-2">
            {transfersData.transfers.map((t) => (
              <Card key={t.transferId} className="flex items-center justify-between">
                <span className="text-foreground">
                  {t.sourceAmount} {t.sourceCurrency} → {t.destAmount} {t.destCurrency}
                </span>
                <span className="text-sm text-foreground/70">{new Date(t.createdAt).toLocaleString()}</span>
              </Card>
            ))}
          </div>
        ) : (
          <p className="text-foreground/70">No transactions yet.</p>
        )}
      </div>
    </div>
  );
}
