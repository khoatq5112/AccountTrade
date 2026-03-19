-- Migration Script: Transaction Workflow System
-- Created: 2026-03-17
-- Description: Creates all tables for the complete transaction workflow including
--              orders, payments, escrow, disputes, and audit logging

-- =====================================================
-- SECTION 1: STATUS REFERENCE TABLES
-- =====================================================

-- Order Status Reference Table
CREATE TABLE IF NOT EXISTS order_statuses (
    status_id INT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_name (status_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payment Status Reference Table
CREATE TABLE IF NOT EXISTS payment_statuses (
    status_id INT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_name (status_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Escrow Status Reference Table
CREATE TABLE IF NOT EXISTS escrow_statuses (
    status_id INT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_name (status_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dispute Status Reference Table
CREATE TABLE IF NOT EXISTS dispute_statuses (
    status_id INT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_name (status_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 2: ORDER MANAGEMENT TABLES
-- =====================================================

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    buyer_id INT NOT NULL,
    status_id INT NOT NULL,
    total_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) DEFAULT 'VND',
    notes TEXT,
    
    -- Timestamps for order lifecycle tracking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    
    -- Timeout tracking for auto-completion
    confirmation_deadline TIMESTAMP NULL,
    
    -- Optimistic locking
    version INT DEFAULT 0,
    
    -- Foreign Keys
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users(user_id),
    CONSTRAINT fk_orders_status FOREIGN KEY (status_id) REFERENCES order_statuses(status_id),
    
    -- Indexes for common queries
    INDEX idx_order_number (order_number),
    INDEX idx_orders_buyer (buyer_id),
    INDEX idx_orders_status (status_id),
    INDEX idx_orders_created_at (created_at),
    INDEX idx_orders_confirmation_deadline (confirmation_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    post_id INT NOT NULL,
    credential_id INT NULL, -- Link to specific credential assigned
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(19, 4) NOT NULL,
    subtotal DECIMAL(19, 4) NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_post FOREIGN KEY (post_id) REFERENCES posts(post_id),
    CONSTRAINT fk_order_items_credential FOREIGN KEY (credential_id) REFERENCES post_credentials(credential_id),
    
    -- Indexes
    INDEX idx_order_items_order (order_id),
    INDEX idx_order_items_post (post_id),
    INDEX idx_order_items_credential (credential_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 3: PAYMENT PROCESSING TABLES
-- =====================================================

-- Payments Table
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    
    -- VNPAY Transaction Identifiers
    vnp_txn_ref VARCHAR(100) NOT NULL UNIQUE, -- Our reference (matches order_number usually)
    vnp_transaction_no VARCHAR(100), -- VNPAY's transaction number
    vnp_bank_code VARCHAR(50),
    vnp_card_type VARCHAR(50),
    vnp_order_info TEXT,
    
    -- Amount and Currency
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    
    -- Status
    status_id INT NOT NULL,
    
    -- IPN Data
    vnp_response_code VARCHAR(10),
    vnp_transaction_status VARCHAR(10),
    vnp_secure_hash TEXT, -- Store the hash for verification
    
    -- Timestamps
    initiated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    failed_at TIMESTAMP NULL,
    
    -- Error handling
    failure_reason TEXT,
    retry_count INT DEFAULT 0,
    
    -- Optimistic locking
    version INT DEFAULT 0,
    
    -- Foreign Keys
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_payments_status FOREIGN KEY (status_id) REFERENCES payment_statuses(status_id),
    
    -- Indexes
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_vnp_txn_ref (vnp_txn_ref),
    INDEX idx_payments_vnp_transaction_no (vnp_transaction_no),
    INDEX idx_payments_status (status_id),
    INDEX idx_payments_initiated_at (initiated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payment Callbacks Table (for logging all IPN and return callbacks)
CREATE TABLE IF NOT EXISTS payment_callbacks (
    callback_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NULL, -- May be null if payment not found
    
    -- Callback Type
    callback_type ENUM('IPN', 'RETURN') NOT NULL,
    
    -- Raw callback data
    raw_query_string TEXT,
    vnp_txn_ref VARCHAR(100),
    vnp_transaction_no VARCHAR(100),
    vnp_response_code VARCHAR(10),
    vnp_transaction_status VARCHAR(10),
    vnp_amount BIGINT, -- Amount in VNPAY format (x100)
    vnp_bank_code VARCHAR(50),
    vnp_card_type VARCHAR(50),
    vnp_order_info TEXT,
    vnp_secure_hash TEXT,
    
    -- Processing Status
    processed TINYINT(1) DEFAULT 0,
    processed_at TIMESTAMP NULL,
    processing_result TEXT,
    
    -- Timestamps
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    -- Foreign Keys
    CONSTRAINT fk_payment_callbacks_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE SET NULL,
    
    -- Indexes
    INDEX idx_payment_callbacks_payment (payment_id),
    INDEX idx_payment_callbacks_vnp_txn_ref (vnp_txn_ref),
    INDEX idx_payment_callbacks_type (callback_type),
    INDEX idx_payment_callbacks_processed (processed),
    INDEX idx_payment_callbacks_received_at (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 4: ESCROW SYSTEM TABLES
-- =====================================================

-- Escrow Table
CREATE TABLE IF NOT EXISTS escrows (
    escrow_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE, -- One escrow per order
    
    -- Amounts
    gross_amount DECIMAL(19, 4) NOT NULL, -- Total order amount
    platform_fee DECIMAL(19, 4) DEFAULT 0.0000, -- Platform commission
    net_amount DECIMAL(19, 4) NOT NULL, -- Amount to be released to seller
    
    -- Status
    status_id INT NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    release_scheduled_at TIMESTAMP NULL, -- When escrow is scheduled for release
    released_at TIMESTAMP NULL,
    refunded_at TIMESTAMP NULL,
    
    -- Release information
    release_reference VARCHAR(100), -- Reference for the payout transaction
    
    -- Optimistic locking
    version INT DEFAULT 0,
    
    -- Foreign Keys
    CONSTRAINT fk_escrows_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_escrows_status FOREIGN KEY (status_id) REFERENCES escrow_statuses(status_id),
    
    -- Indexes
    INDEX idx_escrows_order (order_id),
    INDEX idx_escrows_status (status_id),
    INDEX idx_escrows_release_scheduled (release_scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Escrow Transactions Table (audit trail of all escrow movements)
CREATE TABLE IF NOT EXISTS escrow_transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    escrow_id BIGINT NOT NULL,
    
    -- Transaction Type
    transaction_type ENUM('HOLD', 'RELEASE', 'REFUND', 'PARTIAL_REFUND', 'FREEZE', 'UNFREEZE') NOT NULL,
    
    -- Amounts
    amount DECIMAL(19, 4) NOT NULL,
    balance_before DECIMAL(19, 4) NOT NULL,
    balance_after DECIMAL(19, 4) NOT NULL,
    
    -- Reason and Reference
    reason VARCHAR(255),
    reference VARCHAR(100),
    
    -- Who initiated this transaction
    initiated_by INT, -- User ID (admin or system)
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_escrow_transactions_escrow FOREIGN KEY (escrow_id) REFERENCES escrows(escrow_id),
    CONSTRAINT fk_escrow_transactions_initiated_by FOREIGN KEY (initiated_by) REFERENCES users(user_id),
    
    -- Indexes
    INDEX idx_escrow_transactions_escrow (escrow_id),
    INDEX idx_escrow_transactions_type (transaction_type),
    INDEX idx_escrow_transactions_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 5: CREDENTIAL ASSIGNMENT TABLE
-- =====================================================

-- Credential Assignments Table (tracks which credentials are assigned to which orders)
CREATE TABLE IF NOT EXISTS credential_assignments (
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credential_id INT NOT NULL,
    order_item_id BIGINT NOT NULL,
    
    -- Assignment Status
    assignment_status ENUM('RESERVED', 'DELIVERED', 'CONFIRMED', 'DISPUTED', 'REPLACED', 'INVALID') NOT NULL DEFAULT 'RESERVED',
    
    -- Timestamps
    reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    confirmed_at TIMESTAMP NULL,
    disputed_at TIMESTAMP NULL,
    replaced_at TIMESTAMP NULL,
    
    -- Replacement tracking
    replaced_by_assignment_id BIGINT, -- If this credential was replaced, link to new assignment
    replacement_reason TEXT,
    
    -- Foreign Keys
    CONSTRAINT fk_credential_assignments_credential FOREIGN KEY (credential_id) REFERENCES post_credentials(credential_id),
    CONSTRAINT fk_credential_assignments_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id),
    CONSTRAINT fk_credential_assignments_replaced_by FOREIGN KEY (replaced_by_assignment_id) REFERENCES credential_assignments(assignment_id),
    
    -- Indexes
    INDEX idx_credential_assignments_credential (credential_id),
    INDEX idx_credential_assignments_order_item (order_item_id),
    INDEX idx_credential_assignments_status (assignment_status),
    UNIQUE INDEX idx_unique_credential_order (credential_id, order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 6: DISPUTE MANAGEMENT TABLES
-- =====================================================

-- Disputes Table
CREATE TABLE IF NOT EXISTS disputes (
    dispute_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    
    -- Participants
    opened_by INT NOT NULL, -- User who opened the dispute (usually buyer)
    respondent_id INT, -- Other party (usually seller)
    
    -- Dispute Details
    dispute_type ENUM('INVALID_CREDENTIAL', 'CREDENTIAL_USED', 'CREDENTIAL_EXPIRED', 'NOT_AS_DESCRIBED', 'OTHER') NOT NULL,
    reason TEXT NOT NULL,
    
    -- Status
    status_id INT NOT NULL,
    
    -- Resolution
    resolution_type ENUM('REFUND_FULL', 'REFUND_PARTIAL', 'REPLACEMENT', 'RELEASE_TO_SELLER', 'NO_ACTION') NULL,
    resolution_notes TEXT,
    resolved_by INT, -- Admin who resolved
    
    -- Amounts (for partial refunds)
    refund_amount DECIMAL(19, 4),
    
    -- Timestamps
    opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    sla_deadline TIMESTAMP NULL, -- SLA deadline for response
    
    -- Evidence tracking
    buyer_evidence TEXT, -- JSON or text description of evidence provided
    seller_evidence TEXT,
    
    -- Optimistic locking
    version INT DEFAULT 0,
    
    -- Foreign Keys
    CONSTRAINT fk_disputes_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_disputes_opened_by FOREIGN KEY (opened_by) REFERENCES users(user_id),
    CONSTRAINT fk_disputes_respondent FOREIGN KEY (respondent_id) REFERENCES users(user_id),
    CONSTRAINT fk_disputes_status FOREIGN KEY (status_id) REFERENCES dispute_statuses(status_id),
    CONSTRAINT fk_disputes_resolved_by FOREIGN KEY (resolved_by) REFERENCES users(user_id),
    
    -- Indexes
    INDEX idx_disputes_order (order_id),
    INDEX idx_disputes_opened_by (opened_by),
    INDEX idx_disputes_status (status_id),
    INDEX idx_disputes_opened_at (opened_at),
    INDEX idx_disputes_sla_deadline (sla_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dispute Messages Table
CREATE TABLE IF NOT EXISTS dispute_messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispute_id BIGINT NOT NULL,
    sender_id INT NOT NULL,
    
    -- Message Content
    message TEXT NOT NULL,
    
    -- Attachments (stored as JSON array of file paths)
    attachments TEXT,
    
    -- Visibility
    is_internal TINYINT(1) DEFAULT 0, -- If true, only visible to admins
    
    -- Read Status
    read_at TIMESTAMP NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_dispute_messages_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(dispute_id) ON DELETE CASCADE,
    CONSTRAINT fk_dispute_messages_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
    
    -- Indexes
    INDEX idx_dispute_messages_dispute (dispute_id),
    INDEX idx_dispute_messages_sender (sender_id),
    INDEX idx_dispute_messages_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 7: ADMIN REVIEW AND REFUND TABLES
-- =====================================================

-- Admin Reviews Table (tracks manual admin interventions)
CREATE TABLE IF NOT EXISTS admin_reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Entity being reviewed
    entity_type ENUM('ORDER', 'PAYMENT', 'ESCROW', 'DISPUTE', 'REFUND', 'CREDENTIAL') NOT NULL,
    entity_id BIGINT NOT NULL,
    
    -- Review Details
    review_type ENUM('PAYMENT_MISMATCH', 'FAILED_CREDENTIAL', 'DISPUTE_RESOLUTION', 'SUSPICIOUS_ACTIVITY', 'MANUAL_RELEASE', 'OTHER') NOT NULL,
    reason TEXT NOT NULL,
    
    -- Status
    status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'ESCALATED') NOT NULL DEFAULT 'PENDING',
    
    -- Resolution
    resolution_notes TEXT,
    
    -- Admin Assignment
    assigned_to INT, -- Admin assigned to review
    reviewed_by_admin INT, -- Admin who completed the review
    
    -- Priority
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    
    -- SLA
    sla_deadline TIMESTAMP NULL,
    
    -- Foreign Keys
    CONSTRAINT fk_admin_reviews_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(user_id),
    CONSTRAINT fk_admin_reviews_reviewed_by FOREIGN KEY (reviewed_by_admin) REFERENCES users(user_id),
    
    -- Indexes
    INDEX idx_admin_reviews_entity (entity_type, entity_id),
    INDEX idx_admin_reviews_status (status),
    INDEX idx_admin_reviews_assigned (assigned_to),
    INDEX idx_admin_reviews_priority (priority),
    INDEX idx_admin_reviews_sla (sla_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Refund Requests Table
CREATE TABLE IF NOT EXISTS refund_requests (
    refund_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    dispute_id BIGINT NULL,
    
    -- Refund Details
    refund_type ENUM('FULL', 'PARTIAL') NOT NULL,
    requested_amount DECIMAL(19, 4) NOT NULL,
    approved_amount DECIMAL(19, 4),
    reason TEXT NOT NULL,
    
    -- Status
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'PROCESSED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    
    -- Requester and Approver
    requested_by INT NOT NULL,
    approved_by INT,
    
    -- Processing Details
    vnp_refund_txn VARCHAR(100), -- VNPAY refund transaction reference
    processing_notes TEXT,
    failure_reason TEXT,
    
    -- Timestamps
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    
    -- Foreign Keys
    CONSTRAINT fk_refund_requests_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
    CONSTRAINT fk_refund_requests_dispute FOREIGN KEY (dispute_id) REFERENCES disputes(dispute_id),
    CONSTRAINT fk_refund_requests_requested_by FOREIGN KEY (requested_by) REFERENCES users(user_id),
    CONSTRAINT fk_refund_requests_approved_by FOREIGN KEY (approved_by) REFERENCES users(user_id),
    
    -- Indexes
    INDEX idx_refund_requests_order (order_id),
    INDEX idx_refund_requests_dispute (dispute_id),
    INDEX idx_refund_requests_status (status),
    INDEX idx_refund_requests_requested_at (requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 8: AUDIT AND NOTIFICATION TABLES
-- =====================================================

-- Audit Logs Table (comprehensive audit trail)
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Actor
    performed_by INT, -- User who performed the action (NULL for system)
    
    -- Action Details
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    
    -- Data Changes
    old_values JSON,
    new_values JSON,
    
    -- Request Information
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_url VARCHAR(500),
    request_method VARCHAR(10),
    
    -- Additional Context
    description TEXT,
    metadata JSON, -- Additional context-specific data
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_audit_logs_performed_by FOREIGN KEY (performed_by) REFERENCES users(user_id),
    
    -- Indexes
    INDEX idx_audit_logs_performed_by (performed_by),
    INDEX idx_audit_logs_action (action),
    INDEX idx_audit_logs_entity (entity_type, entity_id),
    INDEX idx_audit_logs_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    
    -- Notification Content
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    
    -- Link to related entity
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    
    -- Status
    is_read TINYINT(1) DEFAULT 0,
    read_at TIMESTAMP NULL,
    
    -- Delivery
    email_sent TINYINT(1) DEFAULT 0,
    email_sent_at TIMESTAMP NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    
    -- Indexes
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_type (notification_type),
    INDEX idx_notifications_is_read (is_read),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 9: SEED DATA FOR STATUS TABLES
-- =====================================================

-- Order Statuses
INSERT INTO order_statuses (status_name, description) VALUES
('PENDING', 'Order created but awaiting payment'),
('AWAITING_PAYMENT', 'Order is waiting for payment confirmation'),
('PAYMENT_FAILED', 'Payment failed for this order'),
('PAYMENT_EXPIRED', 'Order expired due to payment timeout'),
('PAID', 'Payment received, order is being processed'),
('PROCESSING', 'Order is being processed, credentials being assigned'),
('CREDENTIAL_ASSIGNED', 'Credentials have been assigned to the order'),
('AWAITING_BUYER_CONFIRMATION', 'Waiting for buyer to confirm credential validity'),
('DISPUTED', 'Order is under dispute'),
('COMPLETED', 'Order completed successfully, escrow released'),
('CANCELLED', 'Order was cancelled'),
('REFUNDED', 'Order was refunded to buyer')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Payment Statuses
INSERT INTO payment_statuses (status_name, description) VALUES
('INITIATED', 'Payment has been initiated, awaiting VNPAY redirect'),
('PENDING', 'Payment is pending at VNPAY'),
('PAID', 'Payment confirmed successful via IPN'),
('FAILED', 'Payment failed'),
('CALLBACK_MISMATCH', 'Payment callback data mismatch - requires manual review'),
('REFUNDED', 'Payment has been refunded')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Escrow Statuses
INSERT INTO escrow_statuses (status_name, description) VALUES
('NOT_CREATED', 'Escrow has not been created yet'),
('HOLDING', 'Funds are being held in escrow'),
('FROZEN', 'Escrow is frozen due to dispute or review'),
('RELEASED', 'Funds have been released to seller'),
('REFUNDED', 'Funds have been refunded to buyer'),
('PARTIALLY_REFUNDED', 'Funds have been partially refunded')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Dispute Statuses
INSERT INTO dispute_statuses (status_name, description) VALUES
('OPENED', 'Dispute has been opened'),
('UNDER_REVIEW', 'Dispute is being reviewed by admin'),
('RESOLVED', 'Dispute has been resolved'),
('CANCELLED', 'Dispute was cancelled')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- =====================================================
-- SECTION 10: ADDITIONAL INDEXES FOR PERFORMANCE
-- =====================================================

-- Composite indexes for common query patterns
CREATE INDEX idx_orders_buyer_status ON orders(buyer_id, status_id);
CREATE INDEX idx_orders_status_created ON orders(status_id, created_at);
CREATE INDEX idx_payments_order_status ON payments(order_id, status_id);
CREATE INDEX idx_disputes_status_opened ON disputes(status_id, opened_at);

-- =====================================================
-- SECTION 11: VIEWS FOR REPORTING
-- =====================================================

-- View: Order Summary with Payment and Escrow Status
CREATE OR REPLACE VIEW v_order_summary AS
SELECT 
    o.order_id,
    o.order_number,
    o.buyer_id,
    u.username AS buyer_username,
    os.status_name AS order_status,
    o.total_amount,
    o.created_at AS order_created_at,
    o.paid_at,
    o.completed_at,
    p.vnp_txn_ref,
    p.vnp_transaction_no,
    ps.status_name AS payment_status,
    e.gross_amount AS escrow_amount,
    es.status_name AS escrow_status
FROM orders o
JOIN users u ON o.buyer_id = u.user_id
JOIN order_statuses os ON o.status_id = os.status_id
LEFT JOIN payments p ON o.order_id = p.order_id
LEFT JOIN payment_statuses ps ON p.status_id = ps.status_id
LEFT JOIN escrows e ON o.order_id = e.order_id
LEFT JOIN escrow_statuses es ON e.status_id = es.status_id;

-- View: Dispute Summary
CREATE OR REPLACE VIEW v_dispute_summary AS
SELECT 
    d.dispute_id,
    d.order_id,
    o.order_number,
    d.dispute_type,
    ds.status_name AS dispute_status,
    d.opened_at,
    d.resolved_at,
    d.resolution_type,
    buyer.username AS buyer_username,
    seller.username AS seller_username,
    d.refund_amount
FROM disputes d
JOIN orders o ON d.order_id = o.order_id
JOIN dispute_statuses ds ON d.status_id = ds.status_id
JOIN users buyer ON d.opened_by = buyer.user_id
LEFT JOIN users seller ON d.respondent_id = seller.user_id;

-- View: Escrow Summary for Admin
CREATE OR REPLACE VIEW v_escrow_summary AS
SELECT 
    e.escrow_id,
    e.order_id,
    o.order_number,
    e.gross_amount,
    e.platform_fee,
    e.net_amount,
    es.status_name AS escrow_status,
    e.created_at AS escrow_created_at,
    e.release_scheduled_at,
    e.released_at,
    seller.username AS seller_username,
    buyer.username AS buyer_username
FROM escrows e
JOIN orders o ON e.order_id = o.order_id
JOIN escrow_statuses es ON e.status_id = es.status_id
JOIN users buyer ON o.buyer_id = buyer.user_id
JOIN order_items oi ON o.order_id = oi.order_id
JOIN posts p ON oi.post_id = p.post_id
JOIN users seller ON p.seller = seller.user_id
GROUP BY e.escrow_id;

-- =====================================================
-- END OF MIGRATION
-- =====================================================
