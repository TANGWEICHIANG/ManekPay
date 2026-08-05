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
  LEDGER: {
    ME: '/ledger/accounts/me',
    PROXIES: '/ledger/accounts/me/proxies',
    PROXY: (id: string) => `/ledger/accounts/me/proxies/${id}`,
    FX_RATE: (from: string, to: string) => `/ledger/fx-rates/${from}/${to}`,
    TRANSFERS: '/ledger/transfers',
    TRANSFER: (id: string) => `/ledger/transfers/${id}`,
  },
} as const;
