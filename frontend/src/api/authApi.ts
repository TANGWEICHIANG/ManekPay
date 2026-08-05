import { apiRequest } from './apiClient';
import { API_PATHS } from '../constants/paths';
import type { LoginRequest, RegisterRequest, RegisterResponse, TokenResponse, UserProfile } from '../store/models/auth.model';
import type { GovernmentIdFields, InquiryResponse, VerificationSummary } from '../store/models/identity.model';

export const authApi = {
  login: (credentials: LoginRequest): Promise<TokenResponse> =>
    apiRequest(API_PATHS.AUTH.LOGIN, { method: 'POST', body: credentials }),

  register: (data: RegisterRequest): Promise<RegisterResponse> =>
    apiRequest(API_PATHS.AUTH.REGISTER, { method: 'POST', body: data }),

  refresh: (refreshToken: string): Promise<TokenResponse> =>
    apiRequest(API_PATHS.AUTH.REFRESH, { method: 'POST', body: { refreshToken } }),

  logout: (refreshToken: string): Promise<void> =>
    apiRequest(API_PATHS.AUTH.LOGOUT, { method: 'POST', body: { refreshToken } }),

  getMe: (): Promise<UserProfile> =>
    apiRequest(API_PATHS.AUTH.ME, { method: 'GET' }),

  createInquiry: (): Promise<InquiryResponse> =>
    apiRequest(API_PATHS.INQUIRIES.CREATE, { method: 'POST' }),

  getInquiry: (inquiryId: string): Promise<InquiryResponse> =>
    apiRequest(API_PATHS.INQUIRIES.GET(inquiryId), { method: 'GET' }),

  submitGovernmentId: (inquiryId: string, fields: GovernmentIdFields, image: File): Promise<VerificationSummary> => {
    const formData = new FormData();
    formData.append('image', image);
    formData.append('nric', fields.nric);
    formData.append('dob', fields.dob);
    formData.append('nationality', fields.nationality);
    return apiRequest(API_PATHS.INQUIRIES.GOVERNMENT_ID(inquiryId), { method: 'POST', body: formData, isFormData: true });
  },

  submitSelfie: (inquiryId: string, image: File): Promise<VerificationSummary> => {
    const formData = new FormData();
    formData.append('image', image);
    return apiRequest(API_PATHS.INQUIRIES.SELFIE(inquiryId), { method: 'POST', body: formData, isFormData: true });
  },
};
