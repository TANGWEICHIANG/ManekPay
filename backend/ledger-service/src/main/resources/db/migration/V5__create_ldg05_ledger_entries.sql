CREATE TABLE ldg05_ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_id UUID NOT NULL REFERENCES ldg04_transfers(id),
    wallet_id UUID NOT NULL REFERENCES ldg02_wallets(id),
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(18,4) NOT NULL,
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('MYR', 'SGD', 'USD', 'EUR', 'GBP')),
    balance_after NUMERIC(18,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
