export const KycStatus = {
  PENDING: 'PENDING',
  IN_REVIEW: 'IN_REVIEW',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
} as const;

export type KycStatus = typeof KycStatus[keyof typeof KycStatus];