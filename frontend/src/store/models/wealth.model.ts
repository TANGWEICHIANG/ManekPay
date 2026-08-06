export interface Asset {
  assetId: string;
  symbol: string;
  name: string;
  pricePerShare: number;
  shariahCompliant: boolean;
}

export interface AssetsResponse {
  assets: Asset[];
}

export interface CreateTradeRequestBody {
  assetSymbol: string;
  amount: string;
}

export interface Trade {
  tradeId: string;
  assetSymbol: string;
  amount: number;
  shares: number;
  pricePerShare: number;
  createdAt: string;
}

export interface Holding {
  assetSymbol: string;
  assetName: string;
  shares: number;
  shariahCompliant: boolean;
}

export interface HoldingsResponse {
  holdings: Holding[];
}
