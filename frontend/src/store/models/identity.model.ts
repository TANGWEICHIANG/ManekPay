import { InquiryStatus, VerificationStatus, VerificationType } from '../../constants/enums';

export interface VerificationSummary {
  verificationId: string;
  type: VerificationType;
  status: VerificationStatus;
  resultDetail: string | null;
}

export interface InquiryResponse {
  inquiryId: string;
  status: InquiryStatus;
  verifications: VerificationSummary[];
}

export interface GovernmentIdFields {
  nric: string;
  dob: string;
  nationality: string;
}
