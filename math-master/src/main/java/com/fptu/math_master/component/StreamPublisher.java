package com.fptu.math_master.component;

import com.fptu.math_master.dto.request.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class StreamPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final String STREAM_KEY = "notifications";
    private static final int MAX_LEN = 10000;

    public void publish(NotificationRequest message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message to JSON", e);
            return;
        }

        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .in(STREAM_KEY)
                .ofMap(java.util.Collections.singletonMap("payload", jsonPayload));

        log.info("Publishing message to Redis Stream {}: {}", STREAM_KEY, message);

        // Use XADD with MAXLEN ~10000
        stringRedisTemplate.opsForStream().add(record);

        // Trim the stream to maintain size
        stringRedisTemplate.opsForStream().trim(STREAM_KEY, MAX_LEN, true);
    }
}
