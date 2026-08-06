import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ledgerApi } from '../api/ledgerApi';
import type { ProxyType, TransferRequestBody } from '../store/models/ledger.model';
import type { Currency } from '../constants/enums';

export function useMyAccount() {
  return useQuery({
    queryKey: ['ledger', 'me'],
    queryFn: ledgerApi.getMyAccount,
  });
}

export function useProxies() {
  return useQuery({
    queryKey: ['ledger', 'proxies'],
    queryFn: ledgerApi.listProxies,
  });
}

export function useLinkProxy() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ type, value }: { type: ProxyType; value: string }) => ledgerApi.linkProxy(type, value),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ledger', 'proxies'] });
    },
  });
}

export function useFxRate(from: Currency, to: Currency | null) {
  return useQuery({
    queryKey: ['ledger', 'fx-rate', from, to],
    queryFn: () => ledgerApi.getFxRate(from, to as Currency),
    enabled: to !== null && to !== from,
  });
}

export function useTransfers() {
  return useQuery({
    queryKey: ['ledger', 'transfers'],
    queryFn: ledgerApi.listTransfers,
  });
}

export function useCreateTransfer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ request, idempotencyKey }: { request: TransferRequestBody; idempotencyKey: string }) =>
      ledgerApi.createTransfer(request, idempotencyKey),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ledger', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['ledger', 'transfers'] });
    },
  });
}
