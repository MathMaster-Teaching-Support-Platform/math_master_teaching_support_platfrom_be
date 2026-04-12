package com.fptu.math_master.dto.ocr;

import com.fptu.math_master.dto.response.OcrComparisonResult;
import com.fptu.math_master.enums.OcrJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result of an OCR verification job.
 * Stored in Redis and returned to frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrJobResult {
    
    /**
     * Job ID
     */
    private String jobId;
    
    /**
     * Profile ID (for reference)
     */
    private String profileId;
    
    /**
     * Current job status
     */
    private OcrJobStatus status;
    
    /**
     * OCR comparison result (if completed)
     */
    private OcrComparisonResult result;
    
    /**
     * Error message (if failed)
     */
    private String errorMessage;
    
    /**
     * Processing progress (0-100)
     */
    @Builder.Default
    private Integer progress = 0;
    
    /**
     * When job was created
     */
    private Instant createdAt;
    
    /**
     * When job was last updated
     */
    private Instant updatedAt;
    
    /**
     * When job started processing
     */
    private Instant startedAt;
    
    /**
     * When job completed/failed
     */
    private Instant completedAt;
    
    /**
     * Processing duration in milliseconds
     */
    private Long processingTimeMs;
    
    /**
     * Number of retry attempts
     */
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * Check if job is terminal (completed or failed)
     */
    public boolean isTerminal() {
        return status == OcrJobStatus.COMPLETED 
            || status == OcrJobStatus.FAILED 
            || status == OcrJobStatus.CANCELLED;
    }
    
    /**
     * Check if job is still processing
     */
    public boolean isProcessing() {
        return status == OcrJobStatus.PENDING 
            || status == OcrJobStatus.PROCESSING;
    }
}
