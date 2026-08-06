import { useQuery } from '@tanstack/react-query';
import { riskApi } from '../api/riskApi';

export function useMyFlags() {
  return useQuery({
    queryKey: ['risk', 'flags'],
    queryFn: riskApi.getMyFlags,
  });
}
