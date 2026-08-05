import { KycStatus } from '../../constants/enums';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface UserProfile {
  customerId: string;
  email: string;
  fullName: string;
  kycStatus: KycStatus;
}

export interface RegisterResponse {
  customerId: string;
  kycStatus: KycStatus;
}
