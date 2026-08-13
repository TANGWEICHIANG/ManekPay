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
  FX: {
    RATE: (from: string, to: string) => `/fx/rates/${from}/${to}`,
    LOCKS: '/fx/locks',
    LOCK: (id: string) => `/fx/locks/${id}`,
  },
  VAULTS: {
    ME: '/vaults/me',
    GOALS: '/vaults/goals',
    GOAL: (id: string) => `/vaults/goals/${id}`,
  },
  RISK: {
    FLAGS: '/risk/flags/me',
  },
  WEALTH: {
    ASSETS: '/wealth/assets',
    TRADES: '/wealth/trades',
    HOLDINGS: '/wealth/holdings/me',
  },
} as const;
