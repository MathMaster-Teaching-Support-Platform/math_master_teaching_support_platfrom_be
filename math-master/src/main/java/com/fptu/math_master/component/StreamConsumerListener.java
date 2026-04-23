package com.fptu.math_master.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.entity.Notification;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.repository.NotificationRepository;
import com.fptu.math_master.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class StreamConsumerListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final ObjectMapper objectMapper;
    private final PushNotificationService pushNotificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;

    private static final String STREAM_KEY = "notifications";
    private static final String GROUP_NAME = "notif-group";
    private static final int MAX_RETRIES = 3;
    private static final String RETRY_KEY_PREFIX = "notif:retry:";
    private static final Duration RETRY_KEY_TTL = Duration.ofHours(24);

    @Override
    @Transactional
    public void onMessage(MapRecord<String, String, String> record) {
        try {
            String payloadStr = record.getValue().get("payload");
            if (payloadStr == null) {
                // Dummy message or malformed
                redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                return;
            }
            
            NotificationRequest notificationMessage = objectMapper.readValue(payloadStr, NotificationRequest.class);
            log.info("Received message from Redis Stream {}: {}", STREAM_KEY, notificationMessage);

            // 1. Save to DB (only if recipientId is a valid UUID, i.e., not "ALL")
            String recipientIdStr = notificationMessage.getRecipientId();
            if (recipientIdStr != null && !recipientIdStr.equals("ALL")) {
                User recipient = new User();
                recipient.setId(UUID.fromString(recipientIdStr));

                Notification notificationEntity = Notification.builder()
                        .recipient(recipient)
                        .type(notificationMessage.getType())
                        .title(notificationMessage.getTitle())
                        .content(notificationMessage.getContent())
                        .metadata(notificationMessage.getMetadata())
                        .isRead(false)
                        .build();

                // Do NOT set the ID from the stream message — let JPA auto-generate it.
                // Setting a specific UUID causes JPA to use merge() (update) semantics
                // instead of persist() (insert), which fails with OptimisticLockingException.

                notificationRepository.save(notificationEntity);
                log.info("Notification saved to DB for user: {}", recipientIdStr);
            }

            // 2. Forward to FCM Push Service
            pushNotificationService.sendNotification(notificationMessage);

            // 3. ACK on success
            redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
            log.info("Message {} acknowledged.", record.getId());
        } catch (Exception e) {
            String retryKey = RETRY_KEY_PREFIX + record.getId();
            Long retryCount = redisTemplate.opsForValue().increment(retryKey);
            redisTemplate.expire(retryKey, RETRY_KEY_TTL);

            if (retryCount != null && retryCount >= MAX_RETRIES) {
                log.error("Message {} exceeded max retries ({}), acknowledging as dead letter. Error: {}",
                        record.getId(), MAX_RETRIES, e.getMessage(), e);
                redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                redisTemplate.delete(retryKey);
            } else {
                log.error("Error processing Redis Stream message {} (attempt {}/{}), will remain in pending: {}",
                        record.getId(), retryCount, MAX_RETRIES, e.getMessage(), e);
            }
        }
    }
}
