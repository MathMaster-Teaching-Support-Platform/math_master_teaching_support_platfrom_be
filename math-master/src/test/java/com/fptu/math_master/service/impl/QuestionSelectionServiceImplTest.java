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
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.ExamMatrixBankMapping;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ExamMatrixBankMappingRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.service.QuestionSelectionService.SelectionPlan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("QuestionSelectionServiceImpl - Tests")
class QuestionSelectionServiceImplTest extends BaseUnitTest {

  @InjectMocks private QuestionSelectionServiceImpl questionSelectionService;

  @Mock private ExamMatrixBankMappingRepository examMatrixBankMappingRepository;
  @Mock private QuestionRepository questionRepository;

  private ExamMatrixBankMapping buildMapping(
      UUID id,
      UUID examMatrixId,
      UUID bankId,
      Integer questionCount,
      BigDecimal points,
      CognitiveLevel cognitiveLevel,
      Instant createdAt) {
    ExamMatrixBankMapping mapping = new ExamMatrixBankMapping();
    mapping.setId(id);
    mapping.setExamMatrixId(examMatrixId);
    mapping.setQuestionBankId(bankId);
    mapping.setQuestionCount(questionCount);
    mapping.setPointsPerQuestion(points);
    mapping.setCognitiveLevel(cognitiveLevel);
    mapping.setCreatedAt(createdAt);
    return mapping;
  }

  private Question buildQuestion(UUID id) {
    Question q = new Question();
    q.setId(id);
    return q;
  }

  @Nested
  @DisplayName("validateAvailability()")
  class ValidateAvailabilityTests {

