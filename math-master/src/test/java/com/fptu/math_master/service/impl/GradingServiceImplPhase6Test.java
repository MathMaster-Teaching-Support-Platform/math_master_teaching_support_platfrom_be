package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.fptu.math_master.entity.Answer;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.util.ScoringContext;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phase 6 Unit Tests for Enhanced Grading Logic
 * Tests for SHORT_ANSWER validation modes and TRUE_FALSE partial credit
 */
@DisplayName("Phase 6: Enhanced Grading Logic Tests")
class GradingServiceImplPhase6Test {

  private GradingServiceImpl gradingService;

  @BeforeEach
  void setUp() {
    gradingService = new GradingServiceImpl(null, null, null, null, null, null, null, null, null, null);
    ScoringContext.clear();
  }

  @AfterEach
  void tearDown() {
    ScoringContext.clear();
  }

  /**
   * Helper method to invoke private autoGradeAnswer method via reflection
   */
  private boolean invokeAutoGradeAnswer(Answer answer, Question question, BigDecimal effectiveMaxPoints) throws Exception {
    Method method = GradingServiceImpl.class.getDeclaredMethod("autoGradeAnswer", Answer.class, Question.class, BigDecimal.class);
    method.setAccessible(true);
    return (boolean) method.invoke(gradingService, answer, question, effectiveMaxPoints);
  }

  // ==================== SHORT_ANSWER Tests ====================

