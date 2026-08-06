import { apiRequest } from './apiClient';
import { API_PATHS } from '../constants/paths';
import type { AssetsResponse, CreateTradeRequestBody, HoldingsResponse, Trade } from '../store/models/wealth.model';

export const wealthApi = {
  listAssets: (shariahCompliant?: boolean): Promise<AssetsResponse> => {
    const query = shariahCompliant !== undefined ? `?shariahCompliant=${shariahCompliant}` : '';
    return apiRequest(`${API_PATHS.WEALTH.ASSETS}${query}`, { method: 'GET' });
  },

  createTrade: (request: CreateTradeRequestBody, idempotencyKey: string): Promise<Trade> =>
    apiRequest(API_PATHS.WEALTH.TRADES, {
      method: 'POST',
      body: request,
      headers: { 'X-Idempotency-Key': idempotencyKey },
    }),

  getMyHoldings: (): Promise<HoldingsResponse> => apiRequest(API_PATHS.WEALTH.HOLDINGS, { method: 'GET' }),
};
