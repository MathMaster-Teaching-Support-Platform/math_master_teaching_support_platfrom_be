package com.fptu.math_master.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest implements Serializable {
    private String id;
    private String type; // e.g., "SYSTEM", "MESSAGE", "ALERT"
    private String title;
    private String content;
    private String recipientId; // User ID or "ALL"
    private String senderId;
    private String actionUrl; // URL to navigate to when notification is clicked
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
}