  @Test
  @DisplayName("SA: EXACT mode - correct answer")
  void testGradeSA_ExactMode_Correct() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("Paris");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("Paris");
    question.setGenerationMetadata(Map.of("answerValidationMode", "EXACT"));

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertTrue(answer.getIsCorrect());
    assertEquals(BigDecimal.ONE, answer.getPointsEarned());
  }

  @Test
  @DisplayName("SA: EXACT mode - case insensitive")
  void testGradeSA_ExactMode_CaseInsensitive() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("paris");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("Paris");
    question.setGenerationMetadata(Map.of("answerValidationMode", "EXACT"));

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertTrue(answer.getIsCorrect());
    assertEquals(BigDecimal.ONE, answer.getPointsEarned());
  }

  @Test
  @DisplayName("SA: EXACT mode - incorrect answer")
  void testGradeSA_ExactMode_Incorrect() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("London");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("Paris");
    question.setGenerationMetadata(Map.of("answerValidationMode", "EXACT"));

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertFalse(answer.getIsCorrect());
    assertEquals(BigDecimal.ZERO, answer.getPointsEarned());
  }

  @Test
  @DisplayName("SA: NUMERIC mode - within tolerance")
  void testGradeSA_NumericMode_WithinTolerance() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("3.15");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("3.14");
    question.setGenerationMetadata(
        Map.of("answerValidationMode", "NUMERIC", "answerTolerance", 0.01));

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertTrue(answer.getIsCorrect());
    assertEquals(BigDecimal.ONE, answer.getPointsEarned());
  }

  @Test
  @DisplayName("SA: NUMERIC mode - outside tolerance")
  void testGradeSA_NumericMode_OutsideTolerance() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("3.20");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("3.14");
    question.setGenerationMetadata(
        Map.of("answerValidationMode", "NUMERIC", "answerTolerance", 0.01));

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertFalse(answer.getIsCorrect());
    assertEquals(BigDecimal.ZERO, answer.getPointsEarned());
  }

  @Test
  @DisplayName("SA: NUMERIC mode - invalid number")
  void testGradeSA_NumericMode_InvalidNumber() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("abc");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("3.14");
    question.setGenerationMetadata(
        Map.of("answerValidationMode", "NUMERIC", "answerTolerance", 0.01));

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertFalse(answer.getIsCorrect());
    assertEquals(BigDecimal.ZERO, answer.getPointsEarned());
  }

  @Test
  @DisplayName("SA: REGEX mode - matches pattern")
  void testGradeSA_RegexMode_Matches() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("123");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("\\d+");  // Any digits
    question.setGenerationMetadata(Map.of("answerValidationMode", "REGEX"));

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertTrue(answer.getIsCorrect());
    assertEquals(BigDecimal.ONE, answer.getPointsEarned());
  }

  @Test
  @DisplayName("SA: REGEX mode - does not match pattern")
  void testGradeSA_RegexMode_NoMatch() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("abc");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("\\d+");  // Any digits
    question.setGenerationMetadata(Map.of("answerValidationMode", "REGEX"));

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertFalse(answer.getIsCorrect());
    assertEquals(BigDecimal.ZERO, answer.getPointsEarned());
  }

  // ==================== TRUE_FALSE Tests ====================

  @Test
  @DisplayName("TF: 4/4 correct - full credit")
  void testGradeTF_4Correct_FullCredit() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerData(Map.of("value", "A,C"));

    Question question = new Question();
    question.setQuestionType(QuestionType.TRUE_FALSE);
    question.setCorrectAnswer("A,C");  // A=true, B=false, C=true, D=false

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertTrue(answer.getIsCorrect());
    assertEquals(BigDecimal.ONE, answer.getPointsEarned());

    // Verify scoring detail
    Map<String, Object> detail = answer.getScoringDetail();
    assertNotNull(detail);
    assertEquals("TRUE_FALSE", detail.get("questionType"));
    assertEquals(4, detail.get("correctCount"));
    assertEquals(4, detail.get("totalClauses"));
    assertEquals("VIET_THPT", detail.get("scoringRule"));
    assertEquals(1.0, detail.get("earnedRatio"));
  }

  @Test
  @DisplayName("TF: 3/4 correct - partial credit (0.25)")
  void testGradeTF_3Correct_PartialCredit() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerData(Map.of("value", "A,C,D"));  // Student got A, C, D correct

    Question question = new Question();
    question.setQuestionType(QuestionType.TRUE_FALSE);
    question.setCorrectAnswer("A,C");  // A=true, B=false, C=true, D=false

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertFalse(answer.getIsCorrect());  // Not fully correct
    assertEquals(BigDecimal.valueOf(0.25), answer.getPointsEarned());

    // Verify scoring detail
    Map<String, Object> detail = answer.getScoringDetail();
    assertNotNull(detail);
    assertEquals("TRUE_FALSE", detail.get("questionType"));
    assertEquals(3, detail.get("correctCount"));
    assertEquals(4, detail.get("totalClauses"));
    assertEquals("VIET_THPT", detail.get("scoringRule"));
    assertEquals(0.25, detail.get("earnedRatio"));
  }

  @Test
  @DisplayName("TF: 2/4 correct - no credit")
  void testGradeTF_2Correct_NoCredit() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerData(Map.of("value", "A,B"));  // Student got A, B

    Question question = new Question();
    question.setQuestionType(QuestionType.TRUE_FALSE);
    question.setCorrectAnswer("A,C");  // A=true, B=false, C=true, D=false

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertFalse(answer.getIsCorrect());
    assertEquals(BigDecimal.ZERO, answer.getPointsEarned());

    // Verify scoring detail
    Map<String, Object> detail = answer.getScoringDetail();
    assertNotNull(detail);
    assertEquals("TRUE_FALSE", detail.get("questionType"));
    assertEquals(2, detail.get("correctCount"));
    assertEquals(4, detail.get("totalClauses"));
    assertEquals("VIET_THPT", detail.get("scoringRule"));
    assertEquals(0.0, detail.get("earnedRatio"));
  }

  @Test
  @DisplayName("TF: 0/4 correct - no credit")
  void testGradeTF_0Correct_NoCredit() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerData(Map.of("value", "B,D"));  // Student got B, D

    Question question = new Question();
    question.setQuestionType(QuestionType.TRUE_FALSE);
    question.setCorrectAnswer("A,C");  // A=true, B=false, C=true, D=false

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertFalse(answer.getIsCorrect());
    assertEquals(BigDecimal.ZERO, answer.getPointsEarned());

    // Verify scoring detail
    Map<String, Object> detail = answer.getScoringDetail();
    assertNotNull(detail);
    assertEquals("TRUE_FALSE", detail.get("questionType"));
    assertEquals(0, detail.get("correctCount"));
    assertEquals(4, detail.get("totalClauses"));
    assertEquals("VIET_THPT", detail.get("scoringRule"));
    assertEquals(0.0, detail.get("earnedRatio"));
  }

  @Test
  @DisplayName("TF: Clause detail verification")
  void testGradeTF_ClauseDetailVerification() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerData(Map.of("value", "A,C,D"));

    Question question = new Question();
    question.setQuestionType(QuestionType.TRUE_FALSE);
    question.setCorrectAnswer("A,C");

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert - Verify clause results
    Map<String, Object> detail = answer.getScoringDetail();
    @SuppressWarnings("unchecked")
    Map<String, Object> clauseResults = (Map<String, Object>) detail.get("clauseResults");

    // A: expected=true, actual=true, correct=true
    @SuppressWarnings("unchecked")
    Map<String, Object> clauseA = (Map<String, Object>) clauseResults.get("A");
    assertTrue((Boolean) clauseA.get("expected"));
    assertTrue((Boolean) clauseA.get("actual"));
    assertTrue((Boolean) clauseA.get("correct"));

    // B: expected=false, actual=false, correct=true
    @SuppressWarnings("unchecked")
    Map<String, Object> clauseB = (Map<String, Object>) clauseResults.get("B");
    assertFalse((Boolean) clauseB.get("expected"));
    assertFalse((Boolean) clauseB.get("actual"));
    assertTrue((Boolean) clauseB.get("correct"));

    // C: expected=true, actual=true, correct=true
    @SuppressWarnings("unchecked")
    Map<String, Object> clauseC = (Map<String, Object>) clauseResults.get("C");
    assertTrue((Boolean) clauseC.get("expected"));
    assertTrue((Boolean) clauseC.get("actual"));
    assertTrue((Boolean) clauseC.get("correct"));

    // D: expected=false, actual=true, correct=false
    @SuppressWarnings("unchecked")
    Map<String, Object> clauseD = (Map<String, Object>) clauseResults.get("D");
    assertFalse((Boolean) clauseD.get("expected"));
    assertTrue((Boolean) clauseD.get("actual"));
    assertFalse((Boolean) clauseD.get("correct"));
  }

  // ==================== Edge Cases ====================

  @Test
  @DisplayName("SA: Empty answer")
  void testGradeSA_EmptyAnswer() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("Paris");

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertFalse(result);  // Should return false for empty answer
  }

  @Test
  @DisplayName("SA: Null answer")
  void testGradeSA_NullAnswer() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText(null);

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("Paris");

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertFalse(result);  // Should return false for null answer
  }

  @Test
  @DisplayName("TF: Empty student answer")
  void testGradeTF_EmptyStudentAnswer() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerData(Map.of("value", ""));

    Question question = new Question();
    question.setQuestionType(QuestionType.TRUE_FALSE);
    question.setCorrectAnswer("A,C");

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertEquals(BigDecimal.ZERO, answer.getPointsEarned());  // 0/4 correct
  }

  @Test
  @DisplayName("SA: Default validation mode (EXACT)")
  void testGradeSA_DefaultValidationMode() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("Paris");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("Paris");
    question.setGenerationMetadata(new HashMap<>());  // No validation mode specified

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertTrue(answer.getIsCorrect());
    assertEquals(BigDecimal.ONE, answer.getPointsEarned());
  }

  @Test
  @DisplayName("SA: Numeric with default tolerance")
  void testGradeSA_NumericWithDefaultTolerance() throws Exception {
    // Arrange
    Answer answer = new Answer();
    answer.setAnswerText("3.1401");

    Question question = new Question();
    question.setQuestionType(QuestionType.SHORT_ANSWER);
    question.setCorrectAnswer("3.14");
    question.setGenerationMetadata(Map.of("answerValidationMode", "NUMERIC"));  // No tolerance

    BigDecimal maxPoints = BigDecimal.ONE;

    // Act
    boolean result = invokeAutoGradeAnswer(answer, question, maxPoints);

    // Assert
    assertTrue(result);
    assertTrue(answer.getIsCorrect());  // Within default tolerance (0.001)
    assertEquals(BigDecimal.ONE, answer.getPointsEarned());
  }
}
