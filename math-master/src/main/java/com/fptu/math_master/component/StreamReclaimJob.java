package com.fptu.math_master.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class StreamReclaimJob {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STREAM_KEY = "notifications";
    private static final String GROUP_NAME = "notif-group";
    private static final Duration MIN_IDLE_TIME = Duration.ofMinutes(5);

    @Scheduled(fixedDelay = 60000) // Run every minute
    public void reclaimPendingMessages() {
        try {
            String consumerName = getConsumerName();
            log.info("Checking for pending messages in stream {}...", STREAM_KEY);

            // Manual pending check instead of autoClaim for better compatibility
            PendingMessages pendingMessages = redisTemplate.opsForStream()
                    .pending(STREAM_KEY, GROUP_NAME, Range.unbounded(), 100);

            if (pendingMessages != null && !pendingMessages.isEmpty()) {
                for (PendingMessage pm : pendingMessages) {
                    if (pm.getElapsedTimeSinceLastDelivery().compareTo(MIN_IDLE_TIME) > 0) {
                        log.info("Reclaiming stuck message: {} (Idle for {}ms)",
                                pm.getIdAsString(), pm.getElapsedTimeSinceLastDelivery().toMillis());

                        // Claim the message (move it to our consumer)
                        // Note: actual processing happens in the next poll cycles
                        redisTemplate.opsForStream().claim(
                                STREAM_KEY,
                                GROUP_NAME,
                                consumerName,
                                MIN_IDLE_TIME,
                                pm.getId()
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during message reclaim: {}", e.getMessage());
        }
    }

    private String getConsumerName() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-reclaimer-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (UnknownHostException e) {
            return "reclaimer-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
