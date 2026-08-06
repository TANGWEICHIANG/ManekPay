import { apiRequest } from './apiClient';
import { API_PATHS } from '../constants/paths';
import type { VaultResponse } from '../store/models/vaults.model';

export const vaultsApi = {
  getMyVault: (): Promise<VaultResponse> =>
    apiRequest(API_PATHS.VAULTS.ME, { method: 'GET' }),
};
