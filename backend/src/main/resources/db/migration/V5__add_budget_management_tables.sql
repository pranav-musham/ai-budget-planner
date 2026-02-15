-- Budget Management System Tables
-- Adds support for budget tracking, custom categories, and AI-powered alerts

-- Custom Categories table
-- Allows users to create their own spending categories beyond predefined ones
CREATE TABLE custom_categories (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, name)
);

-- Budgets table
-- Stores budget goals per category (both predefined and custom)
CREATE TABLE budgets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    limit_amount DECIMAL(12,2) NOT NULL CHECK (limit_amount > 0),
    limit_type VARCHAR(20) NOT NULL CHECK (limit_type IN ('PERCENTAGE', 'DOLLAR')),
    period_type VARCHAR(20) DEFAULT 'MONTHLY' CHECK (period_type IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, category, period_type)
);

-- Budget Alerts table
-- Stores alert history with AI-generated suggestions
CREATE TABLE budget_alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    budget_id BIGINT REFERENCES budgets(id) ON DELETE SET NULL,
    alert_type VARCHAR(50) NOT NULL CHECK (alert_type IN ('REAL_TIME', 'WEEKLY_SUMMARY', 'MONTHLY_SUMMARY', 'ON_DEMAND')),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    category VARCHAR(100) NOT NULL,
    current_spending DECIMAL(12,2) NOT NULL,
    budget_limit DECIMAL(12,2) NOT NULL,
    percentage_used DECIMAL(5,2) NOT NULL,
    message TEXT NOT NULL,
    ai_suggestions JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_custom_categories_user_id ON custom_categories(user_id);
CREATE INDEX idx_budgets_user_id ON budgets(user_id);
CREATE INDEX idx_budgets_user_category ON budgets(user_id, category);
CREATE INDEX idx_budgets_active ON budgets(user_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_budget_alerts_user_id ON budget_alerts(user_id);
CREATE INDEX idx_budget_alerts_created_at ON budget_alerts(user_id, created_at DESC);
CREATE INDEX idx_budget_alerts_unread ON budget_alerts(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_budget_alerts_budget_id ON budget_alerts(budget_id);

-- Composite indexes for common queries
CREATE INDEX idx_receipts_user_category_date ON receipts(user_id, category, transaction_date DESC);

-- Comments for documentation
COMMENT ON TABLE custom_categories IS 'User-defined spending categories with custom icons and colors';
COMMENT ON TABLE budgets IS 'Budget goals per category with flexible limits ($ or %)';
COMMENT ON TABLE budget_alerts IS 'Budget alert history with AI-powered spending suggestions';
COMMENT ON COLUMN budgets.limit_type IS 'PERCENTAGE = % of total income/budget, DOLLAR = fixed amount';
COMMENT ON COLUMN budget_alerts.ai_suggestions IS 'AI-generated suggestions in JSONB: [{"title": "...", "description": "...", "savings": 50.00}]';
COMMENT ON COLUMN budget_alerts.severity IS 'INFO (0-80%), WARNING (80-100%), CRITICAL (>100%)';
