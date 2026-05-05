package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.ProfileReviewRequest;
import com.fptu.math_master.dto.request.TeacherProfileRequest;
import com.fptu.math_master.dto.response.TeacherProfileResponse;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.ProfileStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.EmailService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.service.async.OcrJobProducer;
import com.fptu.math_master.component.StreamPublisher;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("TeacherProfileServiceImpl - Tests")
class TeacherProfileServiceImplTest extends BaseUnitTest {

  @InjectMocks private TeacherProfileServiceImpl teacherProfileService;

  @Mock private TeacherProfileRepository teacherProfileRepository;
  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private UploadService uploadService;
  @Mock private MinioProperties minioProperties;
  @Mock private EmailService emailService;
  @Mock private StreamPublisher streamPublisher;
  @Mock private OcrJobProducer ocrJobProducer;

  private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID ADMIN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID PROFILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private User teacherUser;
  private TeacherProfile profile;
  private TeacherProfileRequest profileRequest;

  @BeforeEach
  void setUp() {
    teacherUser = new User();
    teacherUser.setId(USER_ID);
    teacherUser.setUserName("teacher.ngoc");
    teacherUser.setFullName("Nguyen Ngoc Anh");
    teacherUser.setEmail("nguyen.ngoc.anh@fptu.edu.vn");

    profile = TeacherProfile.builder()
        .user(teacherUser)
        .fullName("Nguyen Ngoc Anh")
        .schoolName("THPT Le Quy Don")
        .schoolAddress("Da Nang")
        .schoolWebsite("https://lequydon.edu.vn")
        .position("Giao vien Toan")
        .verificationDocumentKey("verification/profile.zip")
        .verificationDocumentPath("verification/profile.zip")
        .description("10 nam kinh nghiem boi duong hoc sinh gioi")
        .status(ProfileStatus.PENDING)
        .ocrVerified(false)
        .build();
    profile.setId(PROFILE_ID);
    profile.setCreatedAt(Instant.parse("2026-04-20T00:00:00Z"));
    profile.setUpdatedAt(Instant.parse("2026-04-21T00:00:00Z"));

    profileRequest = TeacherProfileRequest.builder()
        .fullName("Nguyen Ngoc Anh")
        .schoolName("THPT Le Quy Don")
        .schoolAddress("Da Nang")
        .schoolWebsite("https://lequydon.edu.vn")
        .position("Giao vien Toan")
        .description("Cap nhat ho so")
        .websiteUrl("https://portfolio.teacher.vn")
        .linkedinUrl("https://linkedin.com/in/ngocanh")
        .youtubeUrl("https://youtube.com/@thayngocanh")
        .facebookUrl("https://facebook.com/thayngocanh")
        .build();

    when(minioProperties.getVerificationBucket()).thenReturn("teacher-verifications");
  }

  @Nested
  @DisplayName("submitProfile()")
  class SubmitProfileTests {
    @Test
    void it_should_throw_exception_when_user_not_found_on_submit() {
      // ===== ARRANGE =====
      when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> teacherProfileService.submitProfile(profileRequest, List.of(), USER_ID));
      assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(USER_ID);
      verify(teacherProfileRepository, never()).save(any(TeacherProfile.class));
    }

