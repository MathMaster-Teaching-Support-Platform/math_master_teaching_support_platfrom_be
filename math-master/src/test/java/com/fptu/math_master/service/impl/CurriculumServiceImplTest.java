package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateCurriculumRequest;
import com.fptu.math_master.dto.request.UpdateCurriculumRequest;
import com.fptu.math_master.dto.response.CurriculumResponse;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.enums.CurriculumCategory;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CurriculumRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("CurriculumServiceImpl - Tests")
class CurriculumServiceImplTest extends BaseUnitTest {

  @InjectMocks private CurriculumServiceImpl curriculumService;

  @Mock private CurriculumRepository curriculumRepository;

  private Curriculum buildCurriculum(
      UUID id, String name, Integer grade, CurriculumCategory category, String description) {
    Curriculum curriculum = new Curriculum();
    curriculum.setId(id);
    curriculum.setName(name);
    curriculum.setGrade(grade);
    curriculum.setCategory(category);
    curriculum.setDescription(description);
    curriculum.setCreatedAt(Instant.parse("2026-04-26T01:00:00Z"));
    curriculum.setUpdatedAt(Instant.parse("2026-04-26T01:00:00Z"));
    return curriculum;
  }

  @Nested
  @DisplayName("createCurriculum()")
  class CreateCurriculumTests {

    @Test
    void it_should_create_curriculum_when_name_grade_category_combination_is_unique() {
      // ===== ARRANGE =====
      CreateCurriculumRequest request =
          CreateCurriculumRequest.builder()
              .name("Toan 12 - Nâng cao")
              .grade(12)
              .category(CurriculumCategory.NUMERICAL)
              .description("Chuong trinh toan hoc nang cao lop 12")
              .build();
      Curriculum saved =
          buildCurriculum(
              UUID.fromString("10000000-0000-0000-0000-000000000001"),
              request.getName(),
              request.getGrade(),
              request.getCategory(),
              request.getDescription());

      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(
              request.getName(), request.getGrade(), request.getCategory()))
          .thenReturn(Optional.empty());
      when(curriculumRepository.save(any(Curriculum.class))).thenReturn(saved);

      // ===== ACT =====
      CurriculumResponse response = curriculumService.createCurriculum(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(saved.getId(), response.getId()),
          () -> assertEquals("Toan 12 - Nâng cao", response.getName()),
          () -> assertEquals(12, response.getGrade()),
          () -> assertEquals(CurriculumCategory.NUMERICAL, response.getCategory()));

