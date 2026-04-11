package com.fptu.math_master.controller;

import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.configuration.properties.CentrifugoProperties;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.response.NotificationResponse;
import com.fptu.math_master.service.CentrifugoService;
import com.fptu.math_master.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final StreamPublisher streamPublisher;
    private final CentrifugoService centrifugoService;
    private final NotificationService notificationService;
    private final CentrifugoProperties centrifugoProperties;

    @GetMapping("/token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getConnectionToken(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        int ttlHours = centrifugoProperties.getTokenTtlHours() != null ? centrifugoProperties.getTokenTtlHours() : 1;
        String token = centrifugoService.generateConnectionToken(userId, ttlHours);
        return ResponseEntity.ok(Map.of("token", token));
    }

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

    @PostMapping("/send")
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
    public ResponseEntity<String> sendSystemNotification() {
        NotificationRequest message = NotificationRequest.builder()
                .id(UUID.randomUUID().toString())
                .type("SYSTEM")
                .title("System Alert")
                .content("This is a test system notification triggered from Backend.")
                .recipientId("ALL")
                .senderId("SYSTEM")
                .timestamp(LocalDateTime.now())
                .build();

        streamPublisher.publish(message);
        return ResponseEntity.ok("System notification published to Redis Stream");
    }
}