    @Test
    void it_should_throw_exception_when_profile_already_exists_on_submit() {
      // ===== ARRANGE =====
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(teacherUser));
      when(teacherProfileRepository.existsByUserId(USER_ID)).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> teacherProfileService.submitProfile(profileRequest, List.of(), USER_ID));
      assertEquals(ErrorCode.PROFILE_ALREADY_EXISTS, ex.getErrorCode());

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).existsByUserId(USER_ID);
      verify(uploadService, never()).uploadFilesAsZip(any(), any(), any());
    }

    @Test
    void it_should_submit_profile_successfully_even_when_ocr_job_creation_fails() {
      // ===== ARRANGE =====
      MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(teacherUser));
      when(teacherProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
      when(uploadService.uploadFilesAsZip(any(), eq("verification"), eq("verification_docs")))
          .thenReturn("verification/profile.zip");
      when(teacherProfileRepository.save(any(TeacherProfile.class))).thenReturn(profile);
      when(userRepository.findUserIdsByRoleName(PredefinedRole.ADMIN_ROLE))
          .thenReturn(List.of(ADMIN_ID));
      when(ocrJobProducer.createOcrJob(PROFILE_ID, USER_ID))
          .thenThrow(new RuntimeException("queue unavailable"));

      // ===== ACT =====
      TeacherProfileResponse response =
          teacherProfileService.submitProfile(profileRequest, List.of(file), USER_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(PROFILE_ID, response.getId()),
          () -> assertEquals(ProfileStatus.PENDING, response.getStatus()));

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).save(any(TeacherProfile.class));
      verify(ocrJobProducer, times(1)).createOcrJob(PROFILE_ID, USER_ID);
    }

    @Test
    void it_should_submit_profile_and_trigger_ocr_job_successfully() {
      // ===== ARRANGE =====
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(teacherUser));
      when(teacherProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
      when(uploadService.uploadFilesAsZip(any(), eq("verification"), eq("verification_docs")))
          .thenReturn("verification/profile.zip");
      when(teacherProfileRepository.save(any(TeacherProfile.class))).thenReturn(profile);
      when(userRepository.findUserIdsByRoleName(PredefinedRole.ADMIN_ROLE))
          .thenReturn(List.of(ADMIN_ID));
      when(ocrJobProducer.createOcrJob(PROFILE_ID, USER_ID)).thenReturn("job-02");

      // ===== ACT =====
      TeacherProfileResponse response =
          teacherProfileService.submitProfile(profileRequest, List.of(), USER_ID);

      // ===== ASSERT =====
      assertEquals(PROFILE_ID, response.getId());
      verify(ocrJobProducer, times(1)).createOcrJob(PROFILE_ID, USER_ID);
    }

    @Test
    void it_should_notify_all_admins_when_teacher_profile_is_submitted() {
      // ===== ARRANGE =====
      UUID adminId2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(teacherUser));
      when(teacherProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
      when(uploadService.uploadFilesAsZip(any(), eq("verification"), eq("verification_docs")))
          .thenReturn("verification/profile.zip");
      when(teacherProfileRepository.save(any(TeacherProfile.class))).thenReturn(profile);
      when(userRepository.findUserIdsByRoleName(PredefinedRole.ADMIN_ROLE))
          .thenReturn(List.of(ADMIN_ID, adminId2));
      when(ocrJobProducer.createOcrJob(any(UUID.class), any(UUID.class))).thenReturn("job-03");

      // ===== ACT =====
      teacherProfileService.submitProfile(profileRequest, List.of(), USER_ID);

      // ===== VERIFY =====
      verify(streamPublisher, times(2)).publish(any());
    }

    @Test
    void it_should_not_fail_submission_when_no_admin_exists() {
      // ===== ARRANGE =====
      when(userRepository.findById(USER_ID)).thenReturn(Optional.of(teacherUser));
      when(teacherProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
      when(uploadService.uploadFilesAsZip(any(), eq("verification"), eq("verification_docs")))
          .thenReturn("verification/profile.zip");
      when(teacherProfileRepository.save(any(TeacherProfile.class))).thenReturn(profile);
      when(userRepository.findUserIdsByRoleName(PredefinedRole.ADMIN_ROLE))
          .thenReturn(List.of());
      when(ocrJobProducer.createOcrJob(any(UUID.class), any(UUID.class))).thenReturn("job-04");

      // ===== ACT =====
      TeacherProfileResponse response =
          teacherProfileService.submitProfile(profileRequest, List.of(), USER_ID);

      // ===== ASSERT =====
      assertNotNull(response);

      // ===== VERIFY =====
      verify(streamPublisher, never()).publish(any());
    }
  }

  @Nested
  @DisplayName("updateProfile()")
  class UpdateProfileTests {
    @Test
    void it_should_throw_exception_when_profile_not_found_on_update() {
      // ===== ARRANGE =====
      when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> teacherProfileService.updateProfile(profileRequest, List.of(), USER_ID));
      assertEquals(ErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_profile_already_approved_on_update() {
      // ===== ARRANGE =====
      profile.setStatus(ProfileStatus.APPROVED);
      when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> teacherProfileService.updateProfile(profileRequest, List.of(), USER_ID));
      assertEquals(ErrorCode.PROFILE_CANNOT_BE_MODIFIED, ex.getErrorCode());
    }

    @Test
    void it_should_reset_rejected_profile_to_pending_and_trigger_ocr_job() {
      // ===== ARRANGE =====
      profile.setStatus(ProfileStatus.REJECTED);
      profile.setAdminComment("Thong tin chua ro rang");
      profile.setReviewedBy(ADMIN_ID);
      profile.setReviewedAt(LocalDateTime.now().minusDays(1));
      profile.setOcrVerified(true);
      when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
      when(uploadService.uploadFilesAsZip(any(), eq("verification"), eq("verification_docs")))
          .thenReturn("verification/new-profile.zip");
      when(teacherProfileRepository.save(any(TeacherProfile.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(ocrJobProducer.createOcrJob(PROFILE_ID, USER_ID)).thenReturn("ocr-job-01");

      // ===== ACT =====
      TeacherProfileResponse response =
          teacherProfileService.updateProfile(
              profileRequest, List.of(org.mockito.Mockito.mock(MultipartFile.class)), USER_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(ProfileStatus.PENDING, response.getStatus()),
          () -> assertEquals("verification/new-profile.zip", response.getVerificationDocumentKey()),
          () -> assertEquals("Cap nhat ho so", response.getDescription()));
      assertFalse(profile.getOcrVerified());
      org.junit.jupiter.api.Assertions.assertNull(profile.getAdminComment());
      org.junit.jupiter.api.Assertions.assertNull(profile.getReviewedBy());
      org.junit.jupiter.api.Assertions.assertNull(profile.getReviewedAt());

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).save(profile);
      verify(ocrJobProducer, times(1)).createOcrJob(PROFILE_ID, USER_ID);
    }

    @Test
    void it_should_update_pending_profile_without_triggering_ocr_when_already_verified() {
      // ===== ARRANGE =====
      profile.setStatus(ProfileStatus.PENDING);
      profile.setOcrVerified(true);
      when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
      when(teacherProfileRepository.save(any(TeacherProfile.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      TeacherProfileResponse response =
          teacherProfileService.updateProfile(profileRequest, List.of(), USER_ID);

      // ===== ASSERT =====
      assertEquals(ProfileStatus.PENDING, response.getStatus());

      // ===== VERIFY =====
      verify(uploadService, never()).uploadFilesAsZip(any(), any(), any());
      verify(ocrJobProducer, never()).createOcrJob(any(UUID.class), any(UUID.class));
    }
  }

  @Nested
  @DisplayName("reviewProfile()")
  class ReviewProfileTests {
    @Test
    void it_should_throw_exception_when_profile_is_not_pending() {
      // ===== ARRANGE =====
      profile.setStatus(ProfileStatus.APPROVED);
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  teacherProfileService.reviewProfile(
                      PROFILE_ID,
                      ProfileReviewRequest.builder().status(ProfileStatus.REJECTED).build(),
                      ADMIN_ID));
      assertEquals(ErrorCode.INVALID_PROFILE_STATUS, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_review_status_is_invalid() {
      // ===== ARRANGE =====
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  teacherProfileService.reviewProfile(
                      PROFILE_ID,
                      ProfileReviewRequest.builder().status(ProfileStatus.PENDING).build(),
                      ADMIN_ID));
      assertEquals(ErrorCode.INVALID_PROFILE_STATUS, ex.getErrorCode());
    }

    @Test
    void it_should_approve_profile_and_upgrade_role_and_send_notifications() {
      // ===== ARRANGE =====
      Role studentRole = new Role();
      studentRole.setName(PredefinedRole.STUDENT_ROLE);
      Role teacherRole = new Role();
      teacherRole.setName(PredefinedRole.TEACHER_ROLE);
      teacherUser.setRoles(new HashSet<>(Set.of(studentRole)));

      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
      when(roleRepository.findByName(PredefinedRole.TEACHER_ROLE)).thenReturn(Optional.of(teacherRole));
      when(teacherProfileRepository.save(any(TeacherProfile.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.save(teacherUser)).thenReturn(teacherUser);

      ProfileReviewRequest request =
          ProfileReviewRequest.builder().status(ProfileStatus.APPROVED).adminComment("Dat yeu cau").build();

      // ===== ACT =====
      TeacherProfileResponse response = teacherProfileService.reviewProfile(PROFILE_ID, request, ADMIN_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(ProfileStatus.APPROVED, response.getStatus()),
          () -> assertEquals(ADMIN_ID, response.getReviewedBy()),
          () -> assertNotNull(response.getReviewedAt()));
      assertTrue(teacherUser.getRoles().stream().anyMatch(role -> PredefinedRole.TEACHER_ROLE.equals(role.getName())));
      assertFalse(teacherUser.getRoles().stream().anyMatch(role -> PredefinedRole.STUDENT_ROLE.equals(role.getName())));

      // ===== VERIFY =====
      verify(userRepository, times(1)).save(teacherUser);
      verify(emailService, times(1)).sendTeacherApprovalEmail(teacherUser.getEmail(), teacherUser.getFullName());
      verify(streamPublisher, times(1)).publish(any());
    }

    @Test
    void it_should_reject_profile_and_send_rejection_email_without_role_update() {
      // ===== ARRANGE =====
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
      when(teacherProfileRepository.save(any(TeacherProfile.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      ProfileReviewRequest request =
          ProfileReviewRequest.builder()
              .status(ProfileStatus.REJECTED)
              .adminComment("Can bo sung giay to cong tac")
              .build();

      // ===== ACT =====
      TeacherProfileResponse response = teacherProfileService.reviewProfile(PROFILE_ID, request, ADMIN_ID);

      // ===== ASSERT =====
      assertEquals(ProfileStatus.REJECTED, response.getStatus());

      // ===== VERIFY =====
      verify(userRepository, never()).save(any(User.class));
      verify(emailService, times(1))
          .sendTeacherRejectionEmail(
              teacherUser.getEmail(), teacherUser.getFullName(), "Can bo sung giay to cong tac");
      verify(streamPublisher, times(1)).publish(any());
    }

    @Test
    void it_should_throw_exception_when_teacher_role_not_found_during_approval() {
      // ===== ARRANGE =====
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
      when(roleRepository.findByName(PredefinedRole.TEACHER_ROLE)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  teacherProfileService.reviewProfile(
                      PROFILE_ID,
                      ProfileReviewRequest.builder().status(ProfileStatus.APPROVED).build(),
                      ADMIN_ID));
      assertEquals(ErrorCode.ROLE_NOT_EXISTED, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("profile read and delete methods")
  class ReadAndDeleteTests {
    @Test
    void it_should_return_my_profile_when_user_has_profile() {
      // ===== ARRANGE =====
      when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

      // ===== ACT =====
      TeacherProfileResponse response = teacherProfileService.getMyProfile(USER_ID);

      // ===== ASSERT =====
      assertEquals(PROFILE_ID, response.getId());
    }

    @Test
    void it_should_throw_exception_when_profile_id_not_found() {
      // ===== ARRANGE =====
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> teacherProfileService.getProfileById(PROFILE_ID));
      assertEquals(ErrorCode.PROFILE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_return_profiles_by_status_page() {
      // ===== ARRANGE =====
      Page<TeacherProfile> page = new PageImpl<>(List.of(profile), PageRequest.of(0, 10), 1);
      when(teacherProfileRepository.findByStatusOrderByCreatedAtDesc(ProfileStatus.PENDING, PageRequest.of(0, 10)))
          .thenReturn(page);

      // ===== ACT =====
      Page<TeacherProfileResponse> responsePage =
          teacherProfileService.getProfilesByStatus(ProfileStatus.PENDING, PageRequest.of(0, 10));

      // ===== ASSERT =====
      assertEquals(1, responsePage.getTotalElements());
    }

    @Test
    void it_should_throw_exception_when_deleting_approved_profile() {
      // ===== ARRANGE =====
      profile.setStatus(ProfileStatus.APPROVED);
      when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> teacherProfileService.deleteMyProfile(USER_ID));
      assertEquals(ErrorCode.PROFILE_CANNOT_BE_MODIFIED, ex.getErrorCode());
    }

    @Test
    void it_should_delete_profile_when_status_is_pending() {
      // ===== ARRANGE =====
      profile.setStatus(ProfileStatus.PENDING);
      when(teacherProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

      // ===== ACT =====
      teacherProfileService.deleteMyProfile(USER_ID);

      // ===== VERIFY =====
      verify(teacherProfileRepository, times(1)).delete(profile);
    }
  }

  @Nested
  @DisplayName("document download methods")
  class DocumentTests {
    @Test
    void it_should_return_presigned_download_url_when_first_normalized_key_exists() {
      // ===== ARRANGE =====
      TeacherProfile docProfile = TeacherProfile.builder()
          .user(teacherUser)
          .verificationDocumentKey("https://minio.local/teacher-verifications/verification/profile.zip")
          .verificationDocumentPath("teacher-verifications/verification/profile.zip")
          .build();
      docProfile.setId(PROFILE_ID);
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(docProfile));
      when(uploadService.downloadFile("verification/profile.zip", "teacher-verifications"))
          .thenReturn(new byte[] {1, 2});
      when(uploadService.getPresignedUrl("verification/profile.zip", "teacher-verifications"))
          .thenReturn("https://signed-url");

      // ===== ACT =====
      String url = teacherProfileService.getDownloadUrl(PROFILE_ID);

      // ===== ASSERT =====
      assertEquals("https://signed-url", url);

      // ===== VERIFY =====
      verify(uploadService, times(1)).getPresignedUrl("verification/profile.zip", "teacher-verifications");
    }

    @Test
    void it_should_try_next_candidate_key_when_first_key_not_found() {
      // ===== ARRANGE =====
      TeacherProfile docProfile = TeacherProfile.builder()
          .user(teacherUser)
          .verificationDocumentKey("missing.zip")
          .verificationDocumentPath("backup.zip")
          .build();
      docProfile.setId(PROFILE_ID);
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(docProfile));
      when(uploadService.downloadFile("missing.zip", "teacher-verifications"))
          .thenThrow(new AppException(ErrorCode.DOCUMENT_NOT_FOUND));
      when(uploadService.downloadFile("backup.zip", "teacher-verifications"))
          .thenReturn(new byte[] {7, 8, 9});

      // ===== ACT =====
      byte[] bytes = teacherProfileService.downloadVerificationDocument(PROFILE_ID);

      // ===== ASSERT =====
      assertArrayEquals(new byte[] {7, 8, 9}, bytes);

      // ===== VERIFY =====
      verify(uploadService, times(1)).downloadFile("missing.zip", "teacher-verifications");
      verify(uploadService, times(3)).downloadFile(any(), eq("teacher-verifications"));
    }

    @Test
    void it_should_throw_document_not_found_when_all_candidate_keys_fail() {
      // ===== ARRANGE =====
      TeacherProfile docProfile = TeacherProfile.builder()
          .user(teacherUser)
          .verificationDocumentKey("missing-1.zip")
          .verificationDocumentPath("missing-2.zip")
          .build();
      docProfile.setId(PROFILE_ID);
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(docProfile));
      when(uploadService.downloadFile(any(), eq("teacher-verifications")))
          .thenThrow(new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class, () -> teacherProfileService.downloadVerificationDocument(PROFILE_ID));
      assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_document_not_found_when_all_keys_are_blank() {
      // ===== ARRANGE =====
      TeacherProfile docProfile = TeacherProfile.builder().user(teacherUser).verificationDocumentKey(" ").verificationDocumentPath(" ").build();
      docProfile.setId(PROFILE_ID);
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(docProfile));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> teacherProfileService.getDownloadUrl(PROFILE_ID));
      assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_rethrow_non_document_not_found_exception_when_resolving_existing_key() {
      // ===== ARRANGE =====
      TeacherProfile docProfile =
          TeacherProfile.builder()
              .user(teacherUser)
              .verificationDocumentKey("verification/profile.zip")
              .verificationDocumentPath("backup.zip")
              .build();
      docProfile.setId(PROFILE_ID);
      when(teacherProfileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(docProfile));
      when(uploadService.downloadFile("verification/profile.zip", "teacher-verifications"))
          .thenThrow(new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class, () -> teacherProfileService.downloadVerificationDocument(PROFILE_ID));
      assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION, ex.getErrorCode());
      verify(uploadService, never()).downloadFile("backup.zip", "teacher-verifications");
    }
  }

  @Test
  void it_should_return_pending_profiles_count() {
    // ===== ARRANGE =====
    when(teacherProfileRepository.countPendingProfiles()).thenReturn(4L);

    // ===== ACT =====
    long count = teacherProfileService.countPendingProfiles();

    // ===== ASSERT =====
    assertEquals(4L, count);

    // ===== VERIFY =====
    verify(teacherProfileRepository, times(1)).countPendingProfiles();
    verifyNoMoreInteractions(teacherProfileRepository);
  }
}
