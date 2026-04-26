package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.CreateRoadmapFeedbackRequest;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.RoadmapFeedback;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.RoadmapFeedbackRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("RoadmapFeedbackServiceImpl - Tests")
class RoadmapFeedbackServiceImplTest extends BaseUnitTest {

  @InjectMocks private RoadmapFeedbackServiceImpl roadmapFeedbackService;

  @Mock private RoadmapFeedbackRepository roadmapFeedbackRepository;
  @Mock private LearningRoadmapRepository learningRoadmapRepository;
  @Mock private UserRepository userRepository;

  private static final UUID ROADMAP_ID = UUID.fromString("92000000-0000-0000-0000-000000000001");
  private static final UUID STUDENT_ID = UUID.fromString("92000000-0000-0000-0000-000000000002");
  private static final UUID TEACHER_ID = UUID.fromString("92000000-0000-0000-0000-000000000003");
  private static final UUID FEEDBACK_ID = UUID.fromString("92000000-0000-0000-0000-000000000004");

  private LearningRoadmap buildRoadmap(UUID id, UUID studentId, UUID teacherId) {
    LearningRoadmap roadmap = new LearningRoadmap();
    roadmap.setId(id);
    roadmap.setStudentId(studentId);
    roadmap.setTeacherId(teacherId);
    roadmap.setDeletedAt(null);
    return roadmap;
  }

  private RoadmapFeedback buildFeedback(UUID id, UUID roadmapId, UUID studentId, int rating, String content) {
    RoadmapFeedback feedback = new RoadmapFeedback();
    feedback.setId(id);
    feedback.setRoadmapId(roadmapId);
    feedback.setStudentId(studentId);
    feedback.setRating(rating);
    feedback.setContent(content);
    feedback.setCreatedAt(Instant.parse("2026-04-26T06:30:00Z"));
    feedback.setUpdatedAt(Instant.parse("2026-04-26T06:30:00Z"));
    return feedback;
  }

  @Nested
  @DisplayName("submitFeedback()")
  class SubmitFeedbackTests {

