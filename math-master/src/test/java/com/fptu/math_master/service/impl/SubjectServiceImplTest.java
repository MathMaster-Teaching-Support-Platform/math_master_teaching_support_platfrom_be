package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateSubjectRequest;
import com.fptu.math_master.dto.request.LinkGradeSubjectRequest;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("SubjectServiceImpl - Tests")
class SubjectServiceImplTest extends BaseUnitTest {

  @InjectMocks private SubjectServiceImpl subjectService;

  @Mock private SubjectRepository subjectRepository;
  @Mock private SchoolGradeRepository schoolGradeRepository;

  private static final UUID SUBJECT_ID = UUID.fromString("91000000-0000-0000-0000-000000000001");
  private static final UUID GRADE_ID = UUID.fromString("91000000-0000-0000-0000-000000000002");

  private SchoolGrade buildGrade(UUID id, Integer level, boolean active) {
    SchoolGrade grade = new SchoolGrade();
    grade.setId(id);
    grade.setGradeLevel(level);
    grade.setName("Lop " + level);
    grade.setIsActive(active);
    return grade;
  }

  private Subject buildSubject(UUID id, String name, String code, UUID schoolGradeId, boolean active) {
    Subject subject = new Subject();
    subject.setId(id);
    subject.setName(name);
    subject.setCode(code);
    subject.setSchoolGradeId(schoolGradeId);
    subject.setIsActive(active);
    subject.setCreatedAt(Instant.parse("2026-04-26T06:00:00Z"));
    subject.setUpdatedAt(Instant.parse("2026-04-26T06:00:00Z"));
    return subject;
  }

  @Nested
  @DisplayName("createSubject()")
  class CreateSubjectTests {

    @Test
    void it_should_create_subject_and_generate_unique_code_from_unicode_name() {
      // ===== ARRANGE =====
      CreateSubjectRequest request =
          CreateSubjectRequest.builder().name("Đại số 10").schoolGradeId(GRADE_ID).build();
      SchoolGrade grade = buildGrade(GRADE_ID, 10, true);
      Subject saved = buildSubject(SUBJECT_ID, "Đại số 10", "DAI_SO_10_2", GRADE_ID, true);
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(grade));
      when(subjectRepository.existsByCode("DAI_SO_10")).thenReturn(true);
      when(subjectRepository.existsByCode("DAI_SO_10_2")).thenReturn(false);
      when(subjectRepository.save(any(Subject.class))).thenReturn(saved);

      // ===== ACT =====
      var response = subjectService.createSubject(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(SUBJECT_ID, response.getId()),
          () -> assertEquals("DAI_SO_10_2", response.getCode()),
          () -> assertEquals(GRADE_ID, response.getSchoolGradeId()),
          () -> assertTrue(response.getGradeLevels().isEmpty()));
    }

    @Test
    void it_should_create_subject_with_fallback_code_when_name_has_no_ascii_letters_or_digits() {
      // ===== ARRANGE =====
      CreateSubjectRequest request =
          CreateSubjectRequest.builder().name("___").schoolGradeId(GRADE_ID).build();
      SchoolGrade grade = buildGrade(GRADE_ID, 6, true);
      Subject saved = buildSubject(SUBJECT_ID, "___", "SUBJECT_2", GRADE_ID, true);
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(grade));
      when(subjectRepository.existsByCode("SUBJECT")).thenReturn(true);
      when(subjectRepository.existsByCode("SUBJECT_2")).thenReturn(false);
      when(subjectRepository.save(any(Subject.class))).thenReturn(saved);

      // ===== ACT =====
      var response = subjectService.createSubject(request);

