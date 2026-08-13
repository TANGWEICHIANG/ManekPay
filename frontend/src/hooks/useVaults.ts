import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../api/apiClient';
import { vaultsApi } from '../api/vaultsApi';
import type { CreateGoalRequestBody, UpdateGoalRequestBody } from '../store/models/vaults.model';

// A 404 here means "no vault yet" (a normal, expected state until the customer's first
// home-currency round-up), not a transient failure worth retrying - but a real 500/network
// error should still get the default retry behavior.
export function useMyVault() {
  return useQuery({
    queryKey: ['vaults', 'me'],
    queryFn: vaultsApi.getMyVault,
    retry: (failureCount, error) => error instanceof ApiError && error.status === 404 ? false : failureCount < 3,
  });
}

export function isVaultNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}

export function useGoals() {
  return useQuery({
    queryKey: ['vaults', 'goals'],
    queryFn: vaultsApi.listGoals,
  });
}

export function useCreateGoal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateGoalRequestBody) => vaultsApi.createGoal(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vaults', 'goals'] });
    },
  });
}

export function useUpdateGoal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: UpdateGoalRequestBody }) => vaultsApi.updateGoal(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vaults', 'goals'] });
    },
  });
}
