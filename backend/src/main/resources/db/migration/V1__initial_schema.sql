-- Receipt Scanner - Initial Database Schema
-- Week 1, Day 2-3: PostgreSQL Setup

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    full_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Receipts table
CREATE TABLE receipts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    raw_text TEXT,
    merchant_name VARCHAR(255),
    amount DECIMAL(10,2) NOT NULL,
    transaction_date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    items JSONB,
    confidence_score DECIMAL(3,2),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance optimization
CREATE INDEX idx_receipts_user_id ON receipts(user_id);
CREATE INDEX idx_receipts_transaction_date ON receipts(transaction_date DESC);
CREATE INDEX idx_receipts_category ON receipts(category);
CREATE INDEX idx_receipts_user_date ON receipts(user_id, transaction_date DESC);

-- Comments for documentation
COMMENT ON TABLE users IS 'Application users';
COMMENT ON TABLE receipts IS 'Scanned receipts with OCR and AI-parsed data';
COMMENT ON COLUMN receipts.items IS 'Line items in JSONB format: [{"name": "item", "price": 10.99}]';
COMMENT ON COLUMN receipts.confidence_score IS 'AI confidence score (0.0 to 1.0) for parsed data';