      // ===== ASSERT =====
      assertEquals("SUBJECT_2", response.getCode());
    }

    @Test
    void it_should_throw_school_grade_not_found_when_grade_missing_in_create_subject() {
      // ===== ARRANGE =====
      CreateSubjectRequest request =
          CreateSubjectRequest.builder().name("Hinh hoc").schoolGradeId(GRADE_ID).build();
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> subjectService.createSubject(request));
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_throw_school_grade_not_found_when_grade_is_deleted_or_inactive() {
      // ===== ARRANGE =====
      CreateSubjectRequest request =
          CreateSubjectRequest.builder().name("Hinh hoc").schoolGradeId(GRADE_ID).build();
      SchoolGrade inactive = buildGrade(GRADE_ID, 7, false);
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(inactive));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> subjectService.createSubject(request));
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("query methods")
  class QueryTests {

    @Test
    void it_should_get_subject_by_id_when_subject_exists() {
      // ===== ARRANGE =====
      Subject subject = buildSubject(SUBJECT_ID, "Dai so", "DAI_SO", GRADE_ID, true);
      SchoolGrade grade = buildGrade(GRADE_ID, 8, true);
      subject.setSchoolGrade(grade);
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));

      // ===== ACT =====
      var response = subjectService.getSubjectById(SUBJECT_ID);

      // ===== ASSERT =====
      assertEquals(8, response.getPrimaryGradeLevel());
    }

    @Test
    void it_should_throw_subject_not_found_when_get_subject_by_invalid_id() {
      // ===== ARRANGE =====
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> subjectService.getSubjectById(SUBJECT_ID));
      assertEquals(ErrorCode.SUBJECT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_get_all_subjects_and_map_empty_grade_levels_when_grade_missing() {
      // ===== ARRANGE =====
      Subject s1 = buildSubject(SUBJECT_ID, "Dai so", "DAI_SO", null, true);
      when(subjectRepository.findAllActive()).thenReturn(List.of(s1));

      // ===== ACT =====
      var results = subjectService.getAllSubjects();

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, results.size()),
          () -> assertTrue(results.get(0).getGradeLevels().isEmpty()));
    }

    @Test
    void it_should_get_subjects_by_grade_level_and_by_school_grade_id() {
      // ===== ARRANGE =====
      Subject s1 = buildSubject(SUBJECT_ID, "Hinh hoc", "HINH_HOC", GRADE_ID, true);
      SchoolGrade grade = buildGrade(GRADE_ID, 9, true);
      s1.setSchoolGrade(grade);
      when(subjectRepository.findActiveByGradeLevel(9)).thenReturn(List.of(s1));
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(grade));
      when(subjectRepository.findBySchoolGradeIdAndIsActiveTrueOrderByName(GRADE_ID)).thenReturn(List.of(s1));

      // ===== ACT =====
      var byLevel = subjectService.getSubjectsByGrade(9);
      var byGradeId = subjectService.getSubjectsBySchoolGradeId(GRADE_ID);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(1, byLevel.size()), () -> assertEquals(1, byGradeId.size()));
    }

    @Test
    void it_should_throw_school_grade_not_found_when_get_subjects_by_school_grade_id_for_inactive_grade() {
      // ===== ARRANGE =====
      SchoolGrade inactive = buildGrade(GRADE_ID, 9, false);
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(inactive));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> subjectService.getSubjectsBySchoolGradeId(GRADE_ID));
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("linkToGrade(), unlinkFromGrade(), deactivateSubject()")
  class MutatingTests {

    @Test
    void it_should_link_subject_to_new_grade_when_grade_level_exists_and_active() {
      // ===== ARRANGE =====
      Subject subject = buildSubject(SUBJECT_ID, "Dai so", "DAI_SO", GRADE_ID, true);
      SchoolGrade targetGrade = buildGrade(UUID.fromString("91000000-0000-0000-0000-000000000009"), 11, true);
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
      when(schoolGradeRepository.findByGradeLevel(11)).thenReturn(Optional.of(targetGrade));
      when(subjectRepository.save(subject)).thenReturn(subject);

      // ===== ACT =====
      var response = subjectService.linkToGrade(SUBJECT_ID, LinkGradeSubjectRequest.builder().gradeLevel(11).build());

      // ===== ASSERT =====
      assertEquals(targetGrade.getId(), response.getSchoolGradeId());
    }

    @Test
    void it_should_throw_school_grade_not_found_when_link_grade_level_missing_or_inactive() {
      // ===== ARRANGE =====
      LinkGradeSubjectRequest request = LinkGradeSubjectRequest.builder().gradeLevel(12).build();
      Subject subject = buildSubject(SUBJECT_ID, "Dai so", "DAI_SO", GRADE_ID, true);
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
      SchoolGrade inactive = buildGrade(GRADE_ID, 12, false);
      when(schoolGradeRepository.findByGradeLevel(12)).thenReturn(Optional.of(inactive));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> subjectService.linkToGrade(SUBJECT_ID, request));
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_unlink_subject_when_grade_matches_current_school_grade() {
      // ===== ARRANGE =====
      Subject subject = buildSubject(SUBJECT_ID, "Dai so", "DAI_SO", GRADE_ID, true);
      subject.setSchoolGrade(buildGrade(GRADE_ID, 10, true));
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
      when(subjectRepository.save(subject)).thenReturn(subject);

      // ===== ACT =====
      subjectService.unlinkFromGrade(SUBJECT_ID, 10);

      // ===== ASSERT =====
      assertEquals(null, subject.getSchoolGradeId());
    }

    @Test
    void it_should_throw_grade_subject_not_found_when_unlink_grade_mismatch_or_null_school_grade() {
      // ===== ARRANGE =====
      Subject subjectWithNull = buildSubject(SUBJECT_ID, "Dai so", "DAI_SO", GRADE_ID, true);
      subjectWithNull.setSchoolGrade(null);
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subjectWithNull));

      // ===== ACT & ASSERT =====
      AppException exceptionNull =
          assertThrows(AppException.class, () -> subjectService.unlinkFromGrade(SUBJECT_ID, 10));
      assertEquals(ErrorCode.GRADE_SUBJECT_NOT_FOUND, exceptionNull.getErrorCode());

      Subject subjectWithOtherGrade = buildSubject(SUBJECT_ID, "Dai so", "DAI_SO", GRADE_ID, true);
      subjectWithOtherGrade.setSchoolGrade(buildGrade(GRADE_ID, 11, true));
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subjectWithOtherGrade));

      AppException exceptionMismatch =
          assertThrows(AppException.class, () -> subjectService.unlinkFromGrade(SUBJECT_ID, 10));
      assertEquals(ErrorCode.GRADE_SUBJECT_NOT_FOUND, exceptionMismatch.getErrorCode());
    }

    @Test
    void it_should_deactivate_subject_by_setting_active_false() {
      // ===== ARRANGE =====
      Subject subject = buildSubject(SUBJECT_ID, "Dai so", "DAI_SO", GRADE_ID, true);
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
      when(subjectRepository.save(subject)).thenReturn(subject);

      // ===== ACT =====
      subjectService.deactivateSubject(SUBJECT_ID);

      // ===== ASSERT =====
      assertEquals(false, subject.getIsActive());
      verify(subjectRepository, times(1)).save(subject);
      verify(schoolGradeRepository, never()).findByGradeLevel(any());
    }
  }
}
