import type { Currency } from '../../constants/enums';

export interface VaultResponse {
  vaultId: string;
  currency: Currency;
  balance: number;
}
