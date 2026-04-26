package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateSchoolGradeRequest;
import com.fptu.math_master.dto.request.UpdateSchoolGradeRequest;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.SchoolGradeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("SchoolGradeServiceImpl - Tests")
class SchoolGradeServiceImplTest extends BaseUnitTest {

  @InjectMocks private SchoolGradeServiceImpl schoolGradeService;

  @Mock private SchoolGradeRepository schoolGradeRepository;

  private static final UUID GRADE_ID = UUID.fromString("93000000-0000-0000-0000-000000000001");

  private SchoolGrade buildGrade(UUID id, int level, String name, Boolean active) {
    SchoolGrade grade = new SchoolGrade();
    grade.setId(id);
    grade.setGradeLevel(level);
    grade.setName(name);
    grade.setDescription("Mo ta");
    grade.setIsActive(active);
    grade.setCreatedAt(Instant.parse("2026-04-26T07:00:00Z"));
    grade.setUpdatedAt(Instant.parse("2026-04-26T07:00:00Z"));
    return grade;
  }

  @Nested
  @DisplayName("create()")
  class CreateTests {

    @Test
    void it_should_create_school_grade_when_grade_level_not_exists() {
      // ===== ARRANGE =====
      CreateSchoolGradeRequest request =
          CreateSchoolGradeRequest.builder().gradeLevel(10).name("Lop 10").description("desc").build();
      SchoolGrade saved = buildGrade(GRADE_ID, 10, "Lop 10", true);
      when(schoolGradeRepository.existsByGradeLevel(10)).thenReturn(false);
      when(schoolGradeRepository.save(any(SchoolGrade.class))).thenReturn(saved);

      // ===== ACT =====
      var response = schoolGradeService.create(request);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(10, response.getGradeLevel()), () -> assertEquals(true, response.getActive()));
    }

    @Test
    void it_should_throw_already_exists_when_create_with_existing_grade_level() {
      // ===== ARRANGE =====
      CreateSchoolGradeRequest request =
          CreateSchoolGradeRequest.builder().gradeLevel(10).name("Lop 10").build();
      when(schoolGradeRepository.existsByGradeLevel(10)).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> schoolGradeService.create(request));
      assertEquals(ErrorCode.SCHOOL_GRADE_ALREADY_EXISTS, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("update()")
  class UpdateTests {

    @Test
    void it_should_update_all_fields_when_request_provides_valid_values() {
      // ===== ARRANGE =====
      SchoolGrade grade = buildGrade(GRADE_ID, 10, "Lop 10", true);
      UpdateSchoolGradeRequest request =
          UpdateSchoolGradeRequest.builder()
              .gradeLevel(11)
              .name("Lop 11")
              .description("Mo ta moi")
              .active(false)
              .build();
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(grade));
      when(schoolGradeRepository.existsByGradeLevel(11)).thenReturn(false);
      when(schoolGradeRepository.save(grade)).thenReturn(grade);

      // ===== ACT =====
      var response = schoolGradeService.update(GRADE_ID, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(11, response.getGradeLevel()),
          () -> assertEquals("Lop 11", response.getName()),
          () -> assertEquals("Mo ta moi", response.getDescription()),
          () -> assertEquals(false, response.getActive()));
    }

    @Test
    void it_should_skip_duplicate_check_when_grade_level_unchanged_or_null_and_ignore_blank_name() {
      // ===== ARRANGE =====
      SchoolGrade grade = buildGrade(GRADE_ID, 10, "Ten cu", true);
      UpdateSchoolGradeRequest request =
          UpdateSchoolGradeRequest.builder().gradeLevel(10).name("  ").description(null).active(null).build();
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(grade));
      when(schoolGradeRepository.save(grade)).thenReturn(grade);

      // ===== ACT =====
      var response = schoolGradeService.update(GRADE_ID, request);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(10, response.getGradeLevel()), () -> assertEquals("Ten cu", response.getName()));
      verify(schoolGradeRepository, never()).existsByGradeLevel(any());
    }

    @Test
    void it_should_throw_not_found_when_update_unknown_school_grade() {
      // ===== ARRANGE =====
      UpdateSchoolGradeRequest request = UpdateSchoolGradeRequest.builder().name("x").build();
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> schoolGradeService.update(GRADE_ID, request));
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_throw_already_exists_when_update_to_existing_other_grade_level() {
      // ===== ARRANGE =====
      SchoolGrade grade = buildGrade(GRADE_ID, 10, "Lop 10", true);
      UpdateSchoolGradeRequest request = UpdateSchoolGradeRequest.builder().gradeLevel(11).build();
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(grade));
      when(schoolGradeRepository.existsByGradeLevel(11)).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> schoolGradeService.update(GRADE_ID, request));
      assertEquals(ErrorCode.SCHOOL_GRADE_ALREADY_EXISTS, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("getById(), getAll(), deactivate()")
  class ReadAndDeactivateTests {

    @Test
    void it_should_get_school_grade_by_id_when_exists() {
      // ===== ARRANGE =====
      SchoolGrade grade = buildGrade(GRADE_ID, 8, "Lop 8", true);
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(grade));

      // ===== ACT =====
      var response = schoolGradeService.getById(GRADE_ID);

      // ===== ASSERT =====
      assertEquals("Lop 8", response.getName());
    }

    @Test
    void it_should_throw_not_found_when_get_by_id_missing() {
      // ===== ARRANGE =====
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> schoolGradeService.getById(GRADE_ID));
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_get_all_school_grades_with_active_only_flag_both_true_and_false() {
      // ===== ARRANGE =====
      SchoolGrade active = buildGrade(GRADE_ID, 6, "Lop 6", true);
      SchoolGrade inactive = buildGrade(UUID.fromString("93000000-0000-0000-0000-000000000009"), 7, "Lop 7", false);
      when(schoolGradeRepository.findAllActiveNotDeleted()).thenReturn(List.of(active));
      when(schoolGradeRepository.findAllNotDeleted()).thenReturn(List.of(active, inactive));

      // ===== ACT =====
      var activeOnly = schoolGradeService.getAll(true);
      var all = schoolGradeService.getAll(false);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(1, activeOnly.size()), () -> assertEquals(2, all.size()));
    }

    @Test
    void it_should_deactivate_school_grade_by_soft_deleting_and_setting_active_false() {
      // ===== ARRANGE =====
      SchoolGrade grade = buildGrade(GRADE_ID, 9, "Lop 9", true);
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.of(grade));
      when(schoolGradeRepository.save(grade)).thenReturn(grade);

      // ===== ACT =====
      schoolGradeService.deactivate(GRADE_ID);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(grade.getDeletedAt()), () -> assertEquals(false, grade.getIsActive()));
      verify(schoolGradeRepository, times(1)).save(grade);
    }

    @Test
    void it_should_throw_not_found_when_deactivate_unknown_school_grade() {
      // ===== ARRANGE =====
      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> schoolGradeService.deactivate(GRADE_ID));
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, exception.getErrorCode());
    }
  }
}
