-- Add "Other" to predefined categories
INSERT INTO predefined_categories (name, icon, color, display_order)
VALUES ('Other', 'category', '#6b7280', 11)
ON CONFLICT (name) DO NOTHING;
