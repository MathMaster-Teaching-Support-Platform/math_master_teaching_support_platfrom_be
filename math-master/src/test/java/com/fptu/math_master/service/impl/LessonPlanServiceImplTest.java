package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateLessonPlanRequest;
import com.fptu.math_master.dto.request.UpdateLessonPlanRequest;
import com.fptu.math_master.dto.response.LessonPlanResponse;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.LessonPlan;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.LessonPlanRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.util.SecurityUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@DisplayName("LessonPlanServiceImpl - Tests")
class LessonPlanServiceImplTest extends BaseUnitTest {

  @InjectMocks private LessonPlanServiceImpl service;
  @Mock private LessonPlanRepository lessonPlanRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private UserRepository userRepository;

  private UUID teacherId;
  private UUID lessonId;
  private UUID lessonPlanId;
  private LessonPlan lessonPlan;
  private Lesson lesson;

  @BeforeEach
  void setUp() {
    teacherId = UUID.randomUUID();
    lessonId = UUID.randomUUID();
    lessonPlanId = UUID.randomUUID();

    lesson = new Lesson();
    lesson.setId(lessonId);
    lesson.setTitle("Functions and Graphs");

    lessonPlan = LessonPlan.builder().lessonId(lessonId).teacherId(teacherId).build();
    lessonPlan.setId(lessonPlanId);
  }

