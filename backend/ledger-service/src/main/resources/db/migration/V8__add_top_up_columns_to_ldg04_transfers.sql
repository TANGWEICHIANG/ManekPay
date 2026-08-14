ALTER TABLE ldg04_transfers
    ADD COLUMN top_up_amount NUMERIC(18,4),
    ADD COLUMN top_up_currency VARCHAR(3) CHECK (top_up_currency IN ('MYR', 'SGD', 'USD', 'EUR', 'GBP')),
    ADD COLUMN top_up_fx_rate NUMERIC(18,8);
