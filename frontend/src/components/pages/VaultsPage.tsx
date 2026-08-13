import { type FormEvent, useState } from 'react';
import { PiggyBank } from 'lucide-react';
import { Card } from '../atoms/Card';
import { Button } from '../atoms/Button';
import { Input } from '../atoms/Input';
import { Select } from '../atoms/Select';
import { PageHeader } from '../atoms/PageHeader';
import { isVaultNotFound, useCreateGoal, useGoals, useMyVault, useUpdateGoal } from '../../hooks/useVaults';
import { Currency } from '../../constants/enums';
import type { Goal, SweepFrequency } from '../../store/models/vaults.model';

const CURRENCIES: Currency[] = [Currency.MYR, Currency.SGD, Currency.USD, Currency.EUR, Currency.GBP];
const SWEEP_FREQUENCIES: SweepFrequency[] = ['DAILY', 'WEEKLY', 'MONTHLY'];

export function VaultsPage() {
  const { data: vault, isLoading, isError, error } = useMyVault();
  const { data: goals, isLoading: goalsLoading, isError: goalsError } = useGoals();
  const createGoal = useCreateGoal();

  const [name, setName] = useState('');
  const [currency, setCurrency] = useState<Currency>(Currency.MYR);
  const [targetAmount, setTargetAmount] = useState('');
  const [sweepAmount, setSweepAmount] = useState('');
  const [sweepFrequency, setSweepFrequency] = useState<SweepFrequency>('WEEKLY');

  const handleCreateGoal = (e: FormEvent) => {
    e.preventDefault();
    createGoal.mutate(
      { name, currency, targetAmount: Number(targetAmount), sweepAmount: Number(sweepAmount), sweepFrequency },
      {
        onSuccess: () => {
          setName('');
          setTargetAmount('');
          setSweepAmount('');
        },
      }
    );
  };

  if (isLoading) {
    return <p className="text-foreground">Loading…</p>;
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader icon={PiggyBank} iconClasses="bg-vaults/10 text-vaults" title="Vaults" />
      {isError && !isVaultNotFound(error) ? (
        <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
          Could not load your vault. Please try again shortly.
        </p>
      ) : vault ? (
        <Card className="flex flex-col gap-1">
          <span className="text-sm text-muted">{vault.currency} vault</span>
          <span className="text-3xl font-semibold tabular-nums text-foreground">{vault.balance.toFixed(2)}</span>
        </Card>
      ) : (
        <Card>
          <p className="text-muted">
            No vault yet. Make a transfer in your home currency for a non-whole amount and the
            spare change is automatically rounded up into your vault.
          </p>
        </Card>
      )}

      <div>
        <h2 className="mb-3 text-lg font-medium text-foreground">Goals</h2>
        {goalsLoading ? (
          <p className="text-muted">Loading…</p>
        ) : goalsError ? (
          <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
            Could not load your goals. Please try again shortly.
          </p>
        ) : goals && goals.length > 0 ? (
          <div className="flex flex-col gap-3">
            {goals.map((goal) => (
              <GoalCard key={goal.id} goal={goal} />
            ))}
          </div>
        ) : (
          <p className="text-muted">No goals yet — create one below.</p>
        )}
      </div>

      <Card>
        <form onSubmit={handleCreateGoal} className="flex flex-col gap-4">
          <h2 className="text-lg font-medium text-foreground">New goal</h2>
          {createGoal.isError && (
            <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
              {createGoal.error instanceof Error ? createGoal.error.message : 'Could not create goal'}
            </p>
          )}
          <Input label="Name" required value={name} onChange={(e) => setName(e.target.value)} />
          <Select label="Currency" value={currency} onChange={(e) => setCurrency(e.target.value as Currency)}>
            {CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
          <Input
            label="Target amount"
            type="number"
            step="0.01"
            min="0.01"
            required
            value={targetAmount}
            onChange={(e) => setTargetAmount(e.target.value)}
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Sweep amount"
              type="number"
              step="0.01"
              min="0.01"
              required
              value={sweepAmount}
              onChange={(e) => setSweepAmount(e.target.value)}
            />
            <Select
              label="Sweep frequency"
              value={sweepFrequency}
              onChange={(e) => setSweepFrequency(e.target.value as SweepFrequency)}
            >
              {SWEEP_FREQUENCIES.map((f) => (
                <option key={f} value={f}>
                  {f.charAt(0) + f.slice(1).toLowerCase()}
                </option>
              ))}
            </Select>
          </div>
          <Button type="submit" isLoading={createGoal.isPending}>
            Create goal
          </Button>
        </form>
      </Card>
    </div>
  );
}

interface GoalCardProps {
  goal: Goal;
}

function GoalCard({ goal }: GoalCardProps) {
  const updateGoal = useUpdateGoal();

  const progress = goal.targetAmount > 0
    ? Math.min(100, (goal.balance / goal.targetAmount) * 100)
    : 0;

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <span className="font-semibold text-foreground">{goal.name}</span>
        <Button
          variant="secondary"
          onClick={() => updateGoal.mutate({ id: goal.id, request: { sweepActive: !goal.sweepActive } })}
          isLoading={updateGoal.isPending}
        >
          {goal.sweepActive ? 'Pause' : 'Resume'}
        </Button>
      </div>
      <div className="flex items-center justify-between text-sm text-muted">
        <span className="tabular-nums">
          {goal.balance.toFixed(2)} / {goal.targetAmount.toFixed(2)} {goal.currency}
        </span>
        <span className="tabular-nums">
          {goal.sweepAmount.toFixed(2)} {goal.currency} / {goal.sweepFrequency.toLowerCase()}
        </span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-surface-hover">
        <div className="h-full rounded-full bg-vaults" style={{ width: `${progress}%` }} />
      </div>
      {updateGoal.isError && (
        <p role="alert" className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">
          {updateGoal.error instanceof Error ? updateGoal.error.message : 'Could not update goal'}
        </p>
      )}
    </Card>
  );
}
