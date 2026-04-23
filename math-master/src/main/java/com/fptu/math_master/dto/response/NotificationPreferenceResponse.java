package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {
    
    private UUID id;
    private UUID userId;
    private String notificationType;
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean inAppEnabled;
    private Instant createdAt;
    private Instant updatedAt;
}