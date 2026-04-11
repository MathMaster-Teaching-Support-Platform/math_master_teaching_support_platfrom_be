package com.fptu.math_master.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StreamInitializer implements ApplicationRunner {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String STREAM_KEY = "notifications";
    private static final String GROUP_NAME = "notif-group";

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Check if stream exists, if not create it by creating a group
            // XGROUP CREATE notifications notif-group $ MKSTREAM
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(STREAM_KEY))) {
                log.info("Stream {} not found, creating with group {}", STREAM_KEY, GROUP_NAME);
                redisTemplate.opsForStream().add(STREAM_KEY, java.util.Collections.singletonMap("_ign", "init_stream"));
                try {
                    redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
                } catch (Exception e) {
                    log.info("Group already exists or could not be created.");
                }
            } else {
                try {
                    redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
                    log.info("Created consumer group {} for stream {}", GROUP_NAME, STREAM_KEY);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    String causeMsg = (e.getCause() != null && e.getCause().getMessage() != null) ? e.getCause().getMessage() : "";
                    if (msg.contains("BUSYGROUP") || causeMsg.contains("BUSYGROUP")) {
                        log.info("Consumer group {} already exists for stream {}", GROUP_NAME, STREAM_KEY);
                    } else {
                        log.error("Error creating consumer group: {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize Redis Stream: {}", e.getMessage());
        }
    }
}
