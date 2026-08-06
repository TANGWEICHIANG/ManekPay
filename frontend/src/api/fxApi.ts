import { apiRequest } from './apiClient';
import { API_PATHS } from '../constants/paths';
import type { Currency } from '../constants/enums';
import type { FxLockResponse, FxRateResponse } from '../store/models/fx.model';

export const fxApi = {
  getRate: (from: Currency, to: Currency): Promise<FxRateResponse> =>
    apiRequest(API_PATHS.FX.RATE(from, to), { method: 'GET' }),

  createLock: (from: Currency, to: Currency): Promise<FxLockResponse> =>
    apiRequest(API_PATHS.FX.LOCKS, { method: 'POST', body: { from, to } }),

  getLock: (lockId: string): Promise<FxLockResponse> =>
    apiRequest(API_PATHS.FX.LOCK(lockId), { method: 'GET' }),
};
