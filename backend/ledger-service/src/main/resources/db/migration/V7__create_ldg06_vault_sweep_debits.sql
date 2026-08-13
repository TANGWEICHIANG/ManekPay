CREATE TABLE ldg06_vault_sweep_debits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    amount NUMERIC(18,4) NOT NULL,
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('MYR', 'SGD', 'USD', 'EUR', 'GBP')),
    balance_after NUMERIC(18,4) NOT NULL,
    reference VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
