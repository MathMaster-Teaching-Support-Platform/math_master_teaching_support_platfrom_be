package com.fptu.math_master.enums;

public enum OrderStatus {
    PENDING,      // Order created, awaiting confirmation
    PROCESSING,   // Payment being processed
    COMPLETED,    // Payment successful, enrollment created
    FAILED,       // Payment failed
    CANCELLED,    // Order cancelled by user or expired
    REFUNDED      // Order refunded
}