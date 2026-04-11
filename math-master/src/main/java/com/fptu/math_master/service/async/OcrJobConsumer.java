package com.fptu.math_master.service.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.OcrAsyncProperties;
import com.fptu.math_master.dto.ocr.OcrJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumer service for reading and processing OCR jobs from Redis Stream.
 * Implements async job processing with concurrent workers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@EnableAsync
@ConditionalOnProperty(prefix = "ocr.async", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OcrJobConsumer {

    private final RedisTemplate<String, Object> ocrRedisTemplate;
    private final OcrAsyncProperties ocrAsyncProperties;
    private final OcrJobProcessor jobProcessor;
    private final ObjectMapper objectMapper;
    private final Consumer consumer;

    private ExecutorService executorService;
    private volatile boolean running = false;

    @PostConstruct
    public void init() {
        // Create thread pool for concurrent job processing
        executorService = Executors.newFixedThreadPool(
                ocrAsyncProperties.getConcurrency(),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("ocr-worker-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                }
        );

        running = true;
        log.info("Initialized OCR Job Consumer with {} workers", 
                ocrAsyncProperties.getConcurrency());
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
                log.info("OCR Job Consumer shut down successfully");
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Poll for new jobs from Redis Stream
     * Runs every 1 second
     */
    @Scheduled(fixedDelay = 1000)
    public void pollJobs() {
        if (!running) {
            return;
        }

        try {
            // Read messages from stream
            @SuppressWarnings("unchecked")
            List<org.springframework.data.redis.connection.stream.MapRecord<String, Object, Object>> messages = 
                    (List<org.springframework.data.redis.connection.stream.MapRecord<String, Object, Object>>) 
                    ocrRedisTemplate
                    .opsForStream()
                    .read(
                            consumer,
                            StreamOffset.create(
                                    ocrAsyncProperties.getStreamName(),
                                    ReadOffset.lastConsumed()
                            )
                    );

            if (messages != null && !messages.isEmpty()) {
                log.debug("Received {} OCR jobs from stream", messages.size());
                
                for (org.springframework.data.redis.connection.stream.MapRecord<String, Object, Object> message : messages) {
                    processMessageAsync(message);
                }
            }

        } catch (Exception e) {
            log.error("Error polling jobs from stream", e);
        }
    }

    /**
     * Process message asynchronously
     */
    @Async
    public void processMessageAsync(org.springframework.data.redis.connection.stream.MapRecord<String, Object, Object> message) {
        executorService.submit(() -> processMessage(message));
    }

    /**
     * Process a single message from the stream
     */
    private void processMessage(org.springframework.data.redis.connection.stream.MapRecord<String, Object, Object> message) {
        try {
            // Extract job data from message
            Object jobData = message.getValue().get("jobData");
            if (jobData == null) {
                log.error("No jobData found in message: {}", message.getId());
                return;
            }
            
            // Parse job from message
            String jobJson = jobData.toString();
            OcrJob job = objectMapper.readValue(jobJson, OcrJob.class);
            
            log.info("Processing job: {} for profile: {}", 
                    job.getJobId(), job.getProfileId());
            
            // Process the job
            jobProcessor.processJob(job);
            
            // Acknowledge message (remove from pending)
            acknowledgeMessage(message);
            
        } catch (Exception e) {
            log.error("Error processing message: {}", message.getId(), e);
            
            // Don't acknowledge - message will be retried
            // Could implement dead letter queue here
        }
    }

    /**
     * Acknowledge message (mark as processed)
     */
    private void acknowledgeMessage(org.springframework.data.redis.connection.stream.MapRecord<String, Object, Object> message) {
        try {
            ocrRedisTemplate.opsForStream().acknowledge(
                    ocrAsyncProperties.getStreamName(),
                    ocrAsyncProperties.getConsumerGroup(),
                    message.getId()
            );
            
            log.debug("Acknowledged message: {}", message.getId());
            
        } catch (Exception e) {
            log.error("Failed to acknowledge message: {}", message.getId(), e);
        }
    }

    /**
     * Get consumer statistics
     */
    public ConsumerStats getStats() {
        try {
            Long pendingCount = ocrRedisTemplate.opsForStream()
                    .size(ocrAsyncProperties.getStreamName());
            
            return ConsumerStats.builder()
                    .consumerName(ocrAsyncProperties.getConsumerName())
                    .concurrency(ocrAsyncProperties.getConcurrency())
                    .pendingJobs(pendingCount != null ? pendingCount : 0L)
                    .running(running)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get consumer stats", e);
            return ConsumerStats.builder()
                    .consumerName(ocrAsyncProperties.getConsumerName())
                    .running(running)
                    .build();
        }
    }

    /**
     * Consumer statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class ConsumerStats {
        private String consumerName;
        private Integer concurrency;
        private Long pendingJobs;
        private Boolean running;
    }
}
