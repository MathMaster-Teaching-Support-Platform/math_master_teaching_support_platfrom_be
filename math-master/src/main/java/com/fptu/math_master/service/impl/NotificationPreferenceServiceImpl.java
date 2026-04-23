package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.NotificationPreferenceRequest;
import com.fptu.math_master.dto.response.NotificationPreferenceResponse;
import com.fptu.math_master.entity.NotificationPreference;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.repository.NotificationPreferenceRepository;
import com.fptu.math_master.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    // Default notification types
    private static final List<String> DEFAULT_NOTIFICATION_TYPES = Arrays.asList(
        "COURSE", "PROFILE_VERIFICATION", "SYSTEM", "ASSIGNMENT", "GRADE", "MESSAGE"
    );

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getMyPreferences(UUID userId) {
        List<NotificationPreference> preferences = notificationPreferenceRepository.findAllByUser_Id(userId);
        
        // If no preferences exist, create defaults
        if (preferences.isEmpty()) {
            preferences = createDefaultPreferences(userId);
        }
        
        return preferences.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse updatePreference(UUID userId, NotificationPreferenceRequest request) {
        Optional<NotificationPreference> existingOpt = notificationPreferenceRepository
            .findByUser_IdAndNotificationType(userId, request.getNotificationType());
        
        NotificationPreference preference;
        if (existingOpt.isPresent()) {
            preference = existingOpt.get();
            preference.setEmailEnabled(request.isEmailEnabled());
            preference.setPushEnabled(request.isPushEnabled());
            preference.setInAppEnabled(request.isInAppEnabled());
        } else {
            User user = new User();
            user.setId(userId);
            
            preference = NotificationPreference.builder()
                .user(user)
                .notificationType(request.getNotificationType())
                .emailEnabled(request.isEmailEnabled())
                .pushEnabled(request.isPushEnabled())
                .inAppEnabled(request.isInAppEnabled())
                .build();
        }
        
        preference = notificationPreferenceRepository.save(preference);
        return mapToResponse(preference);
    }

    @Override
    @Transactional
    public void resetToDefaults(UUID userId) {
        // Delete existing preferences
        List<NotificationPreference> existing = notificationPreferenceRepository.findAllByUser_Id(userId);
        notificationPreferenceRepository.deleteAll(existing);
        
        // Create default preferences
        createDefaultPreferences(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendPushNotification(UUID userId, String notificationType) {
        Optional<NotificationPreference> preference = notificationPreferenceRepository
            .findByUserIdAndTypeWithPushEnabled(userId, notificationType);
        
        // If no preference exists, default to enabled
        return preference.map(NotificationPreference::isPushEnabled).orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendInAppNotification(UUID userId, String notificationType) {
        Optional<NotificationPreference> preference = notificationPreferenceRepository
            .findByUserIdAndTypeWithInAppEnabled(userId, notificationType);
        
        // If no preference exists, default to enabled
        return preference.map(NotificationPreference::isInAppEnabled).orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendEmailNotification(UUID userId, String notificationType) {
        Optional<NotificationPreference> preference = notificationPreferenceRepository
            .findByUser_IdAndNotificationType(userId, notificationType);
        
        // If no preference exists, default to enabled
        return preference.map(NotificationPreference::isEmailEnabled).orElse(true);
    }

    private List<NotificationPreference> createDefaultPreferences(UUID userId) {
        User user = new User();
        user.setId(userId);
        
        List<NotificationPreference> defaults = DEFAULT_NOTIFICATION_TYPES.stream()
            .map(type -> NotificationPreference.builder()
                .user(user)
                .notificationType(type)
                .emailEnabled(true)
                .pushEnabled(true)
                .inAppEnabled(true)
                .build())
            .collect(Collectors.toList());
        
        return notificationPreferenceRepository.saveAll(defaults);
    }

    private NotificationPreferenceResponse mapToResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
            .id(preference.getId())
            .userId(preference.getUser().getId())
            .notificationType(preference.getNotificationType())
            .emailEnabled(preference.isEmailEnabled())
            .pushEnabled(preference.isPushEnabled())
            .inAppEnabled(preference.isInAppEnabled())
            .createdAt(preference.getCreatedAt())
            .updatedAt(preference.getUpdatedAt())
            .build();
    }
}