import { useMutation } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import type { GovernmentIdFields } from '../store/models/identity.model';

export function useCreateInquiry() {
  return useMutation({
    mutationFn: () => authApi.createInquiry(),
  });
}

export function useSubmitGovernmentId() {
  return useMutation({
    mutationFn: ({ inquiryId, fields, image }: { inquiryId: string; fields: GovernmentIdFields; image: File }) =>
      authApi.submitGovernmentId(inquiryId, fields, image),
  });
}

export function useSubmitSelfie() {
  return useMutation({
    mutationFn: ({ inquiryId, image }: { inquiryId: string; image: File }) =>
      authApi.submitSelfie(inquiryId, image),
  });
}
