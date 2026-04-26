package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.FirebaseProperties;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.UserFcmToken;
import com.fptu.math_master.repository.UserFcmTokenRepository;
import com.fptu.math_master.service.NotificationPreferenceService;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("FcmPushNotificationServiceImpl - Tests")
class FcmPushNotificationServiceImplTest extends BaseUnitTest {

  @InjectMocks private FcmPushNotificationServiceImpl fcmPushNotificationService;

  @Mock private UserFcmTokenRepository userFcmTokenRepository;
  @Mock private FirebaseProperties firebaseProperties;
  @Mock private NotificationPreferenceService notificationPreferenceService;
  @Mock private FirebaseMessaging firebaseMessaging;
  @Mock private BatchResponse batchResponse;
  @Mock private SendResponse sendResponseSuccess;
  @Mock private SendResponse sendResponseInvalid;

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private UserFcmToken activeToken;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(fcmPushNotificationService, "firebaseMessaging", firebaseMessaging);
    activeToken = buildToken(USER_ID, "fcm-token-001", true, "Chrome on Windows");
  }

  private UserFcmToken buildToken(UUID userId, String tokenValue, boolean isActive, String deviceInfo) {
    User user = new User();
    user.setId(userId);

    UserFcmToken token = new UserFcmToken();
    token.setUser(user);
    token.setToken(tokenValue);
    token.setIsActive(isActive);
    token.setDeviceInfo(deviceInfo);
    return token;
  }

  private NotificationRequest buildNotification(String recipientId) {
    return NotificationRequest.builder()
        .id("notif-001")
        .type("COURSE")
        .title("Thong bao moi")
        .content("Ban co cap nhat khoa hoc moi")
        .recipientId(recipientId)
        .senderId("SYSTEM")
        .actionUrl("/student/notifications")
        .timestamp(LocalDateTime.of(2026, 4, 26, 9, 0))
        .metadata(Map.of("courseId", "abc-course", "event", "COURSE_ENROLLED"))
        .build();
  }

  private NotificationRequest buildSilentNotification(String recipientId) {
    return NotificationRequest.builder()
        .id("notif-silent-001")
        .type("SYSTEM")
        .title(" ")
        .content(" ")
        .recipientId(recipientId)
        .senderId("SYSTEM")
        .metadata(null)
        .timestamp(null)
        .build();
  }

  @Nested
  @DisplayName("registerToken()")
  class RegisterTokenTests {

    @Test
    void it_should_ignore_register_when_token_is_blank() {
      // ===== ARRANGE =====
      String blankToken = "   ";

      // ===== ACT =====
      fcmPushNotificationService.registerToken(USER_ID, blankToken, "Android 15");

      // ===== ASSERT & VERIFY =====
      verifyNoInteractions(userFcmTokenRepository);
    }

    @Test
    void it_should_update_existing_token_when_token_exists() {
      // ===== ARRANGE =====
      String longDeviceInfo = "Pixel 9 Pro ".repeat(40);
      when(userFcmTokenRepository.findByToken("fcm-token-002")).thenReturn(Optional.of(activeToken));

      // ===== ACT =====
      fcmPushNotificationService.registerToken(USER_ID, "  fcm-token-002  ", longDeviceInfo);

      // ===== ASSERT =====
      assertEquals("fcm-token-002", activeToken.getToken());
      assertTrue(activeToken.getIsActive());
      assertNotNull(activeToken.getLastSeenAt());
      assertEquals(255, activeToken.getDeviceInfo().length());

      // ===== VERIFY =====
      verify(userFcmTokenRepository, times(1)).findByToken("fcm-token-002");
      verify(userFcmTokenRepository, times(1)).save(activeToken);
    }

    @Test
    void it_should_create_new_token_when_token_does_not_exist() {
      // ===== ARRANGE =====
      when(userFcmTokenRepository.findByToken("fcm-token-new")).thenReturn(Optional.empty());

      // ===== ACT =====
      fcmPushNotificationService.registerToken(USER_ID, "fcm-token-new", null);

      // ===== VERIFY =====
      verify(userFcmTokenRepository, times(1)).findByToken("fcm-token-new");
      verify(userFcmTokenRepository, times(1)).save(any(UserFcmToken.class));
    }
  }

  @Nested
  @DisplayName("unregisterToken()")
  class UnregisterTokenTests {

    @Test
    void it_should_ignore_unregister_when_token_is_blank() {
      // ===== ARRANGE =====
      String blankToken = "";

      // ===== ACT =====
      fcmPushNotificationService.unregisterToken(USER_ID, blankToken);

      // ===== ASSERT & VERIFY =====
      verifyNoInteractions(userFcmTokenRepository);
    }

    @Test
    void it_should_ignore_unregister_when_token_not_found() {
      // ===== ARRANGE =====
      when(userFcmTokenRepository.findByToken("fcm-token-not-found")).thenReturn(Optional.empty());

      // ===== ACT =====
      fcmPushNotificationService.unregisterToken(USER_ID, "fcm-token-not-found");

      // ===== VERIFY =====
      verify(userFcmTokenRepository, times(1)).findByToken("fcm-token-not-found");
      verify(userFcmTokenRepository, never()).save(any(UserFcmToken.class));
    }

    @Test
    void it_should_ignore_unregister_when_token_belongs_to_other_user() {
      // ===== ARRANGE =====
      UserFcmToken otherUserToken = buildToken(OTHER_USER_ID, "fcm-token-003", true, "Safari iOS");
      when(userFcmTokenRepository.findByToken("fcm-token-003")).thenReturn(Optional.of(otherUserToken));

      // ===== ACT =====
      fcmPushNotificationService.unregisterToken(USER_ID, "fcm-token-003");

      // ===== VERIFY =====
      verify(userFcmTokenRepository, times(1)).findByToken("fcm-token-003");
      verify(userFcmTokenRepository, never()).save(any(UserFcmToken.class));
    }

    @Test
    void it_should_deactivate_token_when_owner_unregisters() {
      // ===== ARRANGE =====
      when(userFcmTokenRepository.findByToken("fcm-token-004")).thenReturn(Optional.of(activeToken));

      // ===== ACT =====
      fcmPushNotificationService.unregisterToken(USER_ID, "fcm-token-004");

      // ===== ASSERT =====
      assertEquals(false, activeToken.getIsActive());
      assertNotNull(activeToken.getLastSeenAt());

      // ===== VERIFY =====
      verify(userFcmTokenRepository, times(1)).save(activeToken);
    }
  }

  @Nested
  @DisplayName("sendNotification()")
  class SendNotificationTests {

    @Test
    void it_should_skip_send_when_firebase_messaging_is_not_configured() {
      // ===== ARRANGE =====
      ReflectionTestUtils.setField(fcmPushNotificationService, "firebaseMessaging", null);
      when(firebaseProperties.isEnabled()).thenReturn(false);

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(buildNotification("ALL"));

      // ===== VERIFY =====
      verify(firebaseProperties, times(1)).isEnabled();
      verify(userFcmTokenRepository, never()).findAllByIsActiveTrue();
    }

    @Test
    void it_should_skip_send_when_firebase_enabled_but_credentials_are_missing() {
      // ===== ARRANGE =====
      ReflectionTestUtils.setField(fcmPushNotificationService, "firebaseMessaging", null);
      when(firebaseProperties.isEnabled()).thenReturn(true);
      when(firebaseProperties.getServiceAccountJson()).thenReturn(null);
      when(firebaseProperties.getServiceAccountPath()).thenReturn(null);

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(buildNotification("ALL"));

      // ===== VERIFY =====
      verify(firebaseProperties, times(1)).isEnabled();
      verify(firebaseProperties, times(1)).getServiceAccountJson();
      verify(firebaseProperties, times(1)).getServiceAccountPath();
      verify(userFcmTokenRepository, never()).findAllByIsActiveTrue();
    }

    @Test
    void it_should_use_existing_firebase_app_instance_when_available() {
      // ===== ARRANGE =====
      ReflectionTestUtils.setField(fcmPushNotificationService, "firebaseMessaging", null);
      when(firebaseProperties.isEnabled()).thenReturn(true);
      FirebaseApp app = Mockito.mock(FirebaseApp.class);

      try (MockedStatic<FirebaseApp> appMock = Mockito.mockStatic(FirebaseApp.class);
          MockedStatic<FirebaseMessaging> messagingMock = Mockito.mockStatic(FirebaseMessaging.class)) {
        appMock.when(FirebaseApp::getApps).thenReturn(List.of(app));
        appMock.when(FirebaseApp::getInstance).thenReturn(app);
        messagingMock.when(() -> FirebaseMessaging.getInstance(app)).thenReturn(firebaseMessaging);
        when(userFcmTokenRepository.findAllByUser_IdAndIsActiveTrue(USER_ID)).thenReturn(List.of());

        // ===== ACT =====
        fcmPushNotificationService.sendNotification(buildNotification(USER_ID.toString()));
      }

      // ===== VERIFY =====
      verify(userFcmTokenRepository, times(1)).findAllByUser_IdAndIsActiveTrue(USER_ID);
    }

    @Test
    void it_should_skip_send_when_recipient_id_is_invalid_uuid() throws Exception {
      // ===== ARRANGE =====
      NotificationRequest request = buildNotification("invalid-uuid");

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(request);

      // ===== VERIFY =====
      verify(userFcmTokenRepository, never()).findAllByUser_IdAndIsActiveTrue(any(UUID.class));
      verify(firebaseMessaging, never()).sendEachForMulticast(any());
    }

    @Test
    void it_should_skip_send_when_destination_tokens_are_empty() throws Exception {
      // ===== ARRANGE =====
      NotificationRequest request = buildNotification(USER_ID.toString());
      when(userFcmTokenRepository.findAllByUser_IdAndIsActiveTrue(USER_ID)).thenReturn(List.of());

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(request);

      // ===== VERIFY =====
      verify(userFcmTokenRepository, times(1)).findAllByUser_IdAndIsActiveTrue(USER_ID);
      verify(firebaseMessaging, never()).sendEachForMulticast(any());
    }

    @Test
    void it_should_send_even_when_preference_check_throws_exception() throws Exception {
      // ===== ARRANGE =====
      NotificationRequest request = buildNotification(USER_ID.toString());
      when(userFcmTokenRepository.findAllByUser_IdAndIsActiveTrue(USER_ID)).thenReturn(List.of(activeToken));
      when(notificationPreferenceService.shouldSendPushNotification(USER_ID, "COURSE"))
          .thenThrow(new RuntimeException("Preference service timeout"));
      when(batchResponse.getResponses()).thenReturn(List.of(sendResponseSuccess));
      when(sendResponseSuccess.isSuccessful()).thenReturn(true);
      when(firebaseMessaging.sendEachForMulticast(any())).thenReturn(batchResponse);

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(request);

      // ===== VERIFY =====
      verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
      verify(userFcmTokenRepository, never()).saveAll(any());
    }

    @Test
    void it_should_skip_send_when_all_users_disable_push_for_notification_type() throws Exception {
      // ===== ARRANGE =====
      NotificationRequest request = buildNotification(USER_ID.toString());
      when(userFcmTokenRepository.findAllByUser_IdAndIsActiveTrue(USER_ID)).thenReturn(List.of(activeToken));
      when(notificationPreferenceService.shouldSendPushNotification(USER_ID, "COURSE")).thenReturn(false);

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(request);

      // ===== VERIFY =====
      verify(firebaseMessaging, never()).sendEachForMulticast(any());
    }

    @Test
    void it_should_deactivate_invalid_tokens_after_multicast_send() throws Exception {
      // ===== ARRANGE =====
      NotificationRequest request = buildNotification("ALL");
      UserFcmToken invalidToken = buildToken(USER_ID, "fcm-token-invalid", true, "Firefox Linux");
      when(userFcmTokenRepository.findAllByIsActiveTrue()).thenReturn(List.of(activeToken, invalidToken));
      when(notificationPreferenceService.shouldSendPushNotification(USER_ID, "COURSE")).thenReturn(true);
      when(batchResponse.getResponses()).thenReturn(List.of(sendResponseSuccess, sendResponseInvalid));
      when(sendResponseSuccess.isSuccessful()).thenReturn(true);
      when(sendResponseInvalid.isSuccessful()).thenReturn(false);

      FirebaseMessagingException invalidException = org.mockito.Mockito.mock(FirebaseMessagingException.class);
      when(sendResponseInvalid.getException()).thenReturn(invalidException);
      when(invalidException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
      when(firebaseMessaging.sendEachForMulticast(any())).thenReturn(batchResponse);
      when(userFcmTokenRepository.findByToken("fcm-token-invalid")).thenReturn(Optional.of(invalidToken));

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(request);

      // ===== ASSERT =====
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<UserFcmToken>> captor = (ArgumentCaptor<List<UserFcmToken>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
      verify(userFcmTokenRepository, times(1)).saveAll(captor.capture());
      List<UserFcmToken> deactivated = captor.getValue();
      assertEquals(1, deactivated.size());
      assertEquals("fcm-token-invalid", deactivated.get(0).getToken());
      assertEquals(false, deactivated.get(0).getIsActive());
      assertNotNull(deactivated.get(0).getLastSeenAt());

      // ===== VERIFY =====
      verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
    }

    @Test
    void it_should_continue_when_fcm_multicast_send_throws_exception() throws Exception {
      // ===== ARRANGE =====
      NotificationRequest request = buildNotification(USER_ID.toString());
      when(userFcmTokenRepository.findAllByUser_IdAndIsActiveTrue(USER_ID)).thenReturn(List.of(activeToken));
      when(notificationPreferenceService.shouldSendPushNotification(USER_ID, "COURSE")).thenReturn(true);
      when(firebaseMessaging.sendEachForMulticast(any()))
          .thenThrow(Mockito.mock(FirebaseMessagingException.class));

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(request);

      // ===== VERIFY =====
      verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
      verify(userFcmTokenRepository, never()).saveAll(any());
    }

    @Test
    void it_should_send_in_multiple_batches_when_token_count_exceeds_limit() throws Exception {
      // ===== ARRANGE =====
      List<UserFcmToken> manyTokens = new ArrayList<>();
      for (int i = 0; i < 501; i++) {
        manyTokens.add(buildToken(USER_ID, "fcm-token-" + i, true, "Windows Desktop"));
      }
      when(userFcmTokenRepository.findAllByIsActiveTrue()).thenReturn(manyTokens);
      when(notificationPreferenceService.shouldSendPushNotification(USER_ID, "COURSE")).thenReturn(true);
      when(batchResponse.getResponses()).thenReturn(List.of());
      when(firebaseMessaging.sendEachForMulticast(any())).thenReturn(batchResponse);

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(buildNotification("ALL"));

      // ===== VERIFY =====
      verify(firebaseMessaging, times(2)).sendEachForMulticast(any());
    }

    @Test
    void it_should_send_data_only_message_when_title_and_content_are_blank() throws Exception {
      // ===== ARRANGE =====
      when(userFcmTokenRepository.findAllByUser_IdAndIsActiveTrue(USER_ID)).thenReturn(List.of(activeToken));
      when(notificationPreferenceService.shouldSendPushNotification(USER_ID, "SYSTEM")).thenReturn(true);
      when(batchResponse.getResponses()).thenReturn(List.of(sendResponseSuccess));
      when(sendResponseSuccess.isSuccessful()).thenReturn(true);
      when(firebaseMessaging.sendEachForMulticast(any())).thenReturn(batchResponse);

      // ===== ACT =====
      fcmPushNotificationService.sendNotification(buildSilentNotification(USER_ID.toString()));

      // ===== VERIFY =====
      verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
    }
  }

  @Nested
  @DisplayName("Private helpers")
  class PrivateHelperTests {

    @Test
    void it_should_return_null_when_trim_to_length_receives_null() {
      // ===== ARRANGE =====
      String value = null;

      // ===== ACT =====
      String result = ReflectionTestUtils.invokeMethod(fcmPushNotificationService, "trimToLength", value, 10);

      // ===== ASSERT =====
      assertNull(result);
    }

    @Test
    void it_should_keep_original_string_when_length_is_within_limit() {
      // ===== ARRANGE =====
      String value = "Chrome";

      // ===== ACT =====
      String result = ReflectionTestUtils.invokeMethod(fcmPushNotificationService, "trimToLength", value, 20);

      // ===== ASSERT =====
      assertEquals("Chrome", result);
    }

    @Test
    void it_should_not_deactivate_tokens_when_send_response_has_no_exception() {
      // ===== ARRANGE =====
      when(sendResponseInvalid.isSuccessful()).thenReturn(false);
      when(sendResponseInvalid.getException()).thenReturn(null);

      // ===== ACT =====
      ReflectionTestUtils.invokeMethod(
          fcmPushNotificationService,
          "deactivateInvalidTokens",
          List.of("fcm-token-007"),
          List.of(sendResponseInvalid));

      // ===== VERIFY =====
      verify(userFcmTokenRepository, never()).saveAll(any());
    }

    @Test
    void it_should_not_deactivate_tokens_when_error_code_is_missing() {
      // ===== ARRANGE =====
      FirebaseMessagingException exception = Mockito.mock(FirebaseMessagingException.class);
      when(sendResponseInvalid.isSuccessful()).thenReturn(false);
      when(sendResponseInvalid.getException()).thenReturn(exception);
      when(exception.getMessagingErrorCode()).thenReturn(null);

      // ===== ACT =====
      ReflectionTestUtils.invokeMethod(
          fcmPushNotificationService,
          "deactivateInvalidTokens",
          List.of("fcm-token-008"),
          List.of(sendResponseInvalid));

      // ===== VERIFY =====
      verify(userFcmTokenRepository, never()).saveAll(any());
    }

    @Test
    void it_should_return_null_credentials_when_service_account_path_is_invalid() {
      // ===== ARRANGE =====
      when(firebaseProperties.getServiceAccountJson()).thenReturn(" ");
      when(firebaseProperties.getServiceAccountPath()).thenReturn("Z:/not-found/firebase.json");

      // ===== ACT =====
      Object credentials = ReflectionTestUtils.invokeMethod(fcmPushNotificationService, "loadCredentials");

      // ===== ASSERT =====
      assertNull(credentials);
    }
  }
}
