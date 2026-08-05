export const VerificationType = {
  GOVERNMENT_ID: 'GOVERNMENT_ID',
  SELFIE: 'SELFIE'
} as const;
export type VerificationType = typeof VerificationType[keyof typeof VerificationType];

export const InquiryStatus = {
  CREATED: 'CREATED',
  IN_PROGRESS: 'IN_PROGRESS',
  APPROVED: 'APPROVED',
  DECLINED: 'DECLINED',
  NEEDS_REVIEW: 'NEEDS_REVIEW',
} as const;
export type InquiryStatus = typeof InquiryStatus[keyof typeof InquiryStatus];

export const VerificationStatus = {
  PENDING: 'PENDING',
  PASSED: 'PASSED',
  FAILED: 'FAILED',
  NEEDS_REVIEW: 'NEEDS_REVIEW',
} as const;
export type VerificationStatus = typeof VerificationStatus[keyof typeof VerificationStatus];
