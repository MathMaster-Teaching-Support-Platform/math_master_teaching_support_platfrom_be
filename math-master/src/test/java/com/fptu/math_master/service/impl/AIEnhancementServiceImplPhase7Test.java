package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.service.GeminiService;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Phase 7 Unit Tests for AI Generation
 * Tests for SHORT_ANSWER and TRUE_FALSE question generation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 7: AI Generation Tests")
class AIEnhancementServiceImplPhase7Test {

  @Mock private GeminiService geminiService;
  @Mock private QuestionTemplateRepository questionTemplateRepository;
  @Mock private QuestionRepository questionRepository;

  private AIEnhancementServiceImpl aiService;

  @BeforeEach
  void setUp() {
    aiService = new AIEnhancementServiceImpl(geminiService, questionTemplateRepository, questionRepository);
  }

  // ==================== SHORT_ANSWER Tests ====================

  @Test
  @DisplayName("SA: Generate question with EXACT validation mode")
  void testGenerateSA_ExactMode() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Capital City Question");
    template.setTemplateType(QuestionType.SHORT_ANSWER);
    template.setTemplateText(Map.of("en", "What is the capital of {country}?"));
    template.setAnswerFormula("{capital}");
    template.setParameters(Map.of(
        "country", Map.of("type", "string", "values", List.of("France", "Germany", "Italy")),
        "capital", Map.of("type", "string", "values", List.of("Paris", "Berlin", "Rome"))
    ));

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getQuestionText());
    assertTrue(result.getQuestionText().contains("capital") || result.getQuestionText().contains("?"));
    assertNotNull(result.getCorrectAnswer());
    assertNull(result.getOptions());  // No options for SA
    assertNotNull(result.getGenerationMetadata());
    assertEquals("EXACT", result.getGenerationMetadata().get("answerValidationMode"));
  }

  @Test
  @DisplayName("SA: Generate question with NUMERIC validation mode")
  void testGenerateSA_NumericMode() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Math Calculation");
    template.setTemplateType(QuestionType.SHORT_ANSWER);
    template.setTemplateText(Map.of("en", "Calculate: {num1} + {num2} = ?"));
    template.setAnswerFormula("{num1} + {num2}");
    template.setParameters(Map.of(
        "num1", Map.of("type", "integer", "min", 1, "max", 10),
        "num2", Map.of("type", "integer", "min", 1, "max", 10)
    ));

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getQuestionText());
    assertNotNull(result.getCorrectAnswer());
    assertNull(result.getOptions());
    assertNotNull(result.getGenerationMetadata());
    assertEquals("NUMERIC", result.getGenerationMetadata().get("answerValidationMode"));
    assertEquals(0.01, result.getGenerationMetadata().get("answerTolerance"));
  }

  @Test
  @DisplayName("SA: Generate question with REGEX validation mode")
  void testGenerateSA_RegexMode() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Pattern Matching");
    template.setTemplateType(QuestionType.SHORT_ANSWER);
    template.setTemplateText(Map.of("en", "Write a fraction: {numerator}/{denominator}"));
    template.setAnswerFormula("{numerator}/{denominator}");
    template.setParameters(Map.of(
        "numerator", Map.of("type", "integer", "min", 1, "max", 5),
        "denominator", Map.of("type", "integer", "min", 2, "max", 5)
    ));

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getQuestionText());
    assertNotNull(result.getCorrectAnswer());
    assertNull(result.getOptions());
    assertNotNull(result.getGenerationMetadata());
    assertEquals("EXACT", result.getGenerationMetadata().get("answerValidationMode"));
  }

  @Test
  @DisplayName("SA: Question text contains substituted parameters")
  void testGenerateSA_ParameterSubstitution() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Parameter Test");
    template.setTemplateType(QuestionType.SHORT_ANSWER);
    template.setTemplateText(Map.of("en", "The {adjective} {noun} is {value}"));
    template.setAnswerFormula("{value}");
    template.setParameters(Map.of(
        "adjective", Map.of("type", "string", "values", List.of("quick", "slow")),
        "noun", Map.of("type", "string", "values", List.of("fox", "turtle")),
        "value", Map.of("type", "string", "values", List.of("fast", "slow"))
    ));

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getQuestionText());
    // Should contain substituted values, not placeholders
    assertFalse(result.getQuestionText().contains("{"));
    assertFalse(result.getQuestionText().contains("}"));
  }

  // ==================== TRUE_FALSE Tests ====================

  @Test
  @DisplayName("TF: Generate question with 4 clauses")
  void testGenerateTF_FourClauses() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Function Properties");
    template.setTemplateType(QuestionType.TRUE_FALSE);
    template.setTemplateText(Map.of("en", "Cho hàm số f(x) = {function}. Xét các mệnh đề:"));
    template.setParameters(Map.of(
        "function", Map.of("type", "string", "values", List.of("x^2", "x^3", "sin(x)"))
    ));

    // Create clause templates
    Map<String, Object> statementMutations = new HashMap<>();
    List<Map<String, Object>> clauseTemplates = List.of(
        Map.of(
            "text", "f(x) đồng biến trên (0, +∞)",
            "truthValue", true,
            "chapterId", "ch1",
            "cognitiveLevel", "THONG_HIEU"
        ),
        Map.of(
            "text", "f(x) có cực tiểu tại x = 0",
            "truthValue", false,
            "chapterId", "ch1",
            "cognitiveLevel", "VAN_DUNG"
        ),
        Map.of(
            "text", "f(0) = 0",
            "truthValue", true,
            "chapterId", "ch2",
            "cognitiveLevel", "NHAN_BIET"
        ),
        Map.of(
            "text", "f(x) là hàm chẵn",
            "truthValue", false,
            "chapterId", "ch2",
            "cognitiveLevel", "VAN_DUNG_CAO"
        )
    );
    statementMutations.put("clauseTemplates", clauseTemplates);
    template.setStatementMutations(statementMutations);

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getQuestionText());
    assertNotNull(result.getOptions());
    assertEquals(4, result.getOptions().size());
    assertTrue(result.getOptions().containsKey("A"));
    assertTrue(result.getOptions().containsKey("B"));
    assertTrue(result.getOptions().containsKey("C"));
    assertTrue(result.getOptions().containsKey("D"));
    assertNotNull(result.getCorrectAnswer());
    // Correct answer should be comma-separated true keys
    assertTrue(result.getCorrectAnswer().contains("A") || result.getCorrectAnswer().contains("C"));
  }

  @Test
  @DisplayName("TF: Correct answer contains true clauses")
  void testGenerateTF_CorrectAnswerFormat() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Math Properties");
    template.setTemplateType(QuestionType.TRUE_FALSE);
    template.setTemplateText(Map.of("en", "Xét các mệnh đề sau:"));
    template.setParameters(new HashMap<>());

    Map<String, Object> statementMutations = new HashMap<>();
    List<Map<String, Object>> clauseTemplates = List.of(
        Map.of("text", "Mệnh đề A", "truthValue", true, "chapterId", "ch1", "cognitiveLevel", "NHAN_BIET"),
        Map.of("text", "Mệnh đề B", "truthValue", false, "chapterId", "ch1", "cognitiveLevel", "THONG_HIEU"),
        Map.of("text", "Mệnh đề C", "truthValue", true, "chapterId", "ch2", "cognitiveLevel", "VAN_DUNG"),
        Map.of("text", "Mệnh đề D", "truthValue", false, "chapterId", "ch2", "cognitiveLevel", "VAN_DUNG_CAO")
    );
    statementMutations.put("clauseTemplates", clauseTemplates);
    template.setStatementMutations(statementMutations);

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    String correctAnswer = result.getCorrectAnswer();
    assertNotNull(correctAnswer);
    // Should contain A and C (the true clauses)
    assertTrue(correctAnswer.contains("A"));
    assertTrue(correctAnswer.contains("C"));
    // Should not contain B and D (the false clauses)
    assertFalse(correctAnswer.contains("B"));
    assertFalse(correctAnswer.contains("D"));
  }

  @Test
  @DisplayName("TF: Clause metadata stored correctly")
  void testGenerateTF_ClauseMetadata() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Metadata Test");
    template.setTemplateType(QuestionType.TRUE_FALSE);
    template.setTemplateText(Map.of("en", "Test question"));
    template.setParameters(new HashMap<>());

    Map<String, Object> statementMutations = new HashMap<>();
    List<Map<String, Object>> clauseTemplates = List.of(
        Map.of("text", "Clause A", "truthValue", true, "chapterId", "ch-a", "cognitiveLevel", "NHAN_BIET"),
        Map.of("text", "Clause B", "truthValue", false, "chapterId", "ch-b", "cognitiveLevel", "THONG_HIEU"),
        Map.of("text", "Clause C", "truthValue", true, "chapterId", "ch-c", "cognitiveLevel", "VAN_DUNG"),
        Map.of("text", "Clause D", "truthValue", false, "chapterId", "ch-d", "cognitiveLevel", "VAN_DUNG_CAO")
    );
    statementMutations.put("clauseTemplates", clauseTemplates);
    template.setStatementMutations(statementMutations);

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getGenerationMetadata());
    @SuppressWarnings("unchecked")
    Map<String, Object> tfClauses = (Map<String, Object>) result.getGenerationMetadata().get("tfClauses");
    assertNotNull(tfClauses);
    assertEquals(4, tfClauses.size());

    // Verify each clause has metadata
    for (String key : List.of("A", "B", "C", "D")) {
      @SuppressWarnings("unchecked")
      Map<String, Object> clauseMeta = (Map<String, Object>) tfClauses.get(key);
      assertNotNull(clauseMeta);
      assertNotNull(clauseMeta.get("chapterId"));
      assertNotNull(clauseMeta.get("cognitiveLevel"));
    }
  }

  @Test
  @DisplayName("TF: Clause text contains substituted parameters")
  void testGenerateTF_ClauseParameterSubstitution() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Clause Parameter Test");
    template.setTemplateType(QuestionType.TRUE_FALSE);
    template.setTemplateText(Map.of("en", "Cho hàm số f(x) = {function}"));
    template.setParameters(Map.of(
        "function", Map.of("type", "string", "values", List.of("x^2", "x^3"))
    ));

    Map<String, Object> statementMutations = new HashMap<>();
    List<Map<String, Object>> clauseTemplates = List.of(
        Map.of("text", "f(x) = {function}", "truthValue", true, "chapterId", "ch1", "cognitiveLevel", "NHAN_BIET"),
        Map.of("text", "f'(x) = 2{function}", "truthValue", false, "chapterId", "ch1", "cognitiveLevel", "THONG_HIEU"),
        Map.of("text", "f(0) = 0", "truthValue", true, "chapterId", "ch2", "cognitiveLevel", "VAN_DUNG"),
        Map.of("text", "f(x) > 0 for all x", "truthValue", false, "chapterId", "ch2", "cognitiveLevel", "VAN_DUNG_CAO")
    );
    statementMutations.put("clauseTemplates", clauseTemplates);
    template.setStatementMutations(statementMutations);

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getOptions());
    for (String clauseText : result.getOptions().values()) {
      // Clauses should not contain unsubstituted placeholders
      assertFalse(clauseText.contains("{function}"));
    }
  }

  // ==================== Dispatch Tests ====================

  @Test
  @DisplayName("Dispatch: MCQ template uses MCQ generation")
  void testDispatch_MCQTemplate() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("MCQ Question");
    template.setTemplateType(QuestionType.MULTIPLE_CHOICE);
    template.setTemplateText(Map.of("en", "What is {value}?"));
    template.setAnswerFormula("{answer}");
    template.setParameters(Map.of(
        "value", Map.of("type", "string", "values", List.of("2+2", "3+3")),
        "answer", Map.of("type", "string", "values", List.of("4", "6"))
    ));

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getOptions());
    // MCQ should have options
    assertTrue(result.getOptions().size() > 0);
  }

  @Test
  @DisplayName("Dispatch: SA template uses SA generation")
  void testDispatch_SATemplate() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("SA Question");
    template.setTemplateType(QuestionType.SHORT_ANSWER);
    template.setTemplateText(Map.of("en", "Answer: {value}"));
    template.setAnswerFormula("{answer}");
    template.setParameters(Map.of(
        "value", Map.of("type", "string", "values", List.of("test")),
        "answer", Map.of("type", "string", "values", List.of("result"))
    ));

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNull(result.getOptions());  // SA should have no options
  }

  @Test
  @DisplayName("Dispatch: TF template uses TF generation")
  void testDispatch_TFTemplate() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("TF Question");
    template.setTemplateType(QuestionType.TRUE_FALSE);
    template.setTemplateText(Map.of("en", "Test"));
    template.setParameters(new HashMap<>());

    Map<String, Object> statementMutations = new HashMap<>();
    List<Map<String, Object>> clauseTemplates = List.of(
        Map.of("text", "A", "truthValue", true, "chapterId", "ch1", "cognitiveLevel", "NHAN_BIET"),
        Map.of("text", "B", "truthValue", false, "chapterId", "ch1", "cognitiveLevel", "THONG_HIEU"),
        Map.of("text", "C", "truthValue", true, "chapterId", "ch2", "cognitiveLevel", "VAN_DUNG"),
        Map.of("text", "D", "truthValue", false, "chapterId", "ch2", "cognitiveLevel", "VAN_DUNG_CAO")
    );
    statementMutations.put("clauseTemplates", clauseTemplates);
    template.setStatementMutations(statementMutations);

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getOptions());
    assertEquals(4, result.getOptions().size());  // TF should have 4 clauses
  }

  // ==================== Edge Cases ====================

  @Test
  @DisplayName("SA: Handles missing validation mode (defaults to EXACT)")
  void testGenerateSA_DefaultValidationMode() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Default Mode");
    template.setTemplateType(QuestionType.SHORT_ANSWER);
    template.setTemplateText(Map.of("en", "Question"));
    template.setAnswerFormula("answer");
    template.setParameters(Map.of("answer", Map.of("type", "string", "values", List.of("test"))));

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getGenerationMetadata());
    assertEquals("EXACT", result.getGenerationMetadata().get("answerValidationMode"));
  }

  @Test
  @DisplayName("TF: Handles missing clause templates (generates defaults)")
  void testGenerateTF_DefaultClauses() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Default Clauses");
    template.setTemplateType(QuestionType.TRUE_FALSE);
    template.setTemplateText(Map.of("en", "Test"));
    template.setParameters(new HashMap<>());
    template.setStatementMutations(new HashMap<>());  // No clause templates

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getOptions());
    assertEquals(4, result.getOptions().size());  // Should generate 4 default clauses
  }

  @Test
  @DisplayName("SA: Difficulty is determined correctly")
  void testGenerateSA_DifficultyDetermined() {
    // Arrange
    QuestionTemplate template = new QuestionTemplate();
    template.setName("Difficulty Test");
    template.setTemplateType(QuestionType.SHORT_ANSWER);
    template.setTemplateText(Map.of("en", "Question"));
    template.setAnswerFormula("answer");
    template.setParameters(Map.of("answer", Map.of("type", "string", "values", List.of("test"))));

    // Act
    GeneratedQuestionSample result = aiService.generateQuestion(template, 0);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getCalculatedDifficulty());
    assertTrue(result.getCalculatedDifficulty() == QuestionDifficulty.EASY
        || result.getCalculatedDifficulty() == QuestionDifficulty.MEDIUM
        || result.getCalculatedDifficulty() == QuestionDifficulty.HARD);
  }
}
