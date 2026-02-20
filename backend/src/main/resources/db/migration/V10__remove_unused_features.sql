-- Remove recurring columns from receipts table
ALTER TABLE receipts DROP COLUMN IF EXISTS recurring_expense_id;
ALTER TABLE receipts DROP COLUMN IF EXISTS is_recurring;

-- Drop unused feature tables (FK-safe order)
DROP TABLE IF EXISTS budget_alerts;
DROP TABLE IF EXISTS app_notifications;
DROP TABLE IF EXISTS recurring_expenses;
