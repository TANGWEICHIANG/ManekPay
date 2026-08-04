CREATE TABLE kyc03_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inquiry_id UUID NOT NULL REFERENCES kyc02_inquiries(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('GOVERNMENT_ID', 'SELFIE')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PASSED', 'FAILED', 'NEEDS_REVIEW')),
    document_data BYTEA,
    declared_data JSONB,
    result_detail JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_kyc03_verifications_inquiry_id ON kyc03_verifications(inquiry_id);
