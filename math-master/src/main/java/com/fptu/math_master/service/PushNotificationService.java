package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.NotificationRequest;
import java.util.UUID;

public interface PushNotificationService {

  void registerToken(UUID userId, String token, String deviceInfo);

  void unregisterToken(UUID userId, String token);

  void sendNotification(NotificationRequest notificationRequest);
}
