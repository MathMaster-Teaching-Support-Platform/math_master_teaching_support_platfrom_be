package com.fptu.math_master.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.request.FcmTokenRequest;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.request.NotificationPreferenceRequest;
import com.fptu.math_master.dto.response.NotificationResponse;
import com.fptu.math_master.dto.response.NotificationPreferenceResponse;
import com.fptu.math_master.service.NotificationService;
import com.fptu.math_master.service.NotificationPreferenceService;
import com.fptu.math_master.service.PushNotificationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final StreamPublisher streamPublisher;
    private final NotificationService notificationService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final PushNotificationService pushNotificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(notificationService.getNotifications(userId, pageable));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());

        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PostMapping("/push-token/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> registerPushToken(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FcmTokenRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        pushNotificationService.registerToken(userId, request.getToken(), request.getDeviceInfo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/push-token/unregister")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unregisterPushToken(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FcmTokenRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        pushNotificationService.unregisterToken(userId, request.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequest message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        streamPublisher.publish(message);
        return ResponseEntity.ok("Notification published to Redis Stream");
    }

    @PostMapping("/test-system")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> sendSystemNotification() {
        NotificationRequest message = NotificationRequest.builder()
                .id(UUID.randomUUID().toString())
                .type("SYSTEM")
                .title("System Alert")
                .content("This is a test system notification triggered from Backend.")
                .recipientId("ALL")
                .senderId("SYSTEM")
                .timestamp(LocalDateTime.now())
                .actionUrl("/notifications")
                .build();

        streamPublisher.publish(message);
        return ResponseEntity.ok("System notification published to Redis Stream");
    }

    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationPreferenceResponse>> getMyPreferences(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(notificationPreferenceService.getMyPreferences(userId));
    }

    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(notificationPreferenceService.updatePreference(userId, request));
    }

    @PostMapping("/preferences/reset")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> resetPreferencesToDefaults(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationPreferenceService.resetToDefaults(userId);
        return ResponseEntity.noContent().build();
    }
}

