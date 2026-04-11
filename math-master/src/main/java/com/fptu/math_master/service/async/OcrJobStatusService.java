package com.fptu.math_master.service.async;

import com.fptu.math_master.configuration.properties.OcrAsyncProperties;
import com.fptu.math_master.dto.ocr.OcrJobResult;
import com.fptu.math_master.dto.response.OcrComparisonResult;
import com.fptu.math_master.enums.OcrJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing OCR job status in Redis.
 * Handles status updates, progress tracking, and result storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrJobStatusService {

    private final RedisTemplate<String, Object> ocrRedisTemplate;
    private final OcrAsyncProperties ocrAsyncProperties;

    /**
     * Get job result from Redis
     *
     * @param jobId Job ID
     * @return Optional containing job result if found
     */
    public Optional<OcrJobResult> getJobResult(String jobId) {
        try {
            String key = getJobResultKey(jobId);
            Object result = ocrRedisTemplate.opsForValue().get(key);
            
            if (result instanceof OcrJobResult) {
                return Optional.of((OcrJobResult) result);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to get job result for: {}", jobId, e);
            return Optional.empty();
        }
    }

    /**
     * Update job status
     *
     * @param jobId Job ID
     * @param status New status
     */
    public void updateJobStatus(String jobId, OcrJobStatus status) {
        getJobResult(jobId).ifPresent(result -> {
            result.setStatus(status);
            
            if (status == OcrJobStatus.PROCESSING && result.getStartedAt() == null) {
                result.setStartedAt(Instant.now());
            }
            
            saveJobResult(result);
            log.debug("Updated job {} status to: {}", jobId, status);
        });
    }

    /**
     * Update job progress
     *
     * @param jobId Job ID
     * @param progress Progress percentage (0-100)
     */
    public void updateJobProgress(String jobId, Integer progress) {
        getJobResult(jobId).ifPresent(result -> {
            result.setProgress(Math.min(100, Math.max(0, progress)));
            saveJobResult(result);
            log.debug("Updated job {} progress to: {}%", jobId, progress);
        });
    }

    /**
     * Mark job as completed with result
     *
     * @param jobId Job ID
     * @param ocrResult OCR comparison result
     */
    public void completeJob(String jobId, OcrComparisonResult ocrResult) {
        getJobResult(jobId).ifPresent(result -> {
            result.setStatus(OcrJobStatus.COMPLETED);
            result.setResult(ocrResult);
            result.setProgress(100);
            result.setCompletedAt(Instant.now());
            
            // Calculate processing time
            if (result.getStartedAt() != null) {
                long processingTime = Duration.between(
                        result.getStartedAt(),
                        result.getCompletedAt()
                ).toMillis();
                result.setProcessingTimeMs(processingTime);
            }
            
            saveJobResult(result);
            log.info("Completed job {} with match score: {}%", 
                    jobId, ocrResult.getMatchScore());
        });
    }

    /**
     * Mark job as failed with error message
     *
     * @param jobId Job ID
     * @param errorMessage Error message
     */
    public void failJob(String jobId, String errorMessage) {
        getJobResult(jobId).ifPresent(result -> {
            result.setStatus(OcrJobStatus.FAILED);
            result.setErrorMessage(errorMessage);
            result.setCompletedAt(Instant.now());
            
            saveJobResult(result);
            log.error("Failed job {}: {}", jobId, errorMessage);
        });
    }

    /**
     * Increment retry count for job
     *
     * @param jobId Job ID
     */
    public void incrementRetryCount(String jobId) {
        getJobResult(jobId).ifPresent(result -> {
            result.setRetryCount(result.getRetryCount() + 1);
            result.setStatus(OcrJobStatus.PENDING); // Reset to pending for retry
            
            saveJobResult(result);
            log.debug("Incremented retry count for job {} to: {}", 
                    jobId, result.getRetryCount());
        });
    }

    /**
     * Cancel job
     *
     * @param jobId Job ID
     */
    public void cancelJob(String jobId) {
        getJobResult(jobId).ifPresent(result -> {
            result.setStatus(OcrJobStatus.CANCELLED);
            result.setCompletedAt(Instant.now());
            
            saveJobResult(result);
            log.info("Cancelled job: {}", jobId);
        });
    }

    /**
     * Save job result to Redis with TTL
     */
    private void saveJobResult(OcrJobResult result) {
        try {
            String key = getJobResultKey(result.getJobId());
            ocrRedisTemplate.opsForValue().set(
                    key,
                    result,
                    Duration.ofSeconds(ocrAsyncProperties.getJobTtl())
            );
        } catch (Exception e) {
            log.error("Failed to save job result for: {}", result.getJobId(), e);
        }
    }

    /**
     * Get Redis key for job result
     */
    private String getJobResultKey(String jobId) {
        return "ocr:job:" + jobId + ":result";
    }

    /**
     * Delete job result from Redis
     *
     * @param jobId Job ID
     */
    public void deleteJobResult(String jobId) {
        try {
            String key = getJobResultKey(jobId);
            ocrRedisTemplate.delete(key);
            log.debug("Deleted job result: {}", jobId);
        } catch (Exception e) {
            log.error("Failed to delete job result for: {}", jobId, e);
        }
    }
}
