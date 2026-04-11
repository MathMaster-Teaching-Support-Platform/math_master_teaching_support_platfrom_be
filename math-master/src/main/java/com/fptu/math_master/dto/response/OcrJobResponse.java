package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.OcrJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OCR job creation.
 * Returned immediately when admin triggers OCR verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrJobResponse {
    
    /**
     * Unique job identifier for tracking
     */
    private String jobId;
    
    /**
     * Initial job status (always PENDING)
     */
    private OcrJobStatus status;
    
    /**
     * Message for admin
     */
    private String message;
    
    /**
     * Polling endpoint URL
     */
    private String statusUrl;
}
