package com.fptu.math_master.enums;

/**
 * OCR job status enum.
 * Represents the current state of an OCR verification job.
 */
public enum OcrJobStatus {
    /**
     * Job created but not yet picked up by worker
     */
    PENDING,
    
    /**
     * Job is currently being processed
     */
    PROCESSING,
    
    /**
     * Job completed successfully
     */
    COMPLETED,
    
    /**
     * Job failed after all retry attempts
     */
    FAILED,
    
    /**
     * Job was cancelled by user or system
     */
    CANCELLED
}
