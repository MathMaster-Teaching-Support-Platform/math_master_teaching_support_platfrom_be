package com.fptu.math_master.service.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.ocr.OcrJob;
import com.fptu.math_master.dto.response.OcrComparisonResult;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.enums.OcrJobStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Processor service for executing OCR verification jobs.
 * Contains the business logic for OCR processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrJobProcessor {

    private final OcrService ocrService;
    private final TeacherProfileRepository teacherProfileRepository;
    private final OcrJobStatusService jobStatusService;
    private final ObjectMapper objectMapper;

    /**
     * Process an OCR verification job
     *
     * @param job OCR job to process
     */
    @Transactional
    public void processJob(OcrJob job) {
        String jobId = job.getJobId();
        
        try {
            log.info("Processing OCR job: {} for profile: {}", jobId, job.getProfileId());
            
            // Update status to PROCESSING
            jobStatusService.updateJobStatus(jobId, OcrJobStatus.PROCESSING);
            jobStatusService.updateJobProgress(jobId, 10);
            
            // Get teacher profile
            TeacherProfile profile = teacherProfileRepository.findById(job.getProfileId())
                    .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
            
            jobStatusService.updateJobProgress(jobId, 20);
            
            // Perform OCR verification
            log.debug("Starting OCR verification for profile: {}", job.getProfileId());
            OcrComparisonResult result = ocrService.processProfileVerification(job.getProfileId());
            
            jobStatusService.updateJobProgress(jobId, 80);
            
            // Update profile with OCR results
            updateProfileWithOcrResults(profile, result);
            
            jobStatusService.updateJobProgress(jobId, 90);
            
            // Mark job as completed
            jobStatusService.completeJob(jobId, result);
            
            log.info("Successfully completed OCR job: {} with match score: {}%", 
                    jobId, result.getMatchScore());
            
        } catch (Exception e) {
            log.error("Failed to process OCR job: {}", jobId, e);
            handleJobFailure(job, e);
        }
    }

    /**
     * Update teacher profile with OCR results
     */
    private void updateProfileWithOcrResults(TeacherProfile profile, OcrComparisonResult result) {
        try {
            profile.setOcrVerified(true);
            profile.setOcrMatchScore(result.getMatchScore());
            profile.setOcrVerificationData(objectMapper.writeValueAsString(result));
            profile.setOcrVerifiedAt(LocalDateTime.now());
            
            teacherProfileRepository.save(profile);
            
            log.debug("Updated profile {} with OCR results", profile.getId());
            
        } catch (Exception e) {
            log.error("Failed to update profile with OCR results", e);
            throw new RuntimeException("Failed to save OCR results", e);
        }
    }

    /**
     * Handle job failure with retry logic
     */
    private void handleJobFailure(OcrJob job, Exception error) {
        String jobId = job.getJobId();
        String errorMessage = error.getMessage();
        
        // Check if job can be retried
        if (job.canRetry()) {
            log.warn("Job {} failed, will retry. Attempt {}/{}", 
                    jobId, job.getRetryCount() + 1, job.getMaxRetries());
            
            jobStatusService.incrementRetryCount(jobId);
            
            // Job will be picked up again by consumer
            // Could implement exponential backoff here
            
        } else {
            log.error("Job {} failed after {} attempts: {}", 
                    jobId, job.getMaxRetries(), errorMessage);
            
            jobStatusService.failJob(jobId, errorMessage);
        }
    }
}
