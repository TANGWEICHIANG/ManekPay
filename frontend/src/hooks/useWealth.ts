import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { wealthApi } from '../api/wealthApi';
import type { CreateTradeRequestBody } from '../store/models/wealth.model';

export function useAssets(shariahCompliant?: boolean) {
  return useQuery({
    queryKey: ['wealth', 'assets', shariahCompliant],
    queryFn: () => wealthApi.listAssets(shariahCompliant),
  });
}

export function useHoldings() {
  return useQuery({
    queryKey: ['wealth', 'holdings'],
    queryFn: wealthApi.getMyHoldings,
  });
}

export function useCreateTrade() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ request, idempotencyKey }: { request: CreateTradeRequestBody; idempotencyKey: string }) =>
      wealthApi.createTrade(request, idempotencyKey),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wealth', 'holdings'] });
    },
  });
}
