import { useQuery } from '@tanstack/react-query';
import { vaultsApi } from '../api/vaultsApi';

// retry: false - a 404 here means "no vault yet" (a normal, expected state until the
// customer's first home-currency round-up), not a transient failure worth retrying.
export function useMyVault() {
  return useQuery({
    queryKey: ['vaults', 'me'],
    queryFn: vaultsApi.getMyVault,
    retry: false,
  });
}
