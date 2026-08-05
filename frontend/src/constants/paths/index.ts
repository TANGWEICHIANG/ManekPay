export const API_PATHS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REFRESH: '/auth/refresh',
    LOGOUT: '/auth/logout',
    ME: '/auth/me',
  },
  INQUIRIES: {
    CREATE: '/inquiries',
    GET: (id: string) => `/inquiries/${id}`,
    GOVERNMENT_ID: (id: string) => `/inquiries/${id}/verifications/government-id`,
    SELFIE: (id: string) => `/inquiries/${id}/verifications/selfie`,
  },
} as const;