    @Test
    void it_should_create_new_feedback_when_not_existing_and_student_role_valid() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, STUDENT_ID, TEACHER_ID);
      CreateRoadmapFeedbackRequest request =
          CreateRoadmapFeedbackRequest.builder().rating(5).content("Rat huu ich").build();
      RoadmapFeedback saved = buildFeedback(FEEDBACK_ID, ROADMAP_ID, STUDENT_ID, 5, "Rat huu ich");
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(roadmapFeedbackRepository.findByRoadmapIdAndStudentIdAndNotDeleted(ROADMAP_ID, STUDENT_ID))
          .thenReturn(Optional.empty());
      when(roadmapFeedbackRepository.save(any(RoadmapFeedback.class))).thenReturn(saved);
      User user = new User();
      user.setFullName("Nguyen Van B");
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(user));

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.STUDENT_ROLE)).thenReturn(true);

        // ===== ACT =====
        var response = roadmapFeedbackService.submitFeedback(ROADMAP_ID, STUDENT_ID, request);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(FEEDBACK_ID, response.getId()),
            () -> assertEquals("Nguyen Van B", response.getStudentName()),
            () -> assertEquals(5, response.getRating()));
      }
    }

    @Test
    void it_should_update_existing_feedback_when_existing_record_found() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, STUDENT_ID, TEACHER_ID);
      RoadmapFeedback existing = buildFeedback(FEEDBACK_ID, ROADMAP_ID, STUDENT_ID, 3, "Cu");
      CreateRoadmapFeedbackRequest request =
          CreateRoadmapFeedbackRequest.builder().rating(4).content("Moi").build();
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(roadmapFeedbackRepository.findByRoadmapIdAndStudentIdAndNotDeleted(ROADMAP_ID, STUDENT_ID))
          .thenReturn(Optional.of(existing));
      when(roadmapFeedbackRepository.save(existing)).thenReturn(existing);
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.STUDENT_ROLE)).thenReturn(true);

        // ===== ACT =====
        var response = roadmapFeedbackService.submitFeedback(ROADMAP_ID, STUDENT_ID, request);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(4, response.getRating()),
            () -> assertEquals("Moi", response.getContent()),
            () -> assertEquals("Unknown", response.getStudentName()));
      }
    }

    @Test
    void it_should_allow_admin_to_submit_feedback_without_student_role_check() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, UUID.fromString("92000000-0000-0000-0000-000000000099"), TEACHER_ID);
      CreateRoadmapFeedbackRequest request =
          CreateRoadmapFeedbackRequest.builder().rating(4).content("Admin ghi nhan").build();
      RoadmapFeedback saved = buildFeedback(FEEDBACK_ID, ROADMAP_ID, STUDENT_ID, 4, "Admin ghi nhan");
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(roadmapFeedbackRepository.findByRoadmapIdAndStudentIdAndNotDeleted(ROADMAP_ID, STUDENT_ID))
          .thenReturn(Optional.empty());
      when(roadmapFeedbackRepository.save(any(RoadmapFeedback.class))).thenReturn(saved);
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(true);

        // ===== ACT =====
        var response = roadmapFeedbackService.submitFeedback(ROADMAP_ID, STUDENT_ID, request);

        // ===== ASSERT =====
        assertEquals(4, response.getRating());
      }
    }

    @Test
    void it_should_throw_assessment_not_found_when_roadmap_missing_or_deleted() {
      // ===== ARRANGE =====
      CreateRoadmapFeedbackRequest request =
          CreateRoadmapFeedbackRequest.builder().rating(4).content("x").build();
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException missing =
          assertThrows(
              AppException.class,
              () -> roadmapFeedbackService.submitFeedback(ROADMAP_ID, STUDENT_ID, request));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, missing.getErrorCode());

      LearningRoadmap deleted = buildRoadmap(ROADMAP_ID, STUDENT_ID, TEACHER_ID);
      deleted.setDeletedAt(Instant.now());
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(deleted));
      AppException deletedEx =
          assertThrows(
              AppException.class,
              () -> roadmapFeedbackService.submitFeedback(ROADMAP_ID, STUDENT_ID, request));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, deletedEx.getErrorCode());
    }

    @Test
    void it_should_throw_unauthorized_when_non_student_role_or_student_not_owner_submits() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap =
          buildRoadmap(
              ROADMAP_ID,
              UUID.fromString("92000000-0000-0000-0000-000000000098"),
              TEACHER_ID);
      CreateRoadmapFeedbackRequest request =
          CreateRoadmapFeedbackRequest.builder().rating(4).content("x").build();
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.STUDENT_ROLE)).thenReturn(false);
        AppException nonStudent =
            assertThrows(
                AppException.class,
                () -> roadmapFeedbackService.submitFeedback(ROADMAP_ID, STUDENT_ID, request));
        assertEquals(ErrorCode.UNAUTHORIZED, nonStudent.getErrorCode());
      }

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.STUDENT_ROLE)).thenReturn(true);
        AppException mismatchOwner =
            assertThrows(
                AppException.class,
                () -> roadmapFeedbackService.submitFeedback(ROADMAP_ID, STUDENT_ID, request));
        assertEquals(ErrorCode.UNAUTHORIZED, mismatchOwner.getErrorCode());
      }
    }
  }

  @Nested
  @DisplayName("getMyFeedback() and getRoadmapFeedbacks()")
  class QueryFeedbackTests {

    @Test
    void it_should_get_my_feedback_when_exists() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, STUDENT_ID, TEACHER_ID);
      RoadmapFeedback feedback = buildFeedback(FEEDBACK_ID, ROADMAP_ID, STUDENT_ID, 5, "Tot");
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(roadmapFeedbackRepository.findByRoadmapIdAndStudentIdAndNotDeleted(ROADMAP_ID, STUDENT_ID))
          .thenReturn(Optional.of(feedback));
      User user = new User();
      user.setFullName("Student Name");
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(user));

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.STUDENT_ROLE)).thenReturn(true);

        // ===== ACT =====
        var response = roadmapFeedbackService.getMyFeedback(ROADMAP_ID, STUDENT_ID);

        // ===== ASSERT =====
        assertEquals("Student Name", response.getStudentName());
      }
    }

    @Test
    void it_should_throw_assessment_not_found_when_my_feedback_not_exists() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, STUDENT_ID, TEACHER_ID);
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(roadmapFeedbackRepository.findByRoadmapIdAndStudentIdAndNotDeleted(ROADMAP_ID, STUDENT_ID))
          .thenReturn(Optional.empty());

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.STUDENT_ROLE)).thenReturn(true);

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(
                AppException.class,
                () -> roadmapFeedbackService.getMyFeedback(ROADMAP_ID, STUDENT_ID));
        assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());
      }
    }

    @Test
    void it_should_get_roadmap_feedbacks_when_reader_is_admin_or_teacher_owner() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, STUDENT_ID, TEACHER_ID);
      RoadmapFeedback feedback = buildFeedback(FEEDBACK_ID, ROADMAP_ID, STUDENT_ID, 4, "On");
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(roadmapFeedbackRepository.findByRoadmapIdAndNotDeleted(ROADMAP_ID, PageRequest.of(0, 10)))
          .thenReturn(new PageImpl<>(List.of(feedback), PageRequest.of(0, 10), 1));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.fromString("92000000-0000-0000-0000-000000000077"));
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(true);
        var pageAdmin = roadmapFeedbackService.getRoadmapFeedbacks(ROADMAP_ID, PageRequest.of(0, 10));
        assertEquals(1, pageAdmin.getTotalElements());
      }

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        var pageTeacher = roadmapFeedbackService.getRoadmapFeedbacks(ROADMAP_ID, PageRequest.of(0, 10));
        assertEquals(1, pageTeacher.getTotalElements());
      }
    }

    @Test
    void it_should_throw_unauthorized_when_reader_is_neither_admin_nor_teacher_owner() {
      // ===== ARRANGE =====
      PageRequest pageable = PageRequest.of(0, 10);
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, STUDENT_ID, TEACHER_ID);
      when(learningRoadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.fromString("92000000-0000-0000-0000-000000000088"));
        mocked.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(
                AppException.class,
                () -> roadmapFeedbackService.getRoadmapFeedbacks(ROADMAP_ID, pageable));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
      }

      verify(roadmapFeedbackRepository, never()).findByRoadmapIdAndNotDeleted(any(), any());
    }
  }
}
