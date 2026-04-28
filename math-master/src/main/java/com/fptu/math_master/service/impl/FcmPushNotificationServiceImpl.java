package com.fptu.math_master.service.impl;

import com.fptu.math_master.configuration.properties.FirebaseProperties;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.UserFcmToken;
import com.fptu.math_master.repository.UserFcmTokenRepository;
import com.fptu.math_master.service.NotificationPreferenceService;
import com.fptu.math_master.service.PushNotificationService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.Notification;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushNotificationServiceImpl implements PushNotificationService {

  private static final int FCM_MULTICAST_BATCH = 500;

  private final UserFcmTokenRepository userFcmTokenRepository;
  private final FirebaseProperties firebaseProperties;
  private final NotificationPreferenceService notificationPreferenceService;

  private volatile FirebaseMessaging firebaseMessaging;

  @Override
  @Transactional
  public void registerToken(UUID userId, String token, String deviceInfo) {
    if (!StringUtils.hasText(token)) {
      return;
    }

    String normalizedToken = token.trim();

    UserFcmToken entity = userFcmTokenRepository.findByToken(normalizedToken).orElseGet(UserFcmToken::new);

    User userRef = new User();
    userRef.setId(userId);

    entity.setUser(userRef);
    entity.setToken(normalizedToken);
    entity.setDeviceInfo(trimToLength(deviceInfo, 255));
    entity.setIsActive(true);
    entity.setLastSeenAt(Instant.now());

    userFcmTokenRepository.save(entity);
  }

  @Override
  @Transactional
  public void unregisterToken(UUID userId, String token) {
    if (!StringUtils.hasText(token)) {
      return;
    }

    Optional<UserFcmToken> found = userFcmTokenRepository.findByToken(token.trim());
    if (found.isEmpty()) {
      return;
    }

    UserFcmToken entity = found.get();
    if (!entity.getUser().getId().equals(userId)) {
      return;
    }

    entity.setIsActive(false);
    entity.setLastSeenAt(Instant.now());
    userFcmTokenRepository.save(entity);
  }

  @Override
  @Transactional
  public void sendNotification(NotificationRequest notificationRequest) {
    FirebaseMessaging messaging = getFirebaseMessaging();
    if (messaging == null) {
      log.warn("Firebase messaging is not configured. Skip push delivery for notification {}", notificationRequest.getId());
      return;
    }

    List<UserFcmToken> destinations = resolveDestinations(notificationRequest.getRecipientId());
    if (destinations.isEmpty()) {
      log.debug("No active FCM destination tokens for recipientId={}", notificationRequest.getRecipientId());
      return;
    }

    // Filter destinations based on user preferences
    List<UserFcmToken> filteredDestinations = destinations.stream()
        .filter(token -> {
          try {
            return notificationPreferenceService.shouldSendPushNotification(
                token.getUser().getId(), 
                notificationRequest.getType()
            );
          } catch (Exception e) {
            log.warn("Failed to check push preference for user {}, defaulting to enabled", 
                token.getUser().getId(), e);
            return true; // Default to enabled on error
          }
        })
        .collect(Collectors.toList());

    if (filteredDestinations.isEmpty()) {
      log.debug("No users want push notifications for type {} and recipientId={}", 
          notificationRequest.getType(), notificationRequest.getRecipientId());
      return;
    }

    Map<String, String> data = toStringData(notificationRequest);
    List<String> tokens = filteredDestinations.stream().map(UserFcmToken::getToken).distinct().collect(Collectors.toList());

    for (int start = 0; start < tokens.size(); start += FCM_MULTICAST_BATCH) {
      int end = Math.min(start + FCM_MULTICAST_BATCH, tokens.size());
      List<String> batchTokens = tokens.subList(start, end);

      MulticastMessage.Builder builder = MulticastMessage.builder().addAllTokens(batchTokens).putAllData(data);

      if (StringUtils.hasText(notificationRequest.getTitle()) || StringUtils.hasText(notificationRequest.getContent())) {
        builder.setNotification(
            Notification.builder()
                .setTitle(notificationRequest.getTitle())
                .setBody(notificationRequest.getContent())
                .build());
      }

      try {
        BatchResponse response = messaging.sendEachForMulticast(builder.build());
        deactivateInvalidTokens(batchTokens, response.getResponses());
      } catch (FirebaseMessagingException e) {
        log.error("Failed to send FCM multicast for notification {}", notificationRequest.getId(), e);
      }
    }
  }

  private List<UserFcmToken> resolveDestinations(String recipientId) {
    if (!StringUtils.hasText(recipientId) || "ALL".equalsIgnoreCase(recipientId)) {
      return userFcmTokenRepository.findAllByIsActiveTrue();
    }

    try {
      UUID userId = UUID.fromString(recipientId);
      return userFcmTokenRepository.findAllByUser_IdAndIsActiveTrue(userId);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid recipientId '{}', fallback to no recipients", recipientId);
      return Collections.emptyList();
    }
  }

  private Map<String, String> toStringData(NotificationRequest request) {
    Map<String, String> data = new HashMap<>();

    putIfPresent(data, "id", request.getId());
    putIfPresent(data, "type", request.getType());
    putIfPresent(data, "title", request.getTitle());
    putIfPresent(data, "content", request.getContent());
    putIfPresent(data, "recipientId", request.getRecipientId());
    putIfPresent(data, "senderId", request.getSenderId());
    putIfPresent(data, "actionUrl", request.getActionUrl());

    if (request.getTimestamp() != null) {
      data.put("timestamp", request.getTimestamp().toString());
    }

    if (request.getMetadata() != null) {
      request.getMetadata().forEach((k, v) -> {
        if (k != null && v != null) {
          data.put("metadata." + k, String.valueOf(v));
        }
      });
    }

    return data;
  }

  private void putIfPresent(Map<String, String> data, String key, String value) {
    if (StringUtils.hasText(value)) {
      data.put(key, value);
    }
  }

  private void deactivateInvalidTokens(List<String> batchTokens, List<SendResponse> responses) {
    List<String> invalidTokens = new ArrayList<>();

    for (int i = 0; i < responses.size(); i++) {
      SendResponse response = responses.get(i);
      if (response.isSuccessful()) {
        continue;
      }

      String token = batchTokens.get(i);
      FirebaseMessagingException exception = response.getException();
      if (exception == null || exception.getMessagingErrorCode() == null) {
        continue;
      }

      String code = exception.getMessagingErrorCode().name().toLowerCase(Locale.ROOT);
      if (code.contains("unregistered") || code.contains("invalid")) {
        invalidTokens.add(token);
      }
    }

    if (invalidTokens.isEmpty()) {
      return;
    }

    List<UserFcmToken> entities =
        invalidTokens.stream()
            .map(userFcmTokenRepository::findByToken)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    entities.forEach(
        tokenEntity -> {
          tokenEntity.setIsActive(false);
          tokenEntity.setLastSeenAt(Instant.now());
        });

    userFcmTokenRepository.saveAll(entities);
  }

  private FirebaseMessaging getFirebaseMessaging() {
    if (firebaseMessaging != null) {
      return firebaseMessaging;
    }

    synchronized (this) {
      if (firebaseMessaging != null) {
        return firebaseMessaging;
      }

      if (!firebaseProperties.isEnabled()) {
        log.info("Firebase push is disabled by configuration");
        return null;
      }

      try {
        FirebaseApp app;
        if (FirebaseApp.getApps().isEmpty()) {
          GoogleCredentials credentials = loadCredentials();
          if (credentials == null) {
            return null;
          }

          FirebaseOptions.Builder options =
              FirebaseOptions.builder().setCredentials(credentials);

          if (StringUtils.hasText(firebaseProperties.getProjectId())) {
            options.setProjectId(firebaseProperties.getProjectId());
          }

          app = FirebaseApp.initializeApp(options.build());
        } else {
          app = FirebaseApp.getInstance();
        }

        firebaseMessaging = FirebaseMessaging.getInstance(app);
      } catch (Exception e) {
        log.error("Unable to initialize Firebase messaging", e);
        firebaseMessaging = null;
      }

      return firebaseMessaging;
    }
  }

  private GoogleCredentials loadCredentials() {
    try {
      if (StringUtils.hasText(firebaseProperties.getServiceAccountJson())) {
        try (InputStream stream =
            new ByteArrayInputStream(
                firebaseProperties.getServiceAccountJson().getBytes(StandardCharsets.UTF_8))) {
          return GoogleCredentials.fromStream(stream);
        }
      }

      if (StringUtils.hasText(firebaseProperties.getServiceAccountPath())) {
        File file = new File(firebaseProperties.getServiceAccountPath());
        
        if (!file.exists()) {
          log.warn("Firebase service account file does not exist at path: {}", file.getAbsolutePath());
          return null;
        }
        
        if (file.isDirectory()) {
          log.error("Firebase service account path is a directory, not a file: {}. " +
              "Please check your Docker volume mounts or secret configuration.", file.getAbsolutePath());
          return null;
        }

        try (InputStream stream = new FileInputStream(file)) {
          return GoogleCredentials.fromStream(stream);
        }
      }

      log.warn("Firebase is enabled but no service account credential was provided");
      return null;
    } catch (Exception e) {
      log.error("Failed to load Firebase service account", e);
      return null;
    }
  }

  private String trimToLength(String value, int maxLen) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLen ? value : value.substring(0, maxLen);
  }
}
