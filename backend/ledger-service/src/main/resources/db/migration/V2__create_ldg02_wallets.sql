CREATE TABLE ldg02_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID REFERENCES ldg01_accounts(id),
    currency VARCHAR(3) NOT NULL CHECK (currency IN ('MYR', 'SGD', 'USD', 'EUR', 'GBP')),
    balance NUMERIC(18,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ldg02_wallets_account_currency_unique ON ldg02_wallets (account_id, currency) WHERE account_id IS NOT NULL;
CREATE UNIQUE INDEX ldg02_wallets_clearing_currency_unique ON ldg02_wallets (currency) WHERE account_id IS NULL;
