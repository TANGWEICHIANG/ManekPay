import { useMutation, useQuery } from '@tanstack/react-query';
import { fxApi } from '../api/fxApi';
import type { Currency } from '../constants/enums';

export function useFxRate(from: Currency, to: Currency | null) {
  return useQuery({
    queryKey: ['fx', 'rate', from, to],
    queryFn: () => fxApi.getRate(from, to as Currency),
    enabled: to !== null && to !== from,
    refetchInterval: 10000,
  });
}

export function useCreateFxLock() {
  return useMutation({
    mutationFn: ({ from, to }: { from: Currency; to: Currency }) => fxApi.createLock(from, to),
  });
}
