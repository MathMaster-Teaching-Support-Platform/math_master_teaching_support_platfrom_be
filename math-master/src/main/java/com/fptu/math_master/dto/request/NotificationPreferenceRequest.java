package com.fptu.math_master.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {
    
    @NotBlank(message = "Notification type is required")
    private String notificationType;
    
    private boolean emailEnabled = true;
    private boolean pushEnabled = true;
    private boolean inAppEnabled = true;
}