export interface Flag {
  flagId: string;
  transactionId: string;
  rule: string;
  detail: string;
  createdAt: string;
}

export interface FlagsResponse {
  flags: Flag[];
}
