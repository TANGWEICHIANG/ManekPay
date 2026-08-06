import type { Currency } from '../../constants/enums';

export interface FxRateResponse {
  from: Currency;
  to: Currency;
  rate: number;
}

export interface FxLockResponse {
  lockId: string;
  from: Currency;
  to: Currency;
  rate: number;
  expiresAt: string;
}
