package com.fptu.math_master.service.async;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.ocr.OcrJob;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.response.OcrComparisonResult;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.enums.OcrJobStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.service.OcrService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final StreamPublisher streamPublisher;

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
     * AUTO-REJECT if OCR verification fails
     */
    private void updateProfileWithOcrResults(TeacherProfile profile, OcrComparisonResult result) {
        try {
            profile.setOcrVerified(true);
            profile.setOcrMatchScore(result.getMatchScore());
            profile.setOcrVerificationData(objectMapper.writeValueAsString(result));
            profile.setOcrVerifiedAt(LocalDateTime.now());
            
            // AUTO-REJECT if OCR verification failed (3 mandatory fields not matched)
            if (!result.getIsMatch()) {
                profile.setStatus(com.fptu.math_master.enums.ProfileStatus.REJECTED);
                profile.setAdminComment("❌ TỰ ĐỘNG TỪ CHỐI: Xác minh OCR thất bại. " + result.getSummary());
                profile.setReviewedAt(LocalDateTime.now());
                
                log.warn("Profile {} AUTO-REJECTED due to OCR verification failure: {}", 
                        profile.getId(), result.getSummary());

                notifyTeacherProfileResult(profile, false,
                        "Hồ sơ không qua được xác minh tự động: " + result.getSummary());
            } else {
                log.info("Profile {} OCR verification passed. Keeping PENDING status for admin review.", 
                        profile.getId());
            }
            
            teacherProfileRepository.save(profile);
            
            log.debug("Updated profile {} with OCR results", profile.getId());
            
        } catch (Exception e) {
            log.error("Failed to update profile with OCR results", e);
            throw new RuntimeException("Failed to save OCR results", e);
        }
    }

    /**
     * Handle job failure with retry logic
     * AUTO-REJECT profile if job fails after all retries
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
            
            // AUTO-REJECT profile if OCR job failed completely
            try {
                TeacherProfile profile = teacherProfileRepository.findById(job.getProfileId())
                        .orElse(null);
                
                if (profile != null && profile.getStatus() == com.fptu.math_master.enums.ProfileStatus.PENDING) {
                    profile.setStatus(com.fptu.math_master.enums.ProfileStatus.REJECTED);
                    profile.setAdminComment("❌ TỰ ĐỘNG TỪ CHỐI: Không thể xác minh OCR. Lỗi: " + errorMessage);
                    profile.setReviewedAt(LocalDateTime.now());
                    profile.setOcrVerified(false);
                    
                    teacherProfileRepository.save(profile);

                    notifyTeacherProfileResult(profile, false,
                            "Xác minh hồ sơ gặp lỗi kỹ thuật. Vui lòng gửi lại hồ sơ hoặc liên hệ hỗ trợ.");
                    
                    log.warn("Profile {} AUTO-REJECTED due to OCR job failure after {} retries", 
                            profile.getId(), job.getMaxRetries());
                }
            } catch (Exception e) {
                log.error("Failed to auto-reject profile after OCR job failure", e);
            }
        }
    }

    /**
     * Send an in-app notification to the teacher about the OCR auto-decision.
     */
    private void notifyTeacherProfileResult(TeacherProfile profile, boolean approved, String reason) {
        try {
            String teacherUserId = profile.getUser().getId().toString();
            NotificationRequest notif = NotificationRequest.builder()
                    .id(UUID.randomUUID().toString())
                    .type("PROFILE_VERIFICATION")
                    .recipientId(teacherUserId)
                    .actionUrl("/submit-teacher-profile")
                    .timestamp(LocalDateTime.now())
                    .build();

            if (approved) {
                notif.setTitle("Hồ sơ Giáo viên được phê duyệt");
                notif.setContent("Chúc mừng! Hồ sơ của bạn đã được xác minh thành công.");
            } else {
                notif.setTitle("Hồ sơ Giáo viên bị từ chối");
                notif.setContent(reason);
            }

            streamPublisher.publish(notif);
            log.info("Notification sent to teacher {} for profile {} (approved={})",
                    teacherUserId, profile.getId(), approved);
        } catch (Exception e) {
            log.error("Failed to send profile result notification for profile {}", profile.getId(), e);
        }
    }
}
