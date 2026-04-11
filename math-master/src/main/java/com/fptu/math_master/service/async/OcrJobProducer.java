package com.fptu.math_master.service.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.OcrAsyncProperties;
import com.fptu.math_master.dto.ocr.OcrJob;
import com.fptu.math_master.dto.ocr.OcrJobResult;
import com.fptu.math_master.enums.OcrJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Producer service for publishing OCR jobs to Redis Stream.
 * Handles job creation, status tracking, and queue management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrJobProducer {

    private final RedisTemplate<String, Object> ocrRedisTemplate;
    private final OcrAsyncProperties ocrAsyncProperties;
    private final ObjectMapper objectMapper;

    /**
     * Create and publish an OCR verification job
     *
     * @param profileId Teacher profile ID to verify
     * @param requestedBy Admin who requested verification
     * @return Job ID for tracking
     */
    public String createOcrJob(UUID profileId, UUID requestedBy) {
        return createOcrJob(profileId, requestedBy, 5); // Default priority
    }

    /**
     * Create and publish an OCR verification job with priority
     *
     * @param profileId Teacher profile ID to verify
     * @param requestedBy Admin who requested verification
     * @param priority Job priority (1-10, higher = more urgent)
     * @return Job ID for tracking
     */
    public String createOcrJob(UUID profileId, UUID requestedBy, Integer priority) {
        // Generate unique job ID
        String jobId = generateJobId();

        // Create job
        OcrJob job = OcrJob.builder()
                .jobId(jobId)
                .profileId(profileId)
                .requestedBy(requestedBy)
                .priority(priority)
                .createdAt(Instant.now())
                .retryCount(0)
                .maxRetries(ocrAsyncProperties.getMaxRetries())
                .build();

        try {
            // Publish to Redis Stream
            publishJob(job);

            // Initialize job status
            initializeJobStatus(job);

            log.info("Created OCR job: {} for profile: {}", jobId, profileId);
            return jobId;

        } catch (Exception e) {
            log.error("Failed to create OCR job for profile: {}", profileId, e);
            throw new RuntimeException("Failed to create OCR job", e);
        }
    }

    /**
     * Publish job to Redis Stream
     */
    private void publishJob(OcrJob job) {
        try {
            // Convert job to JSON string for stream
            String jobJson = objectMapper.writeValueAsString(job);

            // Create stream record with Map format
            java.util.Map<String, Object> jobMap = new java.util.HashMap<>();
            jobMap.put("jobData", jobJson);
            jobMap.put("jobId", job.getJobId());
            jobMap.put("profileId", job.getProfileId().toString());
            jobMap.put("priority", job.getPriority());

            // Add to stream
            ocrRedisTemplate.opsForStream().add(
                    ocrAsyncProperties.getStreamName(),
                    jobMap
            );

            log.debug("Published job {} to stream: {}", job.getJobId(), ocrAsyncProperties.getStreamName());

        } catch (Exception e) {
            log.error("Failed to publish job to stream", e);
            throw new RuntimeException("Failed to publish job", e);
        }
    }

    /**
     * Initialize job status in Redis
     */
    private void initializeJobStatus(OcrJob job) {
        OcrJobResult result = OcrJobResult.builder()
                .jobId(job.getJobId())
                .status(OcrJobStatus.PENDING)
                .progress(0)
                .createdAt(job.getCreatedAt())
                .retryCount(0)
                .build();

        saveJobResult(result);
    }

    /**
     * Save job result to Redis with TTL
     */
    private void saveJobResult(OcrJobResult result) {
        String key = getJobResultKey(result.getJobId());
        ocrRedisTemplate.opsForValue().set(
                key,
                result,
                Duration.ofSeconds(ocrAsyncProperties.getJobTtl())
        );
    }

    /**
     * Generate unique job ID
     */
    private String generateJobId() {
        return "ocr_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Get Redis key for job result
     */
    private String getJobResultKey(String jobId) {
        return "ocr:job:" + jobId + ":result";
    }

    /**
     * Get job count in queue (pending jobs)
     */
    public Long getQueueDepth() {
        try {
            return ocrRedisTemplate.opsForStream()
                    .size(ocrAsyncProperties.getStreamName());
        } catch (Exception e) {
            log.error("Failed to get queue depth", e);
            return 0L;
        }
    }

    /**
     * Check if async processing is enabled
     */
    public boolean isAsyncEnabled() {
        return ocrAsyncProperties.isEnabled();
    }
}
