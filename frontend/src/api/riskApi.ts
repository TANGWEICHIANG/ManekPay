import { apiRequest } from './apiClient';
import { API_PATHS } from '../constants/paths';
import type { FlagsResponse } from '../store/models/risk.model';

export const riskApi = {
  getMyFlags: (): Promise<FlagsResponse> => apiRequest(API_PATHS.RISK.FLAGS, { method: 'GET' }),
};
