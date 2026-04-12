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
            log.debug("Fetching job result with key: {}", key);
            
            Object result = ocrRedisTemplate.opsForValue().get(key);
            
            if (result == null) {
                log.warn("Job result not found in Redis for jobId: {} (key: {})", jobId, key);
                return Optional.empty();
            }
            
            log.debug("Found result in Redis: type={}", result.getClass().getName());
            
            if (result instanceof OcrJobResult) {
                log.info("Successfully retrieved job result for jobId: {}", jobId);
                return Optional.of((OcrJobResult) result);
            }
            
            // Handle LinkedHashMap deserialization issue
            if (result instanceof java.util.Map) {
                log.warn("Result is Map (LinkedHashMap), attempting to convert to OcrJobResult for jobId: {}", jobId);
                try {
                    // Convert Map to OcrJobResult using ObjectMapper
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    
                    OcrJobResult converted = mapper.convertValue(result, OcrJobResult.class);
                    log.info("Successfully converted Map to OcrJobResult for jobId: {}", jobId);
                    
                    // Re-save with correct type
                    saveJobResult(converted);
                    log.debug("Re-saved job result with correct type for jobId: {}", jobId);
                    
                    return Optional.of(converted);
                } catch (Exception e) {
                    log.error("Failed to convert Map to OcrJobResult for jobId: {}", jobId, e);
                    return Optional.empty();
                }
            }
            
            log.error("Result found but wrong type: expected OcrJobResult, got {}", result.getClass().getName());
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
            result.setUpdatedAt(Instant.now());
            
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
            result.setUpdatedAt(Instant.now());
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
            Instant now = Instant.now();
            result.setStatus(OcrJobStatus.COMPLETED);
            result.setResult(ocrResult);
            result.setProgress(100);
            result.setCompletedAt(now);
            result.setUpdatedAt(now);
            
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
            Instant now = Instant.now();
            result.setStatus(OcrJobStatus.FAILED);
            result.setErrorMessage(errorMessage);
            result.setCompletedAt(now);
            result.setUpdatedAt(now);
            
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
            result.setUpdatedAt(Instant.now());
            
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
            Instant now = Instant.now();
            result.setStatus(OcrJobStatus.CANCELLED);
            result.setCompletedAt(now);
            result.setUpdatedAt(now);
            
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
            log.debug("Saving job result to Redis: key={}, status={}, progress={}", 
                    key, result.getStatus(), result.getProgress());
            
            ocrRedisTemplate.opsForValue().set(
                    key,
                    result,
                    Duration.ofSeconds(ocrAsyncProperties.getJobTtl())
            );
            
            // Verify save
            Object saved = ocrRedisTemplate.opsForValue().get(key);
            if (saved != null) {
                log.info("Job result saved successfully to Redis: jobId={}, key={}", result.getJobId(), key);
            } else {
                log.error("Job result save verification failed: jobId={}, key={}", result.getJobId(), key);
            }
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
