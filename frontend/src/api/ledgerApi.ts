import { apiRequest } from './apiClient';
import { API_PATHS } from '../constants/paths';
import type { Currency } from '../constants/enums';
import type {
  AccountProxy,
  FxRateResponse,
  MyAccountResponse,
  ProxiesResponse,
  ProxyType,
  Transfer,
  TransferRequestBody,
  TransfersResponse,
} from '../store/models/ledger.model';

export const ledgerApi = {
  getMyAccount: (): Promise<MyAccountResponse> =>
    apiRequest(API_PATHS.LEDGER.ME, { method: 'GET' }),

  linkProxy: (type: ProxyType, value: string): Promise<AccountProxy> =>
    apiRequest(API_PATHS.LEDGER.PROXIES, { method: 'POST', body: { type, value } }),

  listProxies: (): Promise<ProxiesResponse> =>
    apiRequest(API_PATHS.LEDGER.PROXIES, { method: 'GET' }),

  deleteProxy: (id: string): Promise<void> =>
    apiRequest(API_PATHS.LEDGER.PROXY(id), { method: 'DELETE' }),

  getFxRate: (from: Currency, to: Currency): Promise<FxRateResponse> =>
    apiRequest(API_PATHS.LEDGER.FX_RATE(from, to), { method: 'GET' }),

  createTransfer: (request: TransferRequestBody, idempotencyKey: string): Promise<Transfer> =>
    apiRequest(API_PATHS.LEDGER.TRANSFERS, {
      method: 'POST',
      body: request,
      headers: { 'X-Idempotency-Key': idempotencyKey },
    }),

  listTransfers: (): Promise<TransfersResponse> =>
    apiRequest(API_PATHS.LEDGER.TRANSFERS, { method: 'GET' }),
};
