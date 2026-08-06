import type { Currency } from '../../constants/enums';

export interface Wallet {
  currency: Currency;
  balance: number;
}

export interface MyAccountResponse {
  accountId: string;
  accountNumber: string;
  wallets: Wallet[];
}

export type ProxyType = 'NRIC' | 'MOBILE';

export interface AccountProxy {
  proxyId: string;
  type: ProxyType;
  value: string;
  createdAt: string;
}

export interface ProxiesResponse {
  proxies: AccountProxy[];
}

export interface FxRateResponse {
  from: Currency;
  to: Currency;
  rate: number;
}

export type RecipientType = 'ACCOUNT_NUMBER' | 'NRIC' | 'MOBILE';

export interface TransferRequestBody {
  recipient: { type: RecipientType; value: string };
  sourceCurrency: Currency;
  destCurrency: Currency;
  amount: string;
}

export interface Transfer {
  transferId: string;
  sourceAmount: number;
  sourceCurrency: Currency;
  destAmount: number;
  destCurrency: Currency;
  fxRate: number | null;
  createdAt: string;
}

export interface TransfersResponse {
  transfers: Transfer[];
}
