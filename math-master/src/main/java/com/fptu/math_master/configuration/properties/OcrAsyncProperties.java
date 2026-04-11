package com.fptu.math_master.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for async OCR processing.
 */
@Configuration
@ConfigurationProperties(prefix = "ocr.async")
@Data
public class OcrAsyncProperties {
    
    /**
     * Enable/disable async OCR processing
     */
    private boolean enabled = true;
    
    /**
     * Redis Stream name for OCR jobs
     */
    private String streamName = "ocr:jobs";
    
    /**
     * Consumer group name
     */
    private String consumerGroup = "ocr-workers";
    
    /**
     * Consumer name (unique per instance)
     */
    private String consumerName = "worker-1";
    
    /**
     * Number of messages to read per batch
     */
    private Integer batchSize = 10;
    
    /**
     * Poll timeout in milliseconds
     */
    private Long pollTimeout = 5000L;
    
    /**
     * Maximum retry attempts for failed jobs
     */
    private Integer maxRetries = 3;
    
    /**
     * Delay between retries in milliseconds
     */
    private Long retryDelay = 5000L;
    
    /**
     * Job TTL in seconds (how long to keep job results)
     */
    private Long jobTtl = 86400L; // 24 hours
    
    /**
     * Number of concurrent worker threads
     */
    private Integer concurrency = 5;
    
    /**
     * Test mode flag
     */
    private boolean testMode = false;
}
