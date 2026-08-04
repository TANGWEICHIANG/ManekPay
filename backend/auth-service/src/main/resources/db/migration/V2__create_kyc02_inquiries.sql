CREATE TABLE kyc02_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES kyc01_customers(id),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'IN_PROGRESS', 'APPROVED', 'DECLINED', 'NEEDS_REVIEW')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_kyc02_inquiries_customer_id ON kyc02_inquiries(customer_id);
