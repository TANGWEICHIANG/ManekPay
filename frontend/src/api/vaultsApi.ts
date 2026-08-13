import { apiRequest } from './apiClient';
import { API_PATHS } from '../constants/paths';
import type { CreateGoalRequestBody, Goal, UpdateGoalRequestBody, VaultResponse } from '../store/models/vaults.model';

export const vaultsApi = {
  getMyVault: (): Promise<VaultResponse> =>
    apiRequest(API_PATHS.VAULTS.ME, { method: 'GET' }),

  listGoals: (): Promise<Goal[]> =>
    apiRequest(API_PATHS.VAULTS.GOALS, { method: 'GET' }),

  createGoal: (request: CreateGoalRequestBody): Promise<Goal> =>
    apiRequest(API_PATHS.VAULTS.GOALS, { method: 'POST', body: request }),

  updateGoal: (id: string, request: UpdateGoalRequestBody): Promise<Goal> =>
    apiRequest(API_PATHS.VAULTS.GOAL(id), { method: 'PATCH', body: request }),
};
