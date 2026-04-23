-- ============================================================================
-- Migration: Create Order and Refund Request Tables
-- Description: Creates tables for the new billing flow with order confirmation
-- Author: System
-- Date: 2026-04-23
-- ============================================================================

-- ─── Orders Table ────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    course_id UUID NOT NULL,
    enrollment_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    order_number VARCHAR(50) NOT NULL UNIQUE,
    original_price DECIMAL(15, 2),
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    final_price DECIMAL(15, 2) NOT NULL,
    instructor_earnings DECIMAL(15, 2),
    platform_commission DECIMAL(15, 2),
    expires_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    CONSTRAINT fk_orders_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE SET NULL,
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_orders_final_price CHECK (final_price >= 0)
);

-- Indexes for orders table
CREATE INDEX idx_orders_student ON orders(student_id);
CREATE INDEX idx_orders_course ON orders(course_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_expires_at ON orders(expires_at);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- ─── Refund Requests Table ───────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS refund_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    student_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    refund_amount DECIMAL(15, 2) NOT NULL,
    instructor_deduction DECIMAL(15, 2),
    platform_deduction DECIMAL(15, 2),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processed_by UUID,
    admin_notes TEXT,
    is_auto_approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    
    CONSTRAINT fk_refund_requests_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_refund_requests_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_refund_requests_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refund_requests_processor FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_refund_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'PROCESSING', 'COMPLETED', 'REJECTED')),
    CONSTRAINT chk_refund_requests_amount CHECK (refund_amount >= 0)
);

-- Indexes for refund_requests table
CREATE INDEX idx_refund_requests_order ON refund_requests(order_id);
CREATE INDEX idx_refund_requests_enrollment ON refund_requests(enrollment_id);
CREATE INDEX idx_refund_requests_student ON refund_requests(student_id);
CREATE INDEX idx_refund_requests_status ON refund_requests(status);
CREATE INDEX idx_refund_requests_requested_at ON refund_requests(requested_at);

-- ─── Update Transactions Table ───────────────────────────────────────────

-- Add order_id column to transactions table if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'transactions' AND column_name = 'order_id'
    ) THEN
        ALTER TABLE transactions ADD COLUMN order_id UUID;
        ALTER TABLE transactions ADD CONSTRAINT fk_transactions_order 
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
        CREATE INDEX idx_transactions_order ON transactions(order_id);
    END IF;
END $$;

-- ─── Comments ─────────────────────────────────────────────────────────────

COMMENT ON TABLE orders IS 'Stores course purchase orders with confirmation workflow';
COMMENT ON COLUMN orders.status IS 'Order status: PENDING (awaiting confirmation), PROCESSING (payment in progress), COMPLETED (successful), FAILED (payment failed), CANCELLED (user cancelled), REFUNDED (refund processed)';
COMMENT ON COLUMN orders.expires_at IS 'Order expiry time (15 minutes from creation)';
COMMENT ON COLUMN orders.instructor_earnings IS '90% of final price goes to instructor';
COMMENT ON COLUMN orders.platform_commission IS '10% of final price goes to platform';

COMMENT ON TABLE refund_requests IS 'Stores refund requests with approval workflow';
COMMENT ON COLUMN refund_requests.status IS 'Refund status: PENDING (awaiting approval), APPROVED (approved by admin), PROCESSING (refund in progress), COMPLETED (refund processed), REJECTED (rejected by admin)';
COMMENT ON COLUMN refund_requests.is_auto_approved IS 'TRUE if refund was auto-approved (within 24h or <10% progress)';
COMMENT ON COLUMN refund_requests.instructor_deduction IS 'Amount to deduct from instructor wallet';
COMMENT ON COLUMN refund_requests.platform_deduction IS 'Amount to deduct from platform commission';
