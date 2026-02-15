-- Add role column to users table for authentication
ALTER TABLE users
ADD COLUMN role VARCHAR(50) DEFAULT 'ROLE_USER';

-- Update existing users to have the default role
UPDATE users
SET role = 'ROLE_USER'
WHERE role IS NULL;
