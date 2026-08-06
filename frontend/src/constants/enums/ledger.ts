export const Currency = {
  MYR: 'MYR',
  SGD: 'SGD',
  USD: 'USD',
  EUR: 'EUR',
  GBP: 'GBP',
} as const;
export type Currency = typeof Currency[keyof typeof Currency];
