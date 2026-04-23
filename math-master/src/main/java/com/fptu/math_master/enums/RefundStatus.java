package com.fptu.math_master.enums;

public enum RefundStatus {
    PENDING,      // Refund request submitted, awaiting review
    APPROVED,     // Refund approved, awaiting processing
    PROCESSING,   // Refund being processed
    COMPLETED,    // Refund completed successfully
    REJECTED      // Refund request rejected
}