      // ===== VERIFY =====
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted(
              request.getName(), request.getGrade(), request.getCategory());
      verify(curriculumRepository, times(1)).save(any(Curriculum.class));
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_throw_exception_when_curriculum_already_exists_on_create() {
      // ===== ARRANGE =====
      CreateCurriculumRequest request =
          CreateCurriculumRequest.builder()
              .name("Toan 10 - Co ban")
              .grade(10)
              .category(CurriculumCategory.GEOMETRY)
              .description("Description")
              .build();
      Curriculum existing =
          buildCurriculum(
              UUID.fromString("10000000-0000-0000-0000-000000000002"),
              request.getName(),
              request.getGrade(),
              request.getCategory(),
              request.getDescription());
      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(
              request.getName(), request.getGrade(), request.getCategory()))
          .thenReturn(Optional.of(existing));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> curriculumService.createCurriculum(request));
      assertEquals(ErrorCode.CURRICULUM_ALREADY_EXISTS, exception.getErrorCode());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted(
              request.getName(), request.getGrade(), request.getCategory());
      verify(curriculumRepository, never()).save(any(Curriculum.class));
      verifyNoMoreInteractions(curriculumRepository);
    }
  }

  @Nested
  @DisplayName("updateCurriculum()")
  class UpdateCurriculumTests {

    @Test
    void it_should_throw_not_found_when_update_target_curriculum_does_not_exist() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("20000000-0000-0000-0000-000000000001");
      UpdateCurriculumRequest request = UpdateCurriculumRequest.builder().name("Toan moi").build();
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> curriculumService.updateCurriculum(curriculumId, request));
      assertEquals(ErrorCode.CURRICULUM_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, never()).save(any(Curriculum.class));
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_update_selected_fields_when_no_unique_key_change_happens() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("20000000-0000-0000-0000-000000000002");
      Curriculum existing =
          buildCurriculum(
              curriculumId,
              "Toan 11",
              11,
              CurriculumCategory.NUMERICAL,
              "Mo ta cu");
      UpdateCurriculumRequest request =
          UpdateCurriculumRequest.builder().description("Mo ta moi chi cap nhat description").build();
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(existing));
      when(curriculumRepository.save(existing)).thenReturn(existing);

      // ===== ACT =====
      CurriculumResponse response = curriculumService.updateCurriculum(curriculumId, request);

      // ===== ASSERT =====
      assertEquals("Mo ta moi chi cap nhat description", response.getDescription());
      assertEquals("Toan 11", response.getName());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, never()).findByNameAndGradeAndCategoryAndNotDeleted(any(), any(), any());
      verify(curriculumRepository, times(1)).save(existing);
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_throw_already_exists_when_update_changes_unique_key_to_existing_combination() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("20000000-0000-0000-0000-000000000003");
      Curriculum existing =
          buildCurriculum(
              curriculumId, "Toan 10", 10, CurriculumCategory.NUMERICAL, "Old");
      UpdateCurriculumRequest request =
          UpdateCurriculumRequest.builder()
              .name("Toan 12")
              .grade(12)
              .category(CurriculumCategory.GEOMETRY)
              .build();
      Curriculum conflict =
          buildCurriculum(
              UUID.fromString("20000000-0000-0000-0000-000000000099"),
              "Toan 12",
              12,
              CurriculumCategory.GEOMETRY,
              "Conflict");
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(existing));
      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(
              "Toan 12", 12, CurriculumCategory.GEOMETRY))
          .thenReturn(Optional.of(conflict));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> curriculumService.updateCurriculum(curriculumId, request));
      assertEquals(ErrorCode.CURRICULUM_ALREADY_EXISTS, exception.getErrorCode());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted("Toan 12", 12, CurriculumCategory.GEOMETRY);
      verify(curriculumRepository, never()).save(any(Curriculum.class));
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_update_when_unique_key_changes_but_no_conflict_found() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("20000000-0000-0000-0000-000000000004");
      Curriculum existing =
          buildCurriculum(
              curriculumId, "Toan 10", 10, CurriculumCategory.NUMERICAL, "Old description");
      UpdateCurriculumRequest request =
          UpdateCurriculumRequest.builder()
              .name("Toan 10 moi")
              .grade(10)
              .category(CurriculumCategory.NUMERICAL)
              .description("New description")
              .build();
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(existing));
      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(
              "Toan 10 moi", 10, CurriculumCategory.NUMERICAL))
          .thenReturn(Optional.empty());
      when(curriculumRepository.save(existing)).thenReturn(existing);

      // ===== ACT =====
      CurriculumResponse response = curriculumService.updateCurriculum(curriculumId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Toan 10 moi", response.getName()),
          () -> assertEquals("New description", response.getDescription()));

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted(
              "Toan 10 moi", 10, CurriculumCategory.NUMERICAL);
      verify(curriculumRepository, times(1)).save(existing);
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_skip_uniqueness_lookup_when_request_unique_fields_match_existing_values() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("20000000-0000-0000-0000-000000000005");
      Curriculum existing =
          buildCurriculum(
              curriculumId, "Toan 11", 11, CurriculumCategory.NUMERICAL, "Old description");
      UpdateCurriculumRequest request =
          UpdateCurriculumRequest.builder()
              .name("Toan 11")
              .grade(11)
              .category(CurriculumCategory.NUMERICAL)
              .description("Description refreshed only")
              .build();
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(existing));
      when(curriculumRepository.save(existing)).thenReturn(existing);

      // ===== ACT =====
      CurriculumResponse response = curriculumService.updateCurriculum(curriculumId, request);

      // ===== ASSERT =====
      assertEquals("Description refreshed only", response.getDescription());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, never()).findByNameAndGradeAndCategoryAndNotDeleted(any(), any(), any());
      verify(curriculumRepository, times(1)).save(existing);
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_keep_all_fields_when_update_request_has_only_null_values() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("20000000-0000-0000-0000-000000000006");
      Curriculum existing =
          buildCurriculum(
              curriculumId, "Toan 8", 8, CurriculumCategory.GEOMETRY, "Static description");
      UpdateCurriculumRequest request = UpdateCurriculumRequest.builder().build();
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(existing));
      when(curriculumRepository.save(existing)).thenReturn(existing);

      // ===== ACT =====
      CurriculumResponse response = curriculumService.updateCurriculum(curriculumId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Toan 8", response.getName()),
          () -> assertEquals(8, response.getGrade()),
          () -> assertEquals(CurriculumCategory.GEOMETRY, response.getCategory()),
          () -> assertEquals("Static description", response.getDescription()));

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, never()).findByNameAndGradeAndCategoryAndNotDeleted(any(), any(), any());
      verify(curriculumRepository, times(1)).save(existing);
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_update_grade_and_category_but_keep_description_when_description_is_null() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("20000000-0000-0000-0000-000000000007");
      Curriculum existing =
          buildCurriculum(
              curriculumId, "Toan tong hop", 10, CurriculumCategory.NUMERICAL, "Keep this description");
      UpdateCurriculumRequest request =
          UpdateCurriculumRequest.builder().grade(12).category(CurriculumCategory.GEOMETRY).build();
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(existing));
      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(
              "Toan tong hop", 12, CurriculumCategory.GEOMETRY))
          .thenReturn(Optional.empty());
      when(curriculumRepository.save(existing)).thenReturn(existing);

      // ===== ACT =====
      CurriculumResponse response = curriculumService.updateCurriculum(curriculumId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(12, response.getGrade()),
          () -> assertEquals(CurriculumCategory.GEOMETRY, response.getCategory()),
          () -> assertEquals("Keep this description", response.getDescription()));

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted(
              "Toan tong hop", 12, CurriculumCategory.GEOMETRY);
      verify(curriculumRepository, times(1)).save(existing);
      verifyNoMoreInteractions(curriculumRepository);
    }
  }

  @Nested
  @DisplayName("read methods")
  class ReadMethodsTests {

    @Test
    void it_should_return_curriculum_by_id_when_existing() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("30000000-0000-0000-0000-000000000001");
      Curriculum curriculum =
          buildCurriculum(
              curriculumId, "Toan 9", 9, CurriculumCategory.NUMERICAL, "Chuong trinh toan 9");
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(curriculum));

      // ===== ACT =====
      CurriculumResponse response = curriculumService.getCurriculumById(curriculumId);

      // ===== ASSERT =====
      assertEquals(curriculumId, response.getId());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_throw_not_found_when_get_curriculum_by_id_missing() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("30000000-0000-0000-0000-000000000002");
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> curriculumService.getCurriculumById(curriculumId));
      assertEquals(ErrorCode.CURRICULUM_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_get_all_curricula_with_pageable() {
      // ===== ARRANGE =====
      PageRequest pageable = PageRequest.of(0, 5);
      Curriculum curriculum =
          buildCurriculum(
              UUID.fromString("30000000-0000-0000-0000-000000000003"),
              "Toan roi rac",
              11,
              CurriculumCategory.NUMERICAL,
              "desc");
      when(curriculumRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(curriculum)));

      // ===== ACT =====
      var page = curriculumService.getAllCurricula(pageable);

      // ===== ASSERT =====
      assertEquals(1, page.getTotalElements());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findAll(pageable);
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_get_all_curricula_without_pageable() {
      // ===== ARRANGE =====
      Curriculum curriculum =
          buildCurriculum(
              UUID.fromString("30000000-0000-0000-0000-000000000004"),
              "Hinh hoc 12",
              12,
              CurriculumCategory.GEOMETRY,
              "desc");
      when(curriculumRepository.findAllNotDeleted()).thenReturn(List.of(curriculum));

      // ===== ACT =====
      List<CurriculumResponse> responses = curriculumService.getAllCurricula();

      // ===== ASSERT =====
      assertEquals(1, responses.size());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findAllNotDeleted();
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_get_curricula_by_grade_and_category_filters() {
      // ===== ARRANGE =====
      Curriculum c1 =
          buildCurriculum(
              UUID.fromString("30000000-0000-0000-0000-000000000005"),
              "Toan 10",
              10,
              CurriculumCategory.NUMERICAL,
              "desc");
      when(curriculumRepository.findByGradeAndNotDeleted(10)).thenReturn(List.of(c1));
      when(curriculumRepository.findByCategoryAndNotDeleted(CurriculumCategory.NUMERICAL))
          .thenReturn(List.of(c1));
      when(curriculumRepository.findByGradeAndCategoryAndNotDeleted(10, CurriculumCategory.NUMERICAL))
          .thenReturn(List.of(c1));

      // ===== ACT =====
      List<CurriculumResponse> byGrade = curriculumService.getCurriculaByGrade(10);
      List<CurriculumResponse> byCategory =
          curriculumService.getCurriculaByCategory(CurriculumCategory.NUMERICAL);
      List<CurriculumResponse> byBoth =
          curriculumService.getCurriculaByGradeAndCategory(10, CurriculumCategory.NUMERICAL);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, byGrade.size()),
          () -> assertEquals(1, byCategory.size()),
          () -> assertEquals(1, byBoth.size()));

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByGradeAndNotDeleted(10);
      verify(curriculumRepository, times(1)).findByCategoryAndNotDeleted(CurriculumCategory.NUMERICAL);
      verify(curriculumRepository, times(1))
          .findByGradeAndCategoryAndNotDeleted(10, CurriculumCategory.NUMERICAL);
      verifyNoMoreInteractions(curriculumRepository);
    }
  }

  @Nested
  @DisplayName("deleteCurriculum() and searchCurriculaByName()")
  class DeleteAndSearchTests {

    @Test
    void it_should_soft_delete_curriculum_when_id_exists() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("40000000-0000-0000-0000-000000000001");
      Curriculum curriculum =
          buildCurriculum(
              curriculumId, "Toan 8", 8, CurriculumCategory.NUMERICAL, "desc");
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(curriculum));
      when(curriculumRepository.save(curriculum)).thenReturn(curriculum);

      // ===== ACT =====
      curriculumService.deleteCurriculum(curriculumId);

      // ===== ASSERT =====
      assertNotNull(curriculum.getDeletedAt());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, times(1)).save(curriculum);
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_throw_not_found_when_delete_curriculum_missing() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("40000000-0000-0000-0000-000000000002");
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> curriculumService.deleteCurriculum(curriculumId));
      assertEquals(ErrorCode.CURRICULUM_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findByIdAndNotDeleted(curriculumId);
      verify(curriculumRepository, never()).save(any(Curriculum.class));
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_return_all_curricula_when_search_name_is_null_or_blank() {
      // ===== ARRANGE =====
      Curriculum c1 =
          buildCurriculum(
              UUID.fromString("40000000-0000-0000-0000-000000000003"),
              "Toan 10",
              10,
              CurriculumCategory.NUMERICAL,
              "desc");
      Curriculum c2 =
          buildCurriculum(
              UUID.fromString("40000000-0000-0000-0000-000000000004"),
              "Hinh hoc 10",
              10,
              CurriculumCategory.GEOMETRY,
              "desc");
      when(curriculumRepository.findAllNotDeleted()).thenReturn(List.of(c1, c2));

      // ===== ACT =====
      List<CurriculumResponse> forNull = curriculumService.searchCurriculaByName(null);
      List<CurriculumResponse> forBlank = curriculumService.searchCurriculaByName("   ");

      // ===== ASSERT =====
      assertAll(() -> assertEquals(2, forNull.size()), () -> assertEquals(2, forBlank.size()));

      // ===== VERIFY =====
      verify(curriculumRepository, times(2)).findAllNotDeleted();
      verifyNoMoreInteractions(curriculumRepository);
    }

    @Test
    void it_should_filter_curricula_case_insensitive_when_search_name_has_text() {
      // ===== ARRANGE =====
      Curriculum c1 =
          buildCurriculum(
              UUID.fromString("40000000-0000-0000-0000-000000000005"),
              "Toan roi rac",
              11,
              CurriculumCategory.NUMERICAL,
              "desc");
      Curriculum c2 =
          buildCurriculum(
              UUID.fromString("40000000-0000-0000-0000-000000000006"),
              "Hinh hoc khong gian",
              11,
              CurriculumCategory.GEOMETRY,
              "desc");
      when(curriculumRepository.findAllNotDeleted()).thenReturn(List.of(c1, c2));

      // ===== ACT =====
      List<CurriculumResponse> result = curriculumService.searchCurriculaByName("  TOAN  ");

      // ===== ASSERT =====
      assertAll(() -> assertEquals(1, result.size()), () -> assertTrue(result.get(0).getName().contains("Toan")));

      // ===== VERIFY =====
      verify(curriculumRepository, times(1)).findAllNotDeleted();
      verifyNoMoreInteractions(curriculumRepository);
    }
  }
}
