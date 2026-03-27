USE `account_trading_system`;

START TRANSACTION;
SET FOREIGN_KEY_CHECKS = 0;

-- Runtime / transactional data
DELETE FROM notification_delivery_log;
DELETE FROM notifications;
DELETE FROM audit_logs;
DELETE FROM admin_reviews;
DELETE FROM refund_requests;
DELETE FROM payment_callbacks;
DELETE FROM platform_earnings;
DELETE FROM escrow_transactions;
DELETE FROM escrows;
DELETE FROM payments;
DELETE FROM order_items;
DELETE FROM disputes;
DELETE FROM orders;
DELETE FROM carts;
DELETE FROM credential_assignments;
DELETE FROM credential_access_logs;
DELETE FROM wallet_topups;
DELETE FROM wallet_transactions;

-- Catalog data re-seeded from seed-posts-data.sql
DELETE FROM post_credentials;
DELETE FROM posts;

-- Reset wallet balances while keeping wallet rows attached to users
UPDATE wallets
SET balance = 0,
    frozen_balance = 0,
    version = 0,
    updated_at = NOW();

ALTER TABLE notifications AUTO_INCREMENT = 1;
ALTER TABLE audit_logs AUTO_INCREMENT = 1;
ALTER TABLE admin_reviews AUTO_INCREMENT = 1;
ALTER TABLE refund_requests AUTO_INCREMENT = 1;
ALTER TABLE payment_callbacks AUTO_INCREMENT = 1;
ALTER TABLE platform_earnings AUTO_INCREMENT = 1;
ALTER TABLE escrow_transactions AUTO_INCREMENT = 1;
ALTER TABLE escrows AUTO_INCREMENT = 1;
ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE order_items AUTO_INCREMENT = 1;
ALTER TABLE disputes AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE carts AUTO_INCREMENT = 1;
ALTER TABLE credential_assignments AUTO_INCREMENT = 1;
ALTER TABLE credential_access_logs AUTO_INCREMENT = 1;
ALTER TABLE wallet_topups AUTO_INCREMENT = 1;
ALTER TABLE wallet_transactions AUTO_INCREMENT = 1;
ALTER TABLE post_credentials AUTO_INCREMENT = 1;
ALTER TABLE posts AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;
