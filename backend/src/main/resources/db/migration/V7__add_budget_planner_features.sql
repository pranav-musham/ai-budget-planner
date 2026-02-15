-- Budget Planner Features Migration
-- Adds: Income tracking, Recurring expenses, Predefined categories, In-app notifications
-- Also adds new columns to receipts table for enhanced transaction tracking

-- =============================================
-- 1. Add new columns to existing receipts table
-- =============================================
ALTER TABLE receipts
ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50),
ADD COLUMN IF NOT EXISTS notes TEXT,
ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS recurring_expense_id BIGINT;

-- =============================================
-- 2. Income Sources Table
-- =============================================
CREATE TABLE IF NOT EXISTS income_sources (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_name VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'BI_WEEKLY', 'MONTHLY', 'YEARLY', 'CUSTOM')),
    monthly_equivalent DECIMAL(12,2) NOT NULL,
    custom_days INT,
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_income_sources_user_id ON income_sources(user_id);
CREATE INDEX IF NOT EXISTS idx_income_sources_active ON income_sources(user_id, is_active) WHERE is_active = TRUE;

-- =============================================
-- 3. Recurring Expenses Table
-- =============================================
CREATE TABLE IF NOT EXISTS recurring_expenses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    category VARCHAR(50) NOT NULL,
    frequency VARCHAR(20) NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    next_due_date DATE NOT NULL,
    last_processed_date DATE,
    reminder_days INT DEFAULT 3,
    payment_method VARCHAR(50),
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    is_paused BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recurring_expenses_user_id ON recurring_expenses(user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_expenses_active ON recurring_expenses(user_id, is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_recurring_expenses_due_date ON recurring_expenses(next_due_date) WHERE is_active = TRUE AND is_paused = FALSE;

-- Add foreign key from receipts to recurring_expenses (only if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_receipts_recurring_expense'
    ) THEN
        ALTER TABLE receipts
        ADD CONSTRAINT fk_receipts_recurring_expense
        FOREIGN KEY (recurring_expense_id) REFERENCES recurring_expenses(id) ON DELETE SET NULL;
    END IF;
END $$;

-- =============================================
-- 4. Predefined Categories Table
-- =============================================
CREATE TABLE IF NOT EXISTS predefined_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    icon VARCHAR(50) NOT NULL DEFAULT 'category',
    color VARCHAR(20) NOT NULL DEFAULT '#3b82f6',
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

-- Insert predefined expense categories
INSERT INTO predefined_categories (name, icon, color, display_order) VALUES
    ('Food', 'restaurant', '#ef4444', 1),
    ('Transport', 'directions_car', '#f59e0b', 2),
    ('Bills', 'receipt_long', '#8b5cf6', 3),
    ('Shopping', 'shopping_bag', '#ec4899', 4),
    ('Entertainment', 'movie', '#3b82f6', 5),
    ('Health', 'local_hospital', '#10b981', 6),
    ('Groceries', 'shopping_cart', '#14b8a6', 7),
    ('Utilities', 'bolt', '#f97316', 8),
    ('Rent', 'home', '#6366f1', 9),
    ('Insurance', 'security', '#64748b', 10)
ON CONFLICT (name) DO NOTHING;

-- =============================================
-- 5. In-App Notifications Table
-- =============================================
CREATE TABLE IF NOT EXISTS app_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL CHECK (type IN ('BILL_REMINDER', 'BUDGET_ALERT', 'DAILY_SUMMARY', 'RECURRING_CREATED', 'SYSTEM')),
    title VARCHAR(200) NOT NULL,
    message TEXT,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_notifications_user_id ON app_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_app_notifications_unread ON app_notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX IF NOT EXISTS idx_app_notifications_created ON app_notifications(user_id, created_at DESC);

-- =============================================
-- 6. Comments for documentation
-- =============================================
COMMENT ON TABLE income_sources IS 'User income sources with various payment frequencies';
COMMENT ON COLUMN income_sources.frequency IS 'DAILY, WEEKLY, BI_WEEKLY (every 2 weeks), MONTHLY, YEARLY, CUSTOM';
COMMENT ON COLUMN income_sources.monthly_equivalent IS 'Pre-calculated monthly income for analytics';
COMMENT ON COLUMN income_sources.custom_days IS 'Number of days between payments when frequency is CUSTOM';

COMMENT ON TABLE recurring_expenses IS 'Recurring bills and expenses that auto-create transactions';
COMMENT ON COLUMN recurring_expenses.next_due_date IS 'Next date when this expense is due';
COMMENT ON COLUMN recurring_expenses.last_processed_date IS 'Last date a transaction was auto-created';
COMMENT ON COLUMN recurring_expenses.reminder_days IS 'Days before due date to send reminder notification';

COMMENT ON TABLE predefined_categories IS 'System-wide predefined expense categories';

COMMENT ON TABLE app_notifications IS 'In-app notifications for users (no email)';
COMMENT ON COLUMN app_notifications.type IS 'BILL_REMINDER, BUDGET_ALERT, DAILY_SUMMARY, RECURRING_CREATED, SYSTEM';
