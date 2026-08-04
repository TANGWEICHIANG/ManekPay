export const VerificationType = {
  GOVERNMENT_ID: 'GOVERNMENT_ID',
  SELFIE: 'SELFIE'
} as const;
export type VerificationType = typeof VerificationType[keyof typeof VerificationType];

export const InquiryStatus = {
  CREATED: 'CREATED',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
} as const;
export type InquiryStatus = typeof InquiryStatus[keyof typeof InquiryStatus];

export const VerificationStatus = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
} as const;
export type VerificationStatus = typeof VerificationStatus[keyof typeof VerificationStatus];