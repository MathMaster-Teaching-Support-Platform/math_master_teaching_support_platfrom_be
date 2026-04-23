package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.NotificationPreferenceRequest;
import com.fptu.math_master.dto.response.NotificationPreferenceResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceService {
    
    List<NotificationPreferenceResponse> getMyPreferences(UUID userId);
    
    NotificationPreferenceResponse updatePreference(UUID userId, NotificationPreferenceRequest request);
    
    void resetToDefaults(UUID userId);
    
    boolean shouldSendPushNotification(UUID userId, String notificationType);
    
    boolean shouldSendInAppNotification(UUID userId, String notificationType);
    
    boolean shouldSendEmailNotification(UUID userId, String notificationType);
}