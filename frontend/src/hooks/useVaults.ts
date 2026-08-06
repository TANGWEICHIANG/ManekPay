import { useQuery } from '@tanstack/react-query';
import { ApiError } from '../api/apiClient';
import { vaultsApi } from '../api/vaultsApi';

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
