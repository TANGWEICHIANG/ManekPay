import { API_PATHS } from '../constants/paths';
import { LoginRequest, RegisterRequest, TokenResponse, UserProfile } from '../store/models/auth.model';

// Constructed dynamically using Vite environment variables
const BASE_URL = `${import.meta.env.VITE_API_URL || ''}/api`;


export const authApi = {
  login: async (credentials: LoginRequest): Promise<TokenResponse> => {
    const res = await fetch(`${BASE_URL}${API_PATHS.AUTH.LOGIN}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    });
    if (!res.ok) throw new Error('Invalid credentials');
    return res.json();
  },

  register: async (data: RegisterRequest): Promise<void> => {
    const res = await fetch(`${BASE_URL}${API_PATHS.AUTH.REGISTER}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Registration failed');
  },

  getMe: async (token: string): Promise<UserProfile> => {
    const res = await fetch(`${BASE_URL}${API_PATHS.AUTH.ME}`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) throw new Error('Failed to fetch user profile');
    return res.json();
  }
};
