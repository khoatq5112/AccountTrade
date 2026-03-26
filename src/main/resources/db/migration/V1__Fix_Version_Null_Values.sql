-- Fix NULL version values in all tables with @Version field
-- This migration sets version=0 for all existing records where version IS NULL

-- Update Wallets table
UPDATE Wallets SET version = 0 WHERE version IS NULL;

-- Update orders table
UPDATE orders SET version = 0 WHERE version IS NULL;

-- Update order_items table
UPDATE order_items SET version = 0 WHERE version IS NULL;

-- Update escrows table
UPDATE escrows SET version = 0 WHERE version IS NULL;

-- Update payments table
UPDATE payments SET version = 0 WHERE version IS NULL;

-- Update disputes table
UPDATE disputes SET version = 0 WHERE version IS NULL;

-- Update refund_requests table
UPDATE refund_requests SET version = 0 WHERE version IS NULL;

-- Update credential_assignments table
UPDATE credential_assignments SET version = 0 WHERE version IS NULL;
