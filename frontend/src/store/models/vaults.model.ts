import type { Currency } from '../../constants/enums';

export interface VaultResponse {
  vaultId: string;
  currency: Currency;
  balance: number;
}

export type SweepFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface Goal {
  id: string;
  name: string;
  currency: Currency;
  balance: number;
  targetAmount: number;
  sweepAmount: number;
  sweepFrequency: SweepFrequency;
  sweepActive: boolean;
  nextSweepAt: string;
  lastSweepAt: string | null;
}

export interface CreateGoalRequestBody {
  name: string;
  currency: Currency;
  targetAmount: number;
  sweepAmount: number;
  sweepFrequency: SweepFrequency;
}

export interface UpdateGoalRequestBody {
  sweepAmount?: number;
  sweepFrequency?: SweepFrequency;
  sweepActive?: boolean;
}
