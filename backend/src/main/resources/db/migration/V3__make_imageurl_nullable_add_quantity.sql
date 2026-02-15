-- Make imageUrl nullable for manual entries
-- This allows users to create receipts manually without uploading an image
ALTER TABLE receipts ALTER COLUMN image_url DROP NOT NULL;

-- Add index for filtering manual entries (where imageUrl is null)
CREATE INDEX idx_receipts_imageurl_null ON receipts (user_id) WHERE image_url IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN receipts.image_url IS 'URL to receipt image in Cloudinary. NULL for manually created receipts.';
COMMENT ON COLUMN receipts.items IS 'JSONB array of line items. Enhanced structure includes quantity and unitPrice fields.';
