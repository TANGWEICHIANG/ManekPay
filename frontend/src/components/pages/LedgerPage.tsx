import { type FormEvent, useEffect, useState } from 'react';
import { Wallet } from 'lucide-react';
import { Card } from '../atoms/Card';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { Select } from '../atoms/Select';
import { PageHeader } from '../atoms/PageHeader';
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
  const [latitude, setLatitude] = useState<number | null>(null);
  const [longitude, setLongitude] = useState<number | null>(null);
  const [editingLocation, setEditingLocation] = useState(false);

  const { data: fxRate } = useFxRate(sourceCurrency, destCurrency !== sourceCurrency ? destCurrency : null);

  useEffect(() => {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude);
        setLongitude(position.coords.longitude);
      },
      () => {
        // Permission denied or unavailable - location stays unset, which is a valid state;
        // the transfer still succeeds and the anomaly check is simply skipped for it.
      }
    );
  }, []);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    createTransfer.mutate(
      {
        request: {
          recipient: { type: recipientType, value: recipientValue },
          sourceCurrency,
          destCurrency,
          amount,
          location: latitude !== null && longitude !== null ? { latitude, longitude } : undefined,
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
      <PageHeader icon={Wallet} iconClasses="bg-ledger/10 text-ledger" title="Ledger" />

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Balances</h2>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
          {account.wallets.map((wallet) => (
            <Card key={wallet.currency} className="flex flex-col gap-1">
              <span className="text-sm text-muted">{wallet.currency}</span>
              <span className="text-xl font-semibold tabular-nums text-foreground">{wallet.balance.toFixed(2)}</span>
            </Card>
          ))}
        </div>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <h2 className="text-lg font-medium text-foreground">Send Money</h2>
          {createTransfer.isError && (
            <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
              {createTransfer.error instanceof Error ? createTransfer.error.message : 'Transfer failed'}
            </p>
          )}
          <Select
            label="Recipient type"
            value={recipientType}
            onChange={(e) => setRecipientType(e.target.value as RecipientType)}
          >
            {RECIPIENT_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </Select>
          <Input
            label="Recipient"
            required
            value={recipientValue}
            onChange={(e) => setRecipientValue(e.target.value)}
          />
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Send from"
              value={sourceCurrency}
              onChange={(e) => setSourceCurrency(e.target.value as Currency)}
            >
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
            <Select
              label="Recipient receives"
              value={destCurrency}
              onChange={(e) => setDestCurrency(e.target.value as Currency)}
            >
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </div>
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-foreground">Location</span>
              <button
                type="button"
                onClick={() => setEditingLocation((prev) => !prev)}
                className="text-sm text-primary underline"
              >
                {editingLocation ? 'Done' : 'Edit location'}
              </button>
            </div>
            {editingLocation ? (
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Latitude"
                  type="number"
                  step="any"
                  value={latitude ?? ''}
                  onChange={(e) => setLatitude(e.target.value === '' ? null : Number(e.target.value))}
                />
                <Input
                  label="Longitude"
                  type="number"
                  step="any"
                  value={longitude ?? ''}
                  onChange={(e) => setLongitude(e.target.value === '' ? null : Number(e.target.value))}
                />
              </div>
            ) : (
              <p className="text-sm text-muted">
                {latitude !== null && longitude !== null ? `${latitude.toFixed(4)}, ${longitude.toFixed(4)}` : 'Location not set'}
              </p>
            )}
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
            <p className="text-sm text-muted">
              Recipient gets ≈ <span className="tabular-nums">{(Number(amount) * fxRate.rate).toFixed(2)}</span> {destCurrency}
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
                <span className="tabular-nums text-foreground">
                  {t.sourceAmount} {t.sourceCurrency} → {t.destAmount} {t.destCurrency}
                </span>
                <span className="text-sm text-muted">{new Date(t.createdAt).toLocaleString()}</span>
              </Card>
            ))}
          </div>
        ) : (
          <p className="text-muted">No transactions yet.</p>
        )}
      </div>
    </div>
  );
}
