-- V14: Create knowledge documents
CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    product_id UUID REFERENCES products(id) ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_kd_status CHECK (
        status IN ('ACTIVE', 'PROCESSING', 'FAILED', 'ARCHIVED')
    )
);
CREATE INDEX idx_knowledge_documents_merchant_id ON knowledge_documents(merchant_id);
CREATE INDEX idx_knowledge_documents_product_id ON knowledge_documents(product_id);
