package com.fptu.math_master.dto.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * OCR verification job data model.
 * Represents a job in the Redis Stream queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrJob {
    
    /**
     * Unique job identifier
     */
    private String jobId;
    
    /**
     * Teacher profile ID to verify
     */
    private UUID profileId;
    
    /**
     * Admin who requested verification
     */
    private UUID requestedBy;
    
    /**
     * Job priority (1-10, higher = more urgent)
     */
    @Builder.Default
    private Integer priority = 5;
    
    /**
     * When job was created
     */
    private Instant createdAt;
    
    /**
     * Current retry count
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * Maximum retry attempts
     */
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * Check if job can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    /**
     * Increment retry count
     */
    public void incrementRetry() {
        this.retryCount++;
    }
}