  /** Normal case: Creates lesson plan for authenticated teacher. */
  @Test
  void it_should_create_lesson_plan_when_request_is_valid() {
    // ===== ARRANGE =====
    CreateLessonPlanRequest request =
        CreateLessonPlanRequest.builder()
            .lessonId(lessonId)
            .objectives(new String[] {"Understand function domain and range"})
            .materialsNeeded(new String[] {"Projector and graph papers"})
            .teachingStrategy("Guided practice")
            .assessmentMethods("In-class quiz")
            .notes("Focus on common mistakes")
            .build();
    User teacher = User.builder().fullName("Pham Dang Khoi").build();
    teacher.setId(teacherId);

    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(lessonPlanRepository.existsByLessonIdAndTeacherIdAndNotDeleted(lessonId, teacherId))
          .thenReturn(false);
      when(lessonPlanRepository.save(any(LessonPlan.class))).thenReturn(lessonPlan);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

      // ===== ACT =====
      LessonPlanResponse result = service.createLessonPlan(request);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(lessonPlanId, result.getId());
      assertEquals("Functions and Graphs", result.getLessonTitle());
      assertEquals("Pham Dang Khoi", result.getTeacherName());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verify(lessonPlanRepository, times(1)).save(any(LessonPlan.class));
    }
  }

  /** Abnormal case: Throws when duplicate lesson plan already exists. */
  @Test
  void it_should_throw_exception_when_lesson_plan_already_exists() {
    // ===== ARRANGE =====
    CreateLessonPlanRequest request = CreateLessonPlanRequest.builder().lessonId(lessonId).build();
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(lessonPlanRepository.existsByLessonIdAndTeacherIdAndNotDeleted(lessonId, teacherId))
          .thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> service.createLessonPlan(request));
      assertEquals(ErrorCode.LESSON_PLAN_ALREADY_EXISTS, exception.getErrorCode());
      verify(lessonPlanRepository, never()).save(any(LessonPlan.class));
    }
  }

  /** Abnormal case: Denies update when user is not owner and not admin. */
  @Test
  void it_should_throw_access_denied_when_updating_foreign_lesson_plan() {
    // ===== ARRANGE =====
    UUID anotherTeacher = UUID.randomUUID();
    UpdateLessonPlanRequest request = UpdateLessonPlanRequest.builder().notes("Updated notes").build();
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(anotherTeacher);
      mocked.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
      when(lessonPlanRepository.findByIdAndNotDeleted(lessonPlanId)).thenReturn(Optional.of(lessonPlan));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> service.updateLessonPlan(lessonPlanId, request));
      assertEquals(ErrorCode.LESSON_PLAN_ACCESS_DENIED, exception.getErrorCode());
      verify(lessonPlanRepository, never()).save(any(LessonPlan.class));
    }
  }

  /** Normal case: Allows admin to update foreign lesson plan. */
  @Test
  void it_should_allow_admin_to_update_foreign_lesson_plan() {
    // ===== ARRANGE =====
    UpdateLessonPlanRequest request = UpdateLessonPlanRequest.builder().notes("Admin adjusted notes").build();
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());
      mocked.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
      when(lessonPlanRepository.findByIdAndNotDeleted(lessonPlanId)).thenReturn(Optional.of(lessonPlan));
      when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(inv -> inv.getArgument(0));
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(User.builder().fullName("Teacher A").build()));

      // ===== ACT =====
      LessonPlanResponse result = service.updateLessonPlan(lessonPlanId, request);

      // ===== ASSERT =====
      assertEquals("Admin adjusted notes", result.getNotes());
      verify(lessonPlanRepository, times(1)).save(any(LessonPlan.class));
    }
  }

  /** Normal case: Updates all mutable fields when non-null values are provided. */
  @Test
  void it_should_update_all_fields_when_update_request_contains_values() {
    // ===== ARRANGE =====
    String[] objectives = new String[] {"Master derivative rules"};
    String[] materials = new String[] {"Worksheet", "Calculator"};
    UpdateLessonPlanRequest request =
        UpdateLessonPlanRequest.builder()
            .objectives(objectives)
            .materialsNeeded(materials)
            .teachingStrategy("Peer instruction")
            .assessmentMethods("Exit ticket")
            .notes("Bring extra examples")
            .build();
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(lessonPlanRepository.findByIdAndNotDeleted(lessonPlanId)).thenReturn(Optional.of(lessonPlan));
      when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(inv -> inv.getArgument(0));
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(User.builder().fullName("Teacher A").build()));

      // ===== ACT =====
      LessonPlanResponse result = service.updateLessonPlan(lessonPlanId, request);

      // ===== ASSERT =====
      assertArrayEquals(objectives, result.getObjectives());
      assertArrayEquals(materials, result.getMaterialsNeeded());
      assertEquals("Peer instruction", result.getTeachingStrategy());
      assertEquals("Exit ticket", result.getAssessmentMethods());
      assertEquals("Bring extra examples", result.getNotes());
    }
  }

  /** Normal case: Gets my lesson plans and maps lesson title per record. */
  @Test
  void it_should_return_my_lesson_plans_with_lesson_title_and_teacher_name() {
    // ===== ARRANGE =====
    LessonPlan secondPlan =
        LessonPlan.builder().lessonId(UUID.randomUUID()).teacherId(teacherId).build();
    secondPlan.setId(UUID.randomUUID());
    Lesson secondLesson = new Lesson();
    secondLesson.setId(secondPlan.getLessonId());
    secondLesson.setTitle("Derivative Rules");
    User teacher = User.builder().fullName("Pham Dang Khoi").build();
    teacher.setId(teacherId);

    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
      when(lessonPlanRepository.findByTeacherIdAndNotDeleted(teacherId)).thenReturn(List.of(lessonPlan, secondPlan));
      when(lessonRepository.findByIdAndNotDeleted(lessonPlan.getLessonId())).thenReturn(Optional.of(lesson));
      when(lessonRepository.findByIdAndNotDeleted(secondPlan.getLessonId())).thenReturn(Optional.of(secondLesson));

      // ===== ACT =====
      List<LessonPlanResponse> result = service.getMyLessonPlans();

      // ===== ASSERT =====
      assertEquals(2, result.size());
      assertEquals("Functions and Graphs", result.get(0).getLessonTitle());
      assertEquals("Derivative Rules", result.get(1).getLessonTitle());
    }
  }

  /** Abnormal case: Throws when deleting lesson plan without permission. */
  @Test
  void it_should_throw_access_denied_when_deleting_foreign_lesson_plan() {
    // ===== ARRANGE =====
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());
      mocked.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
      when(lessonPlanRepository.findByIdAndNotDeleted(lessonPlanId)).thenReturn(Optional.of(lessonPlan));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> service.deleteLessonPlan(lessonPlanId));
      assertEquals(ErrorCode.LESSON_PLAN_ACCESS_DENIED, exception.getErrorCode());
      verify(lessonPlanRepository, never()).save(any(LessonPlan.class));
    }
  }

  /** Normal case: Gets lesson plan by id for owner account. */
  @Test
  void it_should_get_lesson_plan_by_id_when_user_is_owner() {
    // ===== ARRANGE =====
    User teacher = User.builder().fullName("Pham Dang Khoi").build();
    teacher.setId(teacherId);
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(lessonPlanRepository.findByIdAndNotDeleted(lessonPlanId)).thenReturn(Optional.of(lessonPlan));
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

      // ===== ACT =====
      LessonPlanResponse result = service.getLessonPlanById(lessonPlanId);

      // ===== ASSERT =====
      assertEquals(lessonPlanId, result.getId());
      assertEquals("Functions and Graphs", result.getLessonTitle());
      assertEquals("Pham Dang Khoi", result.getTeacherName());
    }
  }

  /** Abnormal case: Denies get-by-id when current user is not owner and not admin. */
  @Test
  void it_should_throw_access_denied_when_getting_lesson_plan_by_id_without_permission() {
    // ===== ARRANGE =====
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.randomUUID());
      mocked.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
      when(lessonPlanRepository.findByIdAndNotDeleted(lessonPlanId)).thenReturn(Optional.of(lessonPlan));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> service.getLessonPlanById(lessonPlanId));
      assertEquals(ErrorCode.LESSON_PLAN_ACCESS_DENIED, exception.getErrorCode());
    }
  }

  /** Abnormal case: Throws when requested lesson plan id does not exist. */
  @Test
  void it_should_throw_not_found_when_get_lesson_plan_by_id_missing() {
    // ===== ARRANGE =====
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(lessonPlanRepository.findByIdAndNotDeleted(lessonPlanId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> service.getLessonPlanById(lessonPlanId));
      assertEquals(ErrorCode.LESSON_PLAN_NOT_FOUND, exception.getErrorCode());
    }
  }

  /** Normal case: Returns current teacher lesson plan by lesson id. */
  @Test
  void it_should_return_my_lesson_plan_by_lesson_when_exists() {
    // ===== ARRANGE =====
    User teacher = User.builder().fullName("Pham Dang Khoi").build();
    teacher.setId(teacherId);
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(lessonPlanRepository.findByTeacherIdAndLessonIdAndNotDeleted(teacherId, lessonId))
          .thenReturn(Optional.of(lessonPlan));
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

      // ===== ACT =====
      LessonPlanResponse result = service.getMyLessonPlanByLesson(lessonId);

      // ===== ASSERT =====
      assertEquals(lessonPlanId, result.getId());
      assertEquals("Functions and Graphs", result.getLessonTitle());
    }
  }

  /** Abnormal case: Throws when no lesson plan exists for current teacher and lesson. */
  @Test
  void it_should_throw_not_found_when_my_lesson_plan_by_lesson_missing() {
    // ===== ARRANGE =====
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(lessonPlanRepository.findByTeacherIdAndLessonIdAndNotDeleted(teacherId, lessonId))
          .thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> service.getMyLessonPlanByLesson(lessonId));
      assertEquals(ErrorCode.LESSON_PLAN_NOT_FOUND, exception.getErrorCode());
    }
  }

  /** Normal case: Returns lesson plans by lesson id and resolves teacher names independently. */
  @Test
  void it_should_return_lesson_plans_by_lesson_when_lesson_exists() {
    // ===== ARRANGE =====
    UUID teacher2Id = UUID.randomUUID();
    LessonPlan secondPlan =
        LessonPlan.builder().lessonId(lessonId).teacherId(teacher2Id).notes("Second teacher plan").build();
    secondPlan.setId(UUID.randomUUID());
    User teacher1 = User.builder().fullName("Teacher One").build();
    teacher1.setId(teacherId);
    User teacher2 = User.builder().fullName("Teacher Two").build();
    teacher2.setId(teacher2Id);

    when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson), Optional.of(lesson));
    when(lessonPlanRepository.findByLessonIdAndNotDeleted(lessonId)).thenReturn(List.of(lessonPlan, secondPlan));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher1));
    when(userRepository.findById(teacher2Id)).thenReturn(Optional.of(teacher2));

    // ===== ACT =====
    List<LessonPlanResponse> result = service.getLessonPlansByLesson(lessonId);

    // ===== ASSERT =====
    assertEquals(2, result.size());
    assertEquals("Teacher One", result.get(0).getTeacherName());
    assertEquals("Teacher Two", result.get(1).getTeacherName());
  }

  /** Abnormal case: Throws when requesting lesson plans of a missing lesson. */
  @Test
  void it_should_throw_lesson_not_found_when_getting_lesson_plans_by_lesson_missing() {
    // ===== ARRANGE =====
    when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception =
        assertThrows(AppException.class, () -> service.getLessonPlansByLesson(lessonId));
    assertEquals(ErrorCode.LESSON_NOT_FOUND, exception.getErrorCode());
    verify(lessonPlanRepository, never()).findByLessonIdAndNotDeleted(any());
  }

  /** Normal case: Owner soft deletes lesson plan and stamps deleted metadata. */
  @Test
  void it_should_soft_delete_lesson_plan_when_user_is_owner() {
    // ===== ARRANGE =====
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
      when(lessonPlanRepository.findByIdAndNotDeleted(lessonPlanId)).thenReturn(Optional.of(lessonPlan));
      when(lessonPlanRepository.save(any(LessonPlan.class))).thenAnswer(inv -> inv.getArgument(0));

      // ===== ACT =====
      service.deleteLessonPlan(lessonPlanId);

      // ===== ASSERT =====
      assertNotNull(lessonPlan.getDeletedAt());
      assertEquals(teacherId, lessonPlan.getDeletedBy());

      // ===== VERIFY =====
      verify(lessonPlanRepository, times(1)).save(lessonPlan);
    }
  }
}
