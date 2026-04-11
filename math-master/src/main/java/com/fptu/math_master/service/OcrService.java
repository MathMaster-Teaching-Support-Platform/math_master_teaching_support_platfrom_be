package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.OcrComparisonResult;

import java.util.UUID;

/**
 * Service interface for OCR verification operations.
 */
public interface OcrService {
    
    /**
     * Process OCR verification for a teacher profile.
     * Downloads verification document from MinIO, extracts data using Gemini AI,
     * and compares with profile data.
     *
     * @param profileId Teacher profile ID
     * @return OCR comparison result
     */
    OcrComparisonResult processProfileVerification(UUID profileId);
}