    @Test
    void it_should_throw_validation_failed_when_matrix_has_no_mapping() {
      // ===== ARRANGE =====
      UUID matrixId = UUID.fromString("60000000-0000-0000-0000-000000000001");
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId)).thenReturn(List.of());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> questionSelectionService.validateAvailability(matrixId));
      assertEquals(ErrorCode.MATRIX_VALIDATION_FAILED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixBankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(examMatrixBankMappingRepository, questionRepository);
    }

    @Test
    void it_should_skip_count_check_when_required_question_count_is_zero_or_negative() {
      // ===== ARRANGE =====
      UUID matrixId = UUID.fromString("60000000-0000-0000-0000-000000000002");
      ExamMatrixBankMapping zeroRequired =
          buildMapping(
              UUID.fromString("60000000-0000-0000-0000-000000000003"),
              matrixId,
              UUID.fromString("60000000-0000-0000-0000-000000000004"),
              0,
              BigDecimal.ONE,
              CognitiveLevel.APPLY,
              Instant.parse("2026-04-26T03:00:00Z"));
      ExamMatrixBankMapping negativeRequired =
          buildMapping(
              UUID.fromString("60000000-0000-0000-0000-000000000005"),
              matrixId,
              UUID.fromString("60000000-0000-0000-0000-000000000006"),
              -2,
              BigDecimal.ONE,
              null,
              Instant.parse("2026-04-26T03:01:00Z"));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(zeroRequired, negativeRequired));

      // ===== ACT =====
      questionSelectionService.validateAvailability(matrixId);

      // ===== VERIFY =====
      verify(examMatrixBankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(questionRepository, never())
          .countApprovedByBankAndDifficultyAndCognitiveAndTopic(any(), any(), any(), any());
      verifyNoMoreInteractions(examMatrixBankMappingRepository, questionRepository);
    }

    @Test
    void it_should_throw_insufficient_questions_when_available_count_lower_than_required() {
      // ===== ARRANGE =====
      UUID matrixId = UUID.fromString("60000000-0000-0000-0000-000000000010");
      UUID bankId = UUID.fromString("60000000-0000-0000-0000-000000000011");
      ExamMatrixBankMapping mapping =
          buildMapping(
              UUID.fromString("60000000-0000-0000-0000-000000000012"),
              matrixId,
              bankId,
              3,
              new BigDecimal("1.50"),
              CognitiveLevel.UNDERSTAND,
              Instant.parse("2026-04-26T03:02:00Z"));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(mapping));
      when(questionRepository.countApprovedByBankAndDifficultyAndCognitiveAndTopic(
              bankId, null, CognitiveLevel.UNDERSTAND.name(), null))
          .thenReturn(2L);

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> questionSelectionService.validateAvailability(matrixId));
      assertEquals(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("buildSelectionPlan()")
  class BuildSelectionPlanTests {

    @Test
    void it_should_build_plan_with_order_indexes_points_and_unique_questions_across_mappings() {
      // ===== ARRANGE =====
      UUID matrixId = UUID.fromString("61000000-0000-0000-0000-000000000001");
      UUID assessmentId = UUID.fromString("61000000-0000-0000-0000-000000000002");
      UUID bankA = UUID.fromString("61000000-0000-0000-0000-000000000003");
      UUID bankB = UUID.fromString("61000000-0000-0000-0000-000000000004");
      UUID q1 = UUID.fromString("61000000-0000-0000-0000-000000000101");
      UUID q2 = UUID.fromString("61000000-0000-0000-0000-000000000102");
      UUID q3 = UUID.fromString("61000000-0000-0000-0000-000000000103");

      ExamMatrixBankMapping mappingA =
          buildMapping(
              UUID.fromString("61000000-0000-0000-0000-000000000011"),
              matrixId,
              bankA,
              2,
              new BigDecimal("2.00"),
              CognitiveLevel.APPLY,
              Instant.parse("2026-04-26T03:10:00Z"));
      ExamMatrixBankMapping mappingB =
          buildMapping(
              UUID.fromString("61000000-0000-0000-0000-000000000012"),
              matrixId,
              bankB,
              1,
              null,
              null,
              Instant.parse("2026-04-26T03:11:00Z"));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(mappingA, mappingB));

      when(questionRepository.countApprovedByBankAndDifficultyAndCognitiveAndTopic(
              bankA, null, CognitiveLevel.APPLY.name(), null))
          .thenReturn(5L);
      when(questionRepository.countApprovedByBankAndDifficultyAndCognitiveAndTopic(bankB, null, null, null))
          .thenReturn(3L);

      when(questionRepository.findApprovedIdsByBankAndDifficultyAndCognitiveAndTopic(
              bankA, null, CognitiveLevel.APPLY.name(), null))
          .thenReturn(new java.util.ArrayList<>(List.of(q1, q2, q3)));
      when(questionRepository.findApprovedIdsByBankAndDifficultyAndCognitiveAndTopic(bankB, null, null, null))
          .thenReturn(new java.util.ArrayList<>(List.of(q1, q2, q3)));
      when(questionRepository.findAllById(any()))
          .thenAnswer(
              inv ->
                  ((List<UUID>) inv.getArgument(0)).stream()
                      .map(id -> QuestionSelectionServiceImplTest.this.buildQuestion(id))
                      .toList());

      // ===== ACT =====
      SelectionPlan plan = questionSelectionService.buildSelectionPlan(assessmentId, matrixId, 10);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(plan),
          () -> assertEquals(3, plan.assessmentQuestions().size()),
          () -> assertEquals(10, plan.assessmentQuestions().get(0).getOrderIndex()),
          () -> assertEquals(11, plan.assessmentQuestions().get(1).getOrderIndex()),
          () -> assertEquals(12, plan.assessmentQuestions().get(2).getOrderIndex()),
          () -> assertEquals(4, plan.totalPoints()),
          () -> assertEquals(3, plan.assessmentQuestions().stream().map(aq -> aq.getQuestionId()).distinct().count()));
    }

    @Test
    void it_should_throw_insufficient_when_candidates_after_deduplication_are_less_than_required() {
      // ===== ARRANGE =====
      UUID matrixId = UUID.fromString("62000000-0000-0000-0000-000000000001");
      UUID assessmentId = UUID.fromString("62000000-0000-0000-0000-000000000002");
      UUID bankA = UUID.fromString("62000000-0000-0000-0000-000000000003");
      UUID bankB = UUID.fromString("62000000-0000-0000-0000-000000000004");
      UUID q1 = UUID.fromString("62000000-0000-0000-0000-000000000101");
      UUID q2 = UUID.fromString("62000000-0000-0000-0000-000000000102");

      ExamMatrixBankMapping mappingA =
          buildMapping(
              UUID.fromString("62000000-0000-0000-0000-000000000011"),
              matrixId,
              bankA,
              2,
              BigDecimal.ONE,
              CognitiveLevel.APPLY,
              Instant.parse("2026-04-26T03:20:00Z"));
      ExamMatrixBankMapping mappingB =
          buildMapping(
              UUID.fromString("62000000-0000-0000-0000-000000000012"),
              matrixId,
              bankB,
              1,
              BigDecimal.ONE,
              CognitiveLevel.APPLY,
              Instant.parse("2026-04-26T03:21:00Z"));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(mappingA, mappingB));
      when(questionRepository.countApprovedByBankAndDifficultyAndCognitiveAndTopic(
              any(), any(), any(), any()))
          .thenReturn(5L);
      when(questionRepository.findApprovedIdsByBankAndDifficultyAndCognitiveAndTopic(
              bankA, null, CognitiveLevel.APPLY.name(), null))
          .thenReturn(new java.util.ArrayList<>(List.of(q1, q2)));
      when(questionRepository.findApprovedIdsByBankAndDifficultyAndCognitiveAndTopic(
              bankB, null, CognitiveLevel.APPLY.name(), null))
          .thenReturn(new java.util.ArrayList<>(List.of(q1)));
      when(questionRepository.findAllById(any()))
          .thenAnswer(
              inv ->
                  ((List<UUID>) inv.getArgument(0)).stream()
                      .map(id -> QuestionSelectionServiceImplTest.this.buildQuestion(id))
                      .toList());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> questionSelectionService.buildSelectionPlan(assessmentId, matrixId, 1));
      assertEquals(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE, exception.getErrorCode());
    }

    @Test
    void it_should_throw_question_not_found_when_repository_does_not_return_all_selected_questions() {
      // ===== ARRANGE =====
      UUID matrixId = UUID.fromString("63000000-0000-0000-0000-000000000001");
      UUID assessmentId = UUID.fromString("63000000-0000-0000-0000-000000000002");
      UUID bank = UUID.fromString("63000000-0000-0000-0000-000000000003");
      UUID q1 = UUID.fromString("63000000-0000-0000-0000-000000000101");

      ExamMatrixBankMapping mapping =
          buildMapping(
              UUID.fromString("63000000-0000-0000-0000-000000000011"),
              matrixId,
              bank,
              1,
              BigDecimal.ONE,
              CognitiveLevel.REMEMBER,
              Instant.parse("2026-04-26T03:30:00Z"));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(mapping));
      when(questionRepository.countApprovedByBankAndDifficultyAndCognitiveAndTopic(
              bank, null, CognitiveLevel.REMEMBER.name(), null))
          .thenReturn(1L);
      when(questionRepository.findApprovedIdsByBankAndDifficultyAndCognitiveAndTopic(
              bank, null, CognitiveLevel.REMEMBER.name(), null))
          .thenReturn(new java.util.ArrayList<>(List.of(q1)));
      when(questionRepository.findAllById(List.of(q1))).thenReturn(List.of());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> questionSelectionService.buildSelectionPlan(assessmentId, matrixId, 1));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_return_empty_plan_when_all_mappings_have_null_or_non_positive_question_count() {
      // ===== ARRANGE =====
      UUID matrixId = UUID.fromString("64000000-0000-0000-0000-000000000001");
      UUID assessmentId = UUID.fromString("64000000-0000-0000-0000-000000000002");
      ExamMatrixBankMapping m1 =
          buildMapping(
              UUID.fromString("64000000-0000-0000-0000-000000000011"),
              matrixId,
              UUID.fromString("64000000-0000-0000-0000-000000000012"),
              null,
              BigDecimal.ONE,
              CognitiveLevel.ANALYZE,
              Instant.parse("2026-04-26T03:31:00Z"));
      ExamMatrixBankMapping m2 =
          buildMapping(
              UUID.fromString("64000000-0000-0000-0000-000000000013"),
              matrixId,
              UUID.fromString("64000000-0000-0000-0000-000000000014"),
              -5,
              BigDecimal.ONE,
              CognitiveLevel.ANALYZE,
              Instant.parse("2026-04-26T03:32:00Z"));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(m1, m2));

      // ===== ACT =====
      SelectionPlan plan = questionSelectionService.buildSelectionPlan(assessmentId, matrixId, 30);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(0, plan.assessmentQuestions().size()), () -> assertEquals(0, plan.totalPoints()));

      // ===== VERIFY =====
      verify(questionRepository, never()).findApprovedIdsByBankAndDifficultyAndCognitiveAndTopic(any(), any(), any(), any());
      verify(questionRepository, never()).findAllById(any());
      verifyNoMoreInteractions(questionRepository);
    }
  }
}
