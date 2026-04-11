package com.fptu.math_master.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.OcrAsyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;

/**
 * Configuration for Redis Streams used in async OCR processing.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

    private final OcrAsyncProperties ocrAsyncProperties;
    private final ObjectMapper objectMapper;

    /**
     * Configure RedisTemplate for OCR job operations
     */
    @Bean
    public RedisTemplate<String, Object> ocrRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Create Stream Message Listener Container for consuming OCR jobs
     * Note: We don't actually use this container for listening since we poll manually in OcrJobConsumer
     * This is kept for potential future use with reactive streams
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> ocrStreamListenerContainer(
            RedisConnectionFactory connectionFactory) {
        
        if (!ocrAsyncProperties.isEnabled()) {
            log.info("OCR async processing is disabled");
            return null;
        }

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(ocrAsyncProperties.getPollTimeout()))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        // Create consumer group if it doesn't exist
        try {
            connectionFactory
                    .getConnection()
                    .streamCommands()
                    .xGroupCreate(
                            ocrAsyncProperties.getStreamName().getBytes(),
                            ocrAsyncProperties.getConsumerGroup(),
                            ReadOffset.from("0-0"),
                            true);
            log.info("Created consumer group: {}", ocrAsyncProperties.getConsumerGroup());
        } catch (Exception e) {
            log.debug("Consumer group already exists or stream not created yet: {}", e.getMessage());
        }

        // Note: We don't start the container here since we use manual polling in OcrJobConsumer
        // container.start();
        log.info("OCR Stream Listener Container configured (manual polling mode)");
        
        return container;
    }

    /**
     * Create Consumer for reading from stream
     */
    @Bean
    public Consumer ocrConsumer() {
        return Consumer.from(
                ocrAsyncProperties.getConsumerGroup(),
                ocrAsyncProperties.getConsumerName());
    }
}
