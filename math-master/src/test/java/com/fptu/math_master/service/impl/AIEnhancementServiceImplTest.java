package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.entity.CanonicalQuestion;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.service.GeminiService;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("AIEnhancementServiceImpl - Tests")
class AIEnhancementServiceImplTest extends BaseUnitTest {

  @InjectMocks private AIEnhancementServiceImpl aiEnhancementService;

  @Mock private GeminiService geminiService;

  private AIEnhancementRequest baseMcqRequest;

  @BeforeEach
  void setUp() {
    Map<String, String> rawOptions = new LinkedHashMap<>();
    rawOptions.put("A", "1");
    rawOptions.put("B", "2");
    rawOptions.put("C", "3");
    rawOptions.put("D", "4");
    baseMcqRequest =
        AIEnhancementRequest.builder()
            .rawQuestionText("Find the value of x when 2x + 1 equals 5.")
            .questionType(QuestionType.MULTIPLE_CHOICE)
            .correctAnswer("2")
            .rawOptions(rawOptions)
            .difficulty(QuestionDifficulty.MEDIUM)
            .context("Chương phương trình bậc nhất")
            .answerFormula("x + 1")
            .parameters(Map.of("x", 1))
            .build();
  }

  private AIEnhancementRequest buildMcqRequest(String rawQuestion, String correctAnswer) {
    Map<String, String> rawOptions = new LinkedHashMap<>();
    rawOptions.put("A", "1");
    rawOptions.put("B", "2");
    rawOptions.put("C", "3");
    rawOptions.put("D", "4");
    return AIEnhancementRequest.builder()
        .rawQuestionText(rawQuestion)
        .questionType(QuestionType.MULTIPLE_CHOICE)
        .correctAnswer(correctAnswer)
        .rawOptions(rawOptions)
        .difficulty(QuestionDifficulty.MEDIUM)
        .build();
  }

  private String validMcqAiJson(String enhancedQuestion, String correctKey) {
    return "{"
        + "\"enhancedQuestion\":\""
        + enhancedQuestion
        + "\","
        + "\"options\":{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\",\"D\":\"4\"},"
        + "\"correctAnswerKey\":\""
        + correctKey
        + "\","
        + "\"explanation\":\"Subtract 1 from both sides, then divide by 2 to isolate x.\","
        + "\"alternativeSolutions\":[],"
        + "\"distractorExplanations\":{\"A\":\"Forgot to divide\",\"C\":\"Sign error\",\"D\":\"Arithmetic slip\"}"
        + "}";
  }

  private QuestionTemplate buildConstantAnswerTemplate() {
    return QuestionTemplate.builder()
        .name("Bài kiểm tra hằng số - Giá trị biểu thức cố định")
        .templateType(QuestionType.MULTIPLE_CHOICE)
        .answerFormula("7")
        .templateText(
            Map.of(
                "vi",
                "Biểu thức có chứa các chữ số 7 và phép cộng 3 + 4 được mô tả trong đề."))
        .parameters(null)
        .optionsGenerator(null)
        .diagramTemplate(null)
        .build();
  }

  private QuestionTemplate buildUnknownAnswerTemplate() {
    return QuestionTemplate.builder()
        .name("Mẫu lỗi công thức - Không tính được đáp án")
        .templateType(QuestionType.MULTIPLE_CHOICE)
        .answerFormula("")
        .templateText(Map.of("vi", "Đề bài minh họa khi công thức rỗng."))
        .parameters(null)
        .build();
  }

  private CanonicalQuestion buildCanonicalQuestion() {
    CanonicalQuestion cq = new CanonicalQuestion();
    cq.setId(UUID.fromString("018f1234-5678-7abc-8def-123456789abc"));
    cq.setProblemText("Given integers {{a}} and {{b}}, compute their sum.");
    cq.setSolutionSteps("Add {{a}} and {{b}} step by step to obtain the result.");
    cq.setDiagramDefinition(null);
    cq.setProblemType(QuestionType.MULTIPLE_CHOICE);
    return cq;
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = AIEnhancementServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return (T) method.invoke(aiEnhancementService, args);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to invoke private method: " + methodName, ex);
    }
  }

  @Nested
  @DisplayName("private helpers via reflection")
  class PrivateHelperReflectionTests {

    /**
     * Normal case: Escape control characters nằm trong chuỗi JSON.
     *
     * <p>Input:
     * <ul>
     *   <li>json: chứa newline và tab trong value</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả giữ JSON hợp lệ với ký tự escaped</li>
     * </ul>
     */
    @Test
    void it_should_escape_control_characters_when_sanitizing_json() {
      // ===== ARRANGE =====
      String json = "{\"message\":\"line1\nline2\tvalue\"}";

      // ===== ACT =====
      String sanitized = invokePrivate("sanitizeJSON", new Class<?>[] {String.class}, json);

      // ===== ASSERT =====
      assertTrue(sanitized.contains("\\n"));
      assertTrue(sanitized.contains("\\t"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Loại bỏ phần giải thích trong ngoặc của option value.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Giữ lại numeric phần đầu</li>
     * </ul>
     */
    @Test
    void it_should_extract_numeric_prefix_when_cleaning_option_value() {
      // ===== ARRANGE =====
      String raw = "6 (ignoring the constant term)";

      // ===== ACT =====
      String cleaned = invokePrivate("cleanOptionValue", new Class<?>[] {String.class}, raw);

      // ===== ASSERT =====
      assertEquals("6", cleaned);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Chuẩn hóa LaTeX formula sang biểu thức có thể evaluate.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>Thay {@code \\frac}, decimal comma, unicode operator</li>
     * </ul>
     */
    @Test
    void it_should_normalize_latex_and_decimal_comma_when_preparing_formula() {
      // ===== ARRANGE =====
      String latex = "\\frac{1}{2} + 3,5 × 2";

      // ===== ACT =====
      String normalized =
          invokePrivate("normalizeFormulaForEvaluation", new Class<?>[] {String.class}, latex);

      // ===== ASSERT =====
      assertTrue(normalized.contains("((1)/(2))"));
      assertTrue(normalized.contains("3.5"));
      assertTrue(normalized.contains("*"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Build aliases cho key có dạng double braces.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Có cả key gốc và key plain</li>
     * </ul>
     */
    @Test
    void it_should_include_plain_alias_when_parameter_key_uses_double_braces() {
      // ===== ARRANGE =====
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("{{a}}", 5);
      params.put("b", 2);

      // ===== ACT =====
      Map<String, Object> aliases =
          invokePrivate(
              "buildFormulaParameterAliases", new Class<?>[] {Map.class}, params);

      // ===== ASSERT =====
      assertEquals(5, aliases.get("{{a}}"));
      assertEquals(5, aliases.get("a"));
      assertEquals(2, aliases.get("b"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Evaluate biểu thức số học đơn giản bằng fallback parser.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả đúng với phép mũ và ngoặc</li>
     * </ul>
     */
    @Test
    void it_should_evaluate_arithmetic_expression_when_using_simple_formula_evaluator() {
      // ===== ARRANGE =====
      String formula = "(a-b)^2";
      Map<String, Object> params = Map.of("a", 5, "b", 2);

      // ===== ACT =====
      String value =
          invokePrivate("evaluateFormulaSimple", new Class<?>[] {String.class, Map.class}, formula, params);

      // ===== ASSERT =====
      assertEquals("9", value);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Formula có set-notation -> simple evaluator trả ?.
     */
    @Test
    void it_should_return_unknown_when_formula_contains_set_notation() {
      // ===== ARRANGE =====
      String formula = "{1,2,3}";

      // ===== ACT =====
      String value =
          invokePrivate(
              "evaluateFormulaSimple", new Class<?>[] {String.class, Map.class}, formula, Map.of());

      // ===== ASSERT =====
      assertEquals("?", value);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Fill text bằng cả placeholder trực tiếp và expression.
     */
    @Test
    void it_should_render_placeholders_and_expression_when_filling_text() {
      // ===== ARRANGE =====
      String raw = "Compute {{a+b}} then compare with {x}.";
      Map<String, Object> params = Map.of("a", 2, "b", 3, "x", 10);

      // ===== ACT =====
      String rendered = invokePrivate("fillText", new Class<?>[] {String.class, Map.class}, raw, params);

      // ===== ASSERT =====
      assertEquals("Compute 5 then compare with 10.", rendered);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Option không có placeholder thì giữ nguyên.
     */
    @Test
    void it_should_keep_option_value_unchanged_when_no_placeholder_exists() {
      // ===== ARRANGE =====
      String rawOption = "3.14";

      // ===== ACT =====
      String rendered =
          invokePrivate(
              "renderOptionValue", new Class<?>[] {String.class, Map.class}, rawOption, Map.of("a", 1));

      // ===== ASSERT =====
      assertEquals("3.14", rendered);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Placeholder không resolve được thì trả null.
     */
    @Test
    void it_should_return_null_when_placeholder_expression_cannot_be_resolved() {
      // ===== ARRANGE =====
      String token = "unknownSymbol + 1";

      // ===== ACT =====
      Object resolved =
          invokePrivate("resolvePlaceholderValue", new Class<?>[] {String.class, Map.class}, token, Map.of("a", 1));

      // ===== ASSERT =====
      assertEquals(null, resolved);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Repair JSON bị thiếu dấu đóng.
     */
    @Test
    void it_should_append_missing_braces_when_repairing_truncated_json() {
      // ===== ARRANGE =====
      String truncated = "{\"a\":1,\"b\":[2,3";

      // ===== ACT =====
      String repaired = invokePrivate("repairTruncatedJson", new Class<?>[] {String.class}, truncated);

      // ===== ASSERT =====
      assertTrue(repaired.endsWith("]}"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Tìm key theo numeric value với sai số nhỏ.
     */
    @Test
    void it_should_find_key_by_numeric_approximation_when_values_are_equivalent() {
      // ===== ARRANGE =====
      Map<String, String> options = Map.of("A", "2.00", "B", "3.5", "C", "4", "D", "5");

      // ===== ACT =====
      String key = invokePrivate("findKeyByValue", new Class<?>[] {Map.class, String.class}, options, "2");

      // ===== ASSERT =====
      assertEquals("A", key);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Nhận diện biểu thức số học đúng mẫu.
     */
    @Test
    void it_should_detect_arithmetic_expression_when_operators_and_numbers_exist() {
      // ===== ARRANGE =====
      String expression = "sqrt(9)+2";
      String plainText = "mathematics";

      // ===== ACT =====
      boolean looksArithmetic =
          invokePrivate("looksLikeArithmeticExpression", new Class<?>[] {String.class}, expression);
      boolean looksPlain =
          invokePrivate("looksLikeArithmeticExpression", new Class<?>[] {String.class}, plainText);

      // ===== ASSERT =====
      assertTrue(looksArithmetic);
      assertFalse(looksPlain);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Parse integer parameter từ JsonNode hợp lệ.
     */
    @Test
    void it_should_parse_integer_parameter_value_from_json_node() throws Exception {
      // ===== ARRANGE =====
      JsonNode node = mapper.readTree("5");

      // ===== ACT =====
      Object parsed =
          invokePrivate("parseAIParameterValue", new Class<?>[] {JsonNode.class, String.class}, node, "integer");

      // ===== ASSERT =====
      assertEquals(5, parsed);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Giá trị int dạng thập phân không nguyên -> parse null.
     */
    @Test
    void it_should_return_null_when_integer_parameter_is_not_whole_number() throws Exception {
      // ===== ARRANGE =====
      JsonNode node = mapper.readTree("5.5");

      // ===== ACT =====
      Object parsed =
          invokePrivate("parseAIParameterValue", new Class<?>[] {JsonNode.class, String.class}, node, "integer");

      // ===== ASSERT =====
      assertEquals(null, parsed);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Parse dynamic placeholder value với nhiều loại node.
     */
    @Test
    void it_should_parse_dynamic_placeholder_value_for_numeric_and_text_nodes() throws Exception {
      // ===== ARRANGE =====
      JsonNode intNode = mapper.readTree("9");
      JsonNode doubleNode = mapper.readTree("3.25");
      JsonNode textNode = mapper.readTree("\"alpha\"");

      // ===== ACT =====
      Object intVal =
          invokePrivate("parseDynamicPlaceholderValue", new Class<?>[] {JsonNode.class}, intNode);
      Object doubleVal =
          invokePrivate("parseDynamicPlaceholderValue", new Class<?>[] {JsonNode.class}, doubleNode);
      Object textVal =
          invokePrivate("parseDynamicPlaceholderValue", new Class<?>[] {JsonNode.class}, textNode);

      // ===== ASSERT =====
      assertEquals(9, intVal);
      assertEquals(3.25, doubleVal);
      assertEquals("alpha", textVal);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Giá trị tham số AI vi phạm exclude/range nên bị từ chối.
     */
    @Test
    void it_should_reject_ai_parameter_value_when_out_of_allowed_constraints() {
      // ===== ARRANGE =====
      Map<String, Object> def = new LinkedHashMap<>();
      def.put("min", 1);
      def.put("max", 10);
      def.put("exclude", List.of(3, 4));

      // ===== ACT =====
      boolean allowedExcluded =
          invokePrivate(
              "isAIParameterValueAllowed",
              new Class<?>[] {String.class, Object.class, String.class, Map.class},
              "b",
              3,
              "integer",
              def);
      boolean allowedOutRange =
          invokePrivate(
              "isAIParameterValueAllowed",
              new Class<?>[] {String.class, Object.class, String.class, Map.class},
              "b",
              11,
              "integer",
              def);

      // ===== ASSERT =====
      assertFalse(allowedExcluded);
      assertFalse(allowedOutRange);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Nhận diện template có textual options.
     */
    @Test
    void it_should_detect_textual_options_when_option_contains_words() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .templateText(Map.of("vi", "Chọn phát biểu đúng"))
              .optionsGenerator(
                  Map.of(
                      "A", "Luôn đúng",
                      "B", "Đôi khi đúng",
                      "C", "Hiếm khi đúng",
                      "D", "Sai"))
              .build();

      // ===== ACT =====
      boolean textual =
          invokePrivate("templateUsesTextualOptions", new Class<?>[] {QuestionTemplate.class}, template);

      // ===== ASSERT =====
      assertTrue(textual);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Thu thập placeholder names từ options generator.
     */
    @Test
    void it_should_collect_option_placeholder_names_when_double_brace_tokens_exist() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .templateText(Map.of("vi", "Tính giá trị"))
              .optionsGenerator(
                  Map.of(
                      "A", "{{ans}}",
                      "B", "{{ans}}+1",
                      "C", "{{ans}}-1",
                      "D", "{{distractor}}"))
              .build();

      // ===== ACT =====
      java.util.Set<String> names =
          invokePrivate("collectOptionPlaceholderNames", new Class<?>[] {QuestionTemplate.class}, template);

      // ===== ASSERT =====
      assertTrue(names.contains("ans"));
      assertTrue(names.contains("distractor"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Build template options và render placeholder theo params.
     */
    @Test
    void it_should_build_template_options_with_rendered_values() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .templateText(Map.of("vi", "Bài toán"))
              .optionsGenerator(Map.of("A", "{{x}}", "B", "{{x}}+1", "C", "5", "D", "{{x}}-1"))
              .build();

      // ===== ACT =====
      Map<String, String> options =
          invokePrivate(
              "buildTemplateOptions", new Class<?>[] {QuestionTemplate.class, Map.class}, template, Map.of("x", 4));

      // ===== ASSERT =====
      assertEquals("4", options.get("A"));
      // renderOptionValue collapses pure arithmetic after placeholder substitution.
      assertEquals("5", options.get("B"));
      assertEquals("5", options.get("C"));
      assertEquals("3", options.get("D"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Render diagram template bằng token {{a}}, {{b}}.
     */
    @Test
    void it_should_render_diagram_template_when_tokens_match_parameter_keys() {
      // ===== ARRANGE =====
      String template = "Point A({{a}},{{b}})";

      // ===== ACT =====
      String rendered =
          invokePrivate(
              "renderDiagramTemplate", new Class<?>[] {String.class, Map.class}, template, Map.of("a", 2, "b", 3));

      // ===== ASSERT =====
      assertEquals("Point A(2,3)", rendered);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Sửa dangling double braces về single braces.
     */
    @Test
    void it_should_collapse_double_braces_when_repairing_dangling_rendered_text() {
      // ===== ARRANGE =====
      String input = "{{x}} + {{y}}";

      // ===== ACT =====
      String repaired =
          invokePrivate("repairDanglingRenderedDoubleBraces", new Class<?>[] {String.class}, input);

      // ===== ASSERT =====
      assertEquals("{x} + {y}", repaired);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Sửa syntax TikZ coordinates cho addplot.
     */
    @Test
    void it_should_fix_tikz_coordinate_block_when_parentheses_are_used_directly() {
      // ===== ARRANGE =====
      String input = "\\addplot[smooth] coordinates (1,2);";

      // ===== ACT =====
      String fixed =
          invokePrivate("repairTikzCoordinatesSyntax", new Class<?>[] {String.class}, input);

      // ===== ASSERT =====
      assertTrue(fixed.contains("coordinates {"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }
  }

  @Nested
  @DisplayName("enhanceQuestion()")
  class EnhanceQuestionTests {

    /**
     * Normal case: Trả về phản hồi đã tăng cường khi Gemini trả JSON hợp lệ và validation đạt.
     *
     * <p>Input:
     * <ul>
     *   <li>AIEnhancementRequest: MCQ, đáp án đúng trùng khóa B</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>try trong enhanceQuestion → không throw</li>
     *   <li>validateAIOutput → true (nhánh isValid)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@code enhanced} true, {@code isValid} true</li>
     *   <li>{@code geminiService.sendMessage} gọi đúng 1 lần</li>
     * </ul>
     */
    @Test
    void it_should_return_enhanced_response_when_gemini_returns_valid_json_and_validation_passes() {
      // ===== ARRANGE =====
      String json = validMcqAiJson("Find x such that 2x + 1 equals 5 (integer solution).", "B");
      when(geminiService.sendMessage(anyString())).thenReturn(json);

      // ===== ACT =====
      AIEnhancedQuestionResponse result = aiEnhancementService.enhanceQuestion(baseMcqRequest);

      // ===== ASSERT =====
      assertNotNull(result);
      assertTrue(result.isEnhanced());
      assertTrue(result.isValid());
      assertEquals("B", result.getCorrectAnswerKey());
      assertNotNull(result.getEnhancedOptions());
      assertEquals(4, result.getEnhancedOptions().size());

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Vẫn trả về phản hồi nhưng {@code enhanced} false khi validation không đạt.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateAIOutput → false (nhánh {@code !isValid})</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@code enhanced} false, {@code isValid} false, có lỗi validation</li>
     * </ul>
     */
    @Test
    void it_should_return_response_with_enhanced_false_when_validation_fails() {
      // ===== ARRANGE =====
      String json =
          validMcqAiJson("Find x such that 2x + 1 equals 5 (integer solution).", "C");
      when(geminiService.sendMessage(anyString())).thenReturn(json);

      // ===== ACT =====
      AIEnhancedQuestionResponse result = aiEnhancementService.enhanceQuestion(baseMcqRequest);

      // ===== ASSERT =====
      assertNotNull(result);
      assertFalse(result.isEnhanced());
      assertFalse(result.isValid());
      assertFalse(result.getValidationErrors().isEmpty());

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Gemini ném exception → fallback response.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>catch Exception trong enhanceQuestion</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@code enhanced} false, {@code isValid} false, giữ nguyên nội dung gốc</li>
     * </ul>
     */
    @Test
    void it_should_return_fallback_response_when_gemini_send_message_throws() {
      // ===== ARRANGE =====
      when(geminiService.sendMessage(anyString()))
          .thenThrow(new RuntimeException("Gemini API quota exceeded"));

      // ===== ACT =====
      AIEnhancedQuestionResponse result = aiEnhancementService.enhanceQuestion(baseMcqRequest);

      // ===== ASSERT =====
      assertNotNull(result);
      assertFalse(result.isEnhanced());
      assertFalse(result.isValid());
      assertEquals(baseMcqRequest.getRawQuestionText(), result.getEnhancedQuestionText());
      assertTrue(
          result.getValidationErrors().stream()
              .anyMatch(msg -> msg.contains("AI enhancement failed")));

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Nội dung AI không parse được JSON → fallback.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>parseAIResponse throw RuntimeException → catch ngoài enhanceQuestion</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Phản hồi fallback với lỗi chứa chuỗi parse</li>
     * </ul>
     */
    @Test
    void it_should_return_fallback_response_when_ai_json_cannot_be_parsed() {
      // ===== ARRANGE =====
      when(geminiService.sendMessage(anyString())).thenReturn("not-json-at-all");

      // ===== ACT =====
      AIEnhancedQuestionResponse result = aiEnhancementService.enhanceQuestion(baseMcqRequest);

      // ===== ASSERT =====
      assertNotNull(result);
      assertFalse(result.isValid());
      assertTrue(
          result.getValidationErrors().stream()
              .anyMatch(msg -> msg.contains("AI enhancement failed")));

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: JSON nằm trong khối markdown được trích và xử lý thành công.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>extractJSON → nhánh tìm thấy code fence</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Parse thành công và validation đạt</li>
     * </ul>
     */
    @Test
    void it_should_parse_json_from_markdown_code_block_when_gemini_wraps_output() {
      // ===== ARRANGE =====
      String inner = validMcqAiJson("Solve for x in 2x + 1 = 5 using integer arithmetic.", "B");
      String wrapped = "```json\n" + inner + "\n```";
      when(geminiService.sendMessage(anyString())).thenReturn(wrapped);

      // ===== ACT =====
      AIEnhancedQuestionResponse result = aiEnhancementService.enhanceQuestion(baseMcqRequest);

      // ===== ASSERT =====
      assertTrue(result.isEnhanced());
      assertTrue(result.isValid());

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }
  }

  @Nested
  @DisplayName("validateAIOutput()")
  class ValidateAiOutputTests {

    /**
     * Normal case: MCQ hợp lệ, đáp án khớp, nội dung toán.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>questionType == MULTIPLE_CHOICE và đủ 4 lựa chọn A–D</li>
     *   <li>isMathematicsContent → true</li>
     * </ul>
     */
    @Test
    void it_should_return_true_when_mcq_output_matches_correct_answer_and_content_is_math() {
      // ===== ARRANGE =====
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Calculate the sum of 3 and 4 using addition.")
              .enhancedOptions(
                  Map.of("A", "1", "B", "2", "C", "7", "D", "9"))
              .correctAnswerKey("C")
              .explanation("Add the two numbers.")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Starter text with digit 1.", "7");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertTrue(valid);
      assertTrue(response.getValidationErrors().isEmpty());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: AI đổi đáp án đúng (khóa trỏ giá trị sai).
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@code false} và có lỗi về đáp án</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_ai_changes_correct_answer_for_mcq() {
      // ===== ARRANGE =====
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Find the difference between 10 and 3.")
              .enhancedOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswerKey("D")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Find x.", "2");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertFalse(valid);
      assertTrue(
          response.getValidationErrors().stream()
              .anyMatch(e -> e.contains("correct answer")));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: MCQ thiếu lựa chọn.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>enhancedOptions null hoặc size khác 4</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_mcq_options_count_is_not_four() {
      // ===== ARRANGE =====
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Compute 2 + 2 which equals 4.")
              .enhancedOptions(Map.of("A", "4", "B", "5"))
              .correctAnswerKey("A")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Compute.", "4");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertFalse(valid);
      assertTrue(
          response.getValidationErrors().stream()
              .anyMatch(e -> e.contains("exactly 4 options")));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: MCQ có {@code enhancedOptions} bằng {@code null}.
     *
     * <p>Input:
     * <ul>
     *   <li>questionType: MULTIPLE_CHOICE</li>
     *   <li>enhancedOptions: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>{@code enhancedOptions == null || size != 4} → nhánh {@code enhancedOptions == null}</li>
     *   <li>ternary log count {@code enhancedOptions == null ? 0 : size} → nhánh {@code 0}</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về {@code false} và chứa lỗi số lượng option</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_mcq_options_are_null() {
      // ===== ARRANGE =====
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Compute 2 + 2 then choose the correct result.")
              .enhancedOptions(null)
              .correctAnswerKey("A")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Compute.", "4");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertFalse(valid);
      assertTrue(
          response.getValidationErrors().stream()
              .anyMatch(e -> e.contains("exactly 4 options")));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: MCQ có đủ 4 phần tử nhưng khóa không phải A–D.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>keySet không equals tập A,B,C,D</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_mcq_option_keys_are_not_abcd() {
      // ===== ARRANGE =====
      Map<String, String> weird = new LinkedHashMap<>();
      weird.put("W", "1");
      weird.put("X", "2");
      weird.put("Y", "3");
      weird.put("Z", "4");
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Solve equation 5 = 5 for consistency check.")
              .enhancedOptions(weird)
              .correctAnswerKey("X")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Solve.", "2");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertFalse(valid);
      assertTrue(
          response.getValidationErrors().stream()
              .anyMatch(e -> e.contains("labeled A, B, C, D")));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Không phải MCQ thì không kiểm tra đủ 4 phương án.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>questionType != MULTIPLE_CHOICE → bỏ qua khối kiểm tra MCQ</li>
     * </ul>
     */
    @Test
    void it_should_skip_mcq_option_count_check_when_question_type_is_short_answer() {
      // ===== ARRANGE =====
      AIEnhancementRequest request =
          AIEnhancementRequest.builder()
              .rawQuestionText("Express the value of pi to two decimals: 3.14.")
              .questionType(QuestionType.SHORT_ANSWER)
              .correctAnswer("3.14")
              .rawOptions(null)
              .build();
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("The number 3.14 approximates pi in many exercises.")
              .enhancedOptions(Map.of("only", "3.14"))
              .correctAnswerKey("only")
              .build();

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertTrue(valid);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Nội dung câu hỏi rỗng sau khi tăng cường.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>enhancedQuestionText null hoặc blank</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_enhanced_question_text_is_empty() {
      // ===== ARRANGE =====
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("   ")
              .enhancedOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswerKey("B")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Placeholder.", "2");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertFalse(valid);
      assertTrue(
          response.getValidationErrors().stream()
              .anyMatch(e -> e.contains("Enhanced question text is empty")));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Nội dung câu hỏi tăng cường bị {@code null}.
     *
     * <p>Input:
     * <ul>
     *   <li>enhancedQuestionText: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>{@code enhancedQuestionText == null || trim().isEmpty()} → nhánh {@code == null}</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về {@code false} và có lỗi "Enhanced question text is empty"</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_enhanced_question_text_is_null() {
      // ===== ARRANGE =====
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText(null)
              .enhancedOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswerKey("B")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Placeholder.", "2");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertFalse(valid);
      assertTrue(
          response.getValidationErrors().stream()
              .anyMatch(e -> e.contains("Enhanced question text is empty")));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Nội dung có từ khóa môn lịch sử → không coi là toán.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isMathematicsContent → false (noOtherSubjects)</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_content_mentions_history_topic() {
      // ===== ARRANGE =====
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText(
                  "Discuss the history of ancient numbering systems without digits.")
              .enhancedOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswerKey("B")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Discuss.", "2");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertFalse(valid);
      assertTrue(
          response.getValidationErrors().stream()
              .anyMatch(e -> e.contains("non-mathematics")));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Phát hiện từ khóa không phù hợp (violence).
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>containsInappropriateContent → true</li>
     * </ul>
     */
    @Test
    void it_should_return_false_when_content_contains_inappropriate_keyword() {
      // ===== ARRANGE =====
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText(
                  "A word problem mentions violence in an unrelated story with number 5.")
              .enhancedOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswerKey("B")
              .build();
      AIEnhancementRequest request = buildMcqRequest("Word problem.", "2");

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertFalse(valid);
      assertTrue(
          response.getValidationErrors().stream()
              .anyMatch(e -> e.contains("inappropriate")));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Dùng kết quả tính từ {@code answerFormula} khi tham số hợp lệ.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>answerFormula và parameters khác null → evaluate thành công</li>
     *   <li>calculatedCorrectAnswer != null → dùng cho isSameAnswer</li>
     * </ul>
     */
    @Test
    void it_should_use_formula_calculated_answer_when_answer_formula_and_parameters_are_valid() {
      // ===== ARRANGE =====
      AIEnhancementRequest request =
          AIEnhancementRequest.builder()
              .rawQuestionText("Evaluate linear expression with coefficient 2.")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .correctAnswer("99")
              .rawOptions(Map.of("A", "3", "B", "5", "C", "7", "D", "9"))
              .answerFormula("a + b")
              .parameters(Map.of("a", 2, "b", 3))
              .build();
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Calculate the sum of parameters 2 and 3 to get 5.")
              .enhancedOptions(Map.of("A", "3", "B", "5", "C", "7", "D", "9"))
              .correctAnswerKey("B")
              .build();

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertTrue(valid);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Có {@code answerFormula} nhưng {@code parameters} là {@code null}.
     *
     * <p>Input:
     * <ul>
     *   <li>answerFormula: "a + b"</li>
     *   <li>parameters: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>{@code answerFormula != null && parameters != null} → nhánh FALSE do parameters null</li>
     *   <li>Dùng {@code request.correctAnswer} thay vì giá trị tính từ formula</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Vẫn validate thành công nếu đáp án đúng theo dữ liệu gốc</li>
     * </ul>
     */
    @Test
    void it_should_use_request_correct_answer_when_formula_exists_but_parameters_are_null() {
      // ===== ARRANGE =====
      AIEnhancementRequest request =
          AIEnhancementRequest.builder()
              .rawQuestionText("Select the number equal to two.")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .correctAnswer("2")
              .rawOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .answerFormula("a + b")
              .parameters(null)
              .build();
      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Choose the value 2 from the options.")
              .enhancedOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswerKey("B")
              .build();

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertTrue(valid);
      assertTrue(response.getValidationErrors().isEmpty());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }
  }

  @Nested
  @DisplayName("testEnhancement()")
  class TestEnhancementTests {

    /**
     * Normal case: {@code testEnhancement} ủy quyền cho {@code enhanceQuestion}.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Cùng luồng gọi Gemini một lần</li>
     * </ul>
     */
    @Test
    void it_should_delegate_to_enhance_question_when_test_enhancement_is_called() {
      // ===== ARRANGE =====
      String json = validMcqAiJson("Find roots of simple linear equation 2x+1=5.", "B");
      when(geminiService.sendMessage(anyString())).thenReturn(json);

      // ===== ACT =====
      AIEnhancedQuestionResponse result =
          aiEnhancementService.testEnhancement(baseMcqRequest);

      // ===== ASSERT =====
      assertTrue(result.isEnhanced());

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }
  }

  @Nested
  @DisplayName("generateQuestion()")
  class GenerateQuestionTests {

    /**
     * Normal case: Công thức cho kết quả không xác định → trả mẫu lỗi, không gọi Gemini.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>{@code "?".equals(correctAnswerStr)} → return sớm</li>
     * </ul>
     */
    @Test
    void it_should_return_formula_error_sample_without_calling_gemini_when_evaluated_answer_is_unknown() {
      // ===== ARRANGE =====
      QuestionTemplate template = buildUnknownAnswerTemplate();

      // ===== ACT =====
      GeneratedQuestionSample result = aiEnhancementService.generateQuestion(template, 0);

      // ===== ASSERT =====
      assertNotNull(result);
      assertTrue(result.getQuestionText().contains("Cannot evaluate formula"));
      assertEquals("Unable to generate", result.getOptions().get("A"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Gemini trả JSON hợp lệ cho bài có đáp án cố định.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Đủ 4 phương án, khóa đáp án khớp giá trị 7</li>
     * </ul>
     */
    @Test
    void it_should_return_generated_sample_when_gemini_returns_valid_json() {
      // ===== ARRANGE =====
      QuestionTemplate template = buildConstantAnswerTemplate();
      String aiJson =
          "{\"questionText\":\"Tính tổng 3 + 4 và so sánh với số 7.\","
              + "\"options\":{\"A\":\"5\",\"B\":\"6\",\"C\":\"7\",\"D\":\"8\"},"
              + "\"correctAnswer\":\"C\","
              + "\"explanation\":\"Cộng 3 và 4 được 7 theo phép cộng số nguyên.\","
              + "\"difficulty\":\"MEDIUM\","
              + "\"usedParameters\":{},"
              + "\"answerCalculation\":\"7 = 7\"}";
      when(geminiService.sendMessage(anyString())).thenReturn(aiJson);

      // ===== ACT =====
      GeneratedQuestionSample result = aiEnhancementService.generateQuestion(template, 1);

      // ===== ASSERT =====
      assertNotNull(result);
      assertNotNull(result.getQuestionText());
      assertEquals(4, result.getOptions().size());
      assertEquals("C", result.getCorrectAnswer());
      assertEquals("7", result.getOptions().get("C"));

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Gemini lỗi khi sinh câu → fallback chỉ dùng Java.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>catch trong generateQuestion sau sendMessage</li>
     * </ul>
     */
    @Test
    void it_should_return_java_fallback_when_gemini_throws_during_generation() {
      // ===== ARRANGE =====
      QuestionTemplate template = buildConstantAnswerTemplate();
      when(geminiService.sendMessage(anyString()))
          .thenThrow(new RuntimeException("Network timeout contacting Gemini"));

      // ===== ACT =====
      GeneratedQuestionSample result = aiEnhancementService.generateQuestion(template, 2);

      // ===== ASSERT =====
      assertNotNull(result);
      assertNotNull(result.getOptions());
      assertEquals(4, result.getOptions().size());
      assertNotNull(result.getCorrectAnswer());
      assertTrue(result.getExplanation().contains("Áp dụng công thức"));

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }
  }

  @Nested
  @DisplayName("generateQuestionFromCanonical()")
  class GenerateQuestionFromCanonicalTests {

    /**
     * Normal case: {@code canonicalQuestion == null} ủy quyền {@code generateQuestion}.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>canonical null → gọi generateQuestion</li>
     * </ul>
     */
    @Test
    void it_should_delegate_to_generate_question_when_canonical_question_is_null() {
      // ===== ARRANGE =====
      QuestionTemplate template = buildUnknownAnswerTemplate();

      // ===== ACT =====
      GeneratedQuestionSample result =
          aiEnhancementService.generateQuestionFromCanonical(null, template, 0);

      // ===== ASSERT =====
      assertNotNull(result);
      assertTrue(result.getQuestionText().contains("Cannot evaluate formula"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: Sinh câu từ canonical với JSON hợp lệ.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Có nội dung câu hỏi và đáp án khóa MCQ</li>
     * </ul>
     */
    @Test
    void it_should_return_parsed_sample_when_canonical_generation_succeeds() {
      // ===== ARRANGE =====
      CanonicalQuestion canonical = buildCanonicalQuestion();
      Map<String, Object> paramA =
          Map.of("type", "integer", "min", 2, "max", 2);
      Map<String, Object> paramB =
          Map.of("type", "integer", "min", 3, "max", 3);
      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu từ câu chuẩn - Tổng hai số nguyên")
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .answerFormula("a + b")
              .templateText(Map.of("vi", "Tính {{a}} + {{b}} bằng bao nhiêu?"))
              .parameters(Map.of("a", paramA, "b", paramB))
              .optionsGenerator(null)
              .diagramTemplate(null)
              .build();

      String aiJson =
          "{\"questionText\":\"Cho hai số {{a}} và {{b}}, hãy tính tổng (phép +).\","
              + "\"options\":{\"A\":\"4\",\"B\":\"5\",\"C\":\"6\",\"D\":\"7\"},"
              + "\"correctAnswer\":\"B\","
              + "\"explanation\":\"Cộng {{a}} và {{b}} theo quy tắc số học.\","
              + "\"solutionSteps\":\"Bước 1: cộng hai số.\","
              + "\"difficulty\":\"EASY\","
              + "\"usedParameters\":{},"
              + "\"answerCalculation\":\"a+b\"}";
      when(geminiService.sendMessage(anyString())).thenReturn(aiJson);

      // ===== ACT =====
      GeneratedQuestionSample result =
          aiEnhancementService.generateQuestionFromCanonical(canonical, template, 0);

      // ===== ASSERT =====
      assertNotNull(result);
      assertTrue(result.getQuestionText().contains("+"));
      assertEquals(4, result.getOptions().size());
      assertNotNull(result.getCorrectAnswer());

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: Gemini lỗi khi sinh từ canonical → fallback có bốn lựa chọn mặc định.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>catch trong generateQuestionFromCanonical</li>
     * </ul>
     */
    @Test
    void it_should_return_fallback_sample_when_gemini_throws_during_canonical_generation() {
      // ===== ARRANGE =====
      CanonicalQuestion canonical = buildCanonicalQuestion();
      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu fallback canonical")
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .answerFormula("a + b")
              .templateText(Map.of("vi", "Tính {{a}} + {{b}}?"))
              .parameters(
                  Map.of(
                      "a",
                      Map.of("type", "integer", "min", 1, "max", 1),
                      "b",
                      Map.of("type", "integer", "min", 2, "max", 2)))
              .optionsGenerator(null)
              .diagramTemplate(null)
              .build();
      when(geminiService.sendMessage(anyString()))
          .thenThrow(new IllegalStateException("Streaming response interrupted"));

      // ===== ACT =====
      GeneratedQuestionSample result =
          aiEnhancementService.generateQuestionFromCanonical(canonical, template, 1);

      // ===== ASSERT =====
      assertNotNull(result);
      assertNotNull(result.getOptions());
      assertEquals(4, result.getOptions().size());
      assertEquals("A", result.getCorrectAnswer());
      assertEquals("Lua chon A", result.getOptions().get("A"));

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }
  }

  @Nested
  @DisplayName("advanced branch coverage")
  class AdvancedBranchCoverageTests {

    @Test
    void it_should_cover_determine_difficulty_with_null_and_rules() {
      // ===== ARRANGE =====
      Map<String, Object> params = Map.of("a", 5);
      Map<String, Object> rules = Map.of("easy", "a > 0", "hard", "a < 0");

      // ===== ACT =====
      QuestionDifficulty fromNull =
          invokePrivate("determineDifficulty", new Class<?>[] {Map.class, Map.class}, null, params);
      QuestionDifficulty fromRules =
          invokePrivate("determineDifficulty", new Class<?>[] {Map.class, Map.class}, rules, params);

      // ===== ASSERT =====
      assertEquals(QuestionDifficulty.MEDIUM, fromNull);
      assertTrue(fromRules == QuestionDifficulty.EASY || fromRules == QuestionDifficulty.MEDIUM);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_validate_ai_output_formula_evaluation_exception_branch() {
      // ===== ARRANGE =====
      Map<String, Object> explodingParams =
          new LinkedHashMap<>() {
            @Override
            public java.util.Set<Map.Entry<String, Object>> entrySet() {
              throw new RuntimeException("entrySet exploded");
            }
          };

      AIEnhancementRequest request =
          AIEnhancementRequest.builder()
              .rawQuestionText("Compute value for x in equation x+1=2.")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .correctAnswer("1")
              .rawOptions(Map.of("A", "0", "B", "1", "C", "2", "D", "3"))
              .answerFormula("a+b")
              .parameters(explodingParams)
              .build();

      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Find x where x + 1 equals 2.")
              .enhancedOptions(Map.of("A", "0", "B", "1", "C", "2", "D", "3"))
              .correctAnswerKey("B")
              .build();

      // ===== ACT =====
      boolean valid = aiEnhancementService.validateAIOutput(request, response);

      // ===== ASSERT =====
      assertTrue(valid);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_all_special_cases_in_sanitize_json() {
      // ===== ARRANGE =====
      String jsonWithControls = "{\"v\":\"a\rb\bc\fd\u0001x\\\\q\"}";

      // ===== ACT =====
      String nullSanitized = invokePrivate("sanitizeJSON", new Class<?>[] {String.class}, (Object) null);
      String sanitized = invokePrivate("sanitizeJSON", new Class<?>[] {String.class}, jsonWithControls);

      // ===== ASSERT =====
      assertEquals(null, nullSanitized);
      assertTrue(sanitized.contains("\\r"));
      assertTrue(sanitized.contains("\\b"));
      assertTrue(sanitized.contains("\\f"));
      assertTrue(sanitized.contains("\\u0001"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_generated_question_success_and_catch() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .answerFormula("a+b")
              .templateText(Map.of("vi", "Tính {{a}} + {{b}}"))
              .parameters(
                  Map.of(
                      "a", Map.of("type", "integer", "min", 1, "max", 5),
                      "b", Map.of("type", "integer", "min", 1, "max", 5)))
              .optionsGenerator(Map.of("A", "2", "B", "3"))
              .build();
      Map<String, Object> params = Map.of("a", 1, "b", 2);
      String okJson =
          "{\"questionText\":\"Tổng của {{a}} và {{b}} là bao nhiêu?\","
              + "\"options\":{\"A\":\"1\",\"B\":\"3\"},"
              + "\"correctAnswer\":\"B\","
              + "\"explanation\":\"Cộng hai số nguyên.\","
              + "\"solutionSteps\":\"B1 cộng trực tiếp.\","
              + "\"difficulty\":\"EASY\","
              + "\"usedParameters\":{\"a\":1,\"b\":2},"
              + "\"answerCalculation\":\"a+b\"}";

      // ===== ACT =====
      GeneratedQuestionSample ok =
          invokePrivate(
              "parseCanonicalGeneratedQuestion",
              new Class<?>[] {
                String.class,
                QuestionTemplate.class,
                Map.class,
                QuestionDifficulty.class,
                String.class,
                String.class,
                String.class
              },
              okJson,
              template,
              params,
              QuestionDifficulty.MEDIUM,
              "fallback question",
              "fallback explanation",
              "diagram");

      GeneratedQuestionSample fallback =
          invokePrivate(
              "parseCanonicalGeneratedQuestion",
              new Class<?>[] {
                String.class,
                QuestionTemplate.class,
                Map.class,
                QuestionDifficulty.class,
                String.class,
                String.class,
                String.class
              },
              "not-json",
              template,
              params,
              QuestionDifficulty.HARD,
              "fallback question",
              "fallback explanation",
              "diagram");

      // ===== ASSERT =====
      assertNotNull(ok.getOptions().get("A"));
      assertNotNull(ok.getOptions().get("B"));
      assertNotNull(ok.getOptions().get("C"));
      assertNotNull(ok.getOptions().get("D"));
      assertEquals("B", ok.getCorrectAnswer());
      assertEquals("A", fallback.getCorrectAnswer());
      assertEquals("fallback question", fallback.getQuestionText());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_ensure_canonical_four_option_keys_for_missing_and_extra_keys() {
      // ===== ARRANGE =====
      Map<String, String> options = new LinkedHashMap<>();
      options.put("A", "1");
      options.put("E", "5");

      // ===== ACT =====
      invokePrivate("ensureCanonicalFourOptionKeys", new Class<?>[] {Map.class}, options);

      // ===== ASSERT =====
      assertEquals(4, options.size());
      assertTrue(options.containsKey("A"));
      assertTrue(options.containsKey("B"));
      assertTrue(options.containsKey("C"));
      assertTrue(options.containsKey("D"));
      assertFalse(options.containsKey("E"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_evaluate_formula_all_fallback_paths() {
      // ===== ARRANGE =====
      Map<String, Object> params = Map.of("a", 3, "b", 4);

      // ===== ACT =====
      String direct =
          invokePrivate("evaluateFormula", new Class<?>[] {String.class, Map.class}, "a+b", params);
      String symbolic =
          invokePrivate(
              "evaluateFormula", new Class<?>[] {String.class, Map.class}, "x + a", params);
      String literal =
          invokePrivate(
              "evaluateFormula", new Class<?>[] {String.class, Map.class}, "\"final answer text\"", params);
      String unknown =
          invokePrivate(
              "evaluateFormula", new Class<?>[] {String.class, Map.class}, "{{bad}}", params);

      // ===== ASSERT =====
      assertEquals("7", direct);
      assertTrue(symbolic.contains("x"));
      assertEquals("final answer text", literal);
      assertEquals("bad", unknown);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_build_symbolic_formula_answer_and_literal_resolver_branches() {
      // ===== ARRANGE =====
      Map<String, Object> params = Map.of("a", 2);

      // ===== ACT =====
      String replaced =
          invokePrivate(
              "buildSymbolicFormulaAnswer", new Class<?>[] {String.class, Map.class}, "a + x", params);
      String noReplacement =
          invokePrivate(
              "buildSymbolicFormulaAnswer", new Class<?>[] {String.class, Map.class}, "x + y", params);

      String literalOk =
          invokePrivate(
              "resolveLiteralFormulaAnswer", new Class<?>[] {String.class, Map.class}, "\"Done\"", params);
      String literalArithmetic =
          invokePrivate(
              "resolveLiteralFormulaAnswer", new Class<?>[] {String.class, Map.class}, "2+3", params);
      String literalPlaceholder =
          invokePrivate(
              "resolveLiteralFormulaAnswer", new Class<?>[] {String.class, Map.class}, "{{x}}", params);

      // ===== ASSERT =====
      assertEquals("2 + x", replaced);
      assertEquals("?", noReplacement);
      assertEquals("Done", literalOk);
      assertEquals("?", literalArithmetic);
      assertEquals("x", literalPlaceholder);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_return_unknown_when_build_symbolic_formula_answer_input_is_null_or_blank() {
      // ===== ACT =====
      String fromNull =
          invokePrivate(
              "buildSymbolicFormulaAnswer", new Class<?>[] {String.class, Map.class}, null, Map.of("a", 1));
      String fromBlank =
          invokePrivate(
              "buildSymbolicFormulaAnswer", new Class<?>[] {String.class, Map.class}, "   ", Map.of("a", 1));
      String fromNullValueParam =
          invokePrivate(
              "buildSymbolicFormulaAnswer",
              new Class<?>[] {String.class, Map.class},
              "a + 1",
              new LinkedHashMap<String, Object>() {
                {
                  put("a", null);
                }
              });
      String fromWhitespaceReplacement =
          invokePrivate(
              "buildSymbolicFormulaAnswer",
              new Class<?>[] {String.class, Map.class},
              "a",
              new LinkedHashMap<String, Object>() {
                {
                  put("a", " ");
                }
              });

      // ===== ASSERT =====
      assertEquals("?", fromNull);
      assertEquals("?", fromBlank);
      assertEquals("0 + 1", fromNullValueParam);
      assertEquals("?", fromWhitespaceReplacement);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_evaluate_known_function_all_switch_cases_and_default() {
      // ===== ARRANGE =====
      Method evalMethod;
      try {
        evalMethod =
            AIEnhancementServiceImpl.class.getDeclaredMethod("evaluateKnownFunction", String.class, List.class);
        evalMethod.setAccessible(true);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }

      // ===== ACT =====
      try {
        assertEquals(3.0, (double) evalMethod.invoke(aiEnhancementService, "sqrt", List.of(9.0)));
        assertEquals(2.0, (double) evalMethod.invoke(aiEnhancementService, "abs", List.of(-2.0)));
        assertEquals(8.0, (double) evalMethod.invoke(aiEnhancementService, "pow", List.of(2.0, 3.0)));
        assertEquals(7.0, (double) evalMethod.invoke(aiEnhancementService, "max", List.of(7.0, 5.0)));
        assertEquals(5.0, (double) evalMethod.invoke(aiEnhancementService, "min", List.of(7.0, 5.0)));
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }

      boolean thrown = false;
      try {
        evalMethod.invoke(aiEnhancementService, "unknown", List.of(1.0));
      } catch (Exception ex) {
        thrown = true;
      }

      // ===== ASSERT =====
      assertTrue(thrown);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_looks_like_arithmetic_expression_null_blank_and_function_patterns() {
      // ===== ACT =====
      boolean nullCase =
          invokePrivate("looksLikeArithmeticExpression", new Class<?>[] {String.class}, (Object) null);
      boolean blankCase =
          invokePrivate("looksLikeArithmeticExpression", new Class<?>[] {String.class}, "   ");
      boolean digitsAndOpsCase =
          invokePrivate("looksLikeArithmeticExpression", new Class<?>[] {String.class}, "2*(3+4)-5");
      boolean functionCase =
          invokePrivate(
              "looksLikeArithmeticExpression", new Class<?>[] {String.class}, "sqrt(9) + 1");
      boolean textCase =
          invokePrivate("looksLikeArithmeticExpression", new Class<?>[] {String.class}, "final answer text");
      boolean opNoDigitsCase =
          invokePrivate("looksLikeArithmeticExpression", new Class<?>[] {String.class}, "x+y");
      boolean digitsNoOpsCase =
          invokePrivate("looksLikeArithmeticExpression", new Class<?>[] {String.class}, "12345");

      // ===== ASSERT =====
      assertFalse(nullCase);
      assertFalse(blankCase);
      assertTrue(digitsAndOpsCase);
      assertTrue(functionCase);
      assertFalse(textCase);
      assertFalse(opNoDigitsCase);
      assertTrue(digitsNoOpsCase);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_generated_question_option_branches_and_catch() {
      // ===== ARRANGE =====
      QuestionTemplate dynamicTemplate =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .answerFormula("a+b")
              .templateText(Map.of("vi", "Tính {{a}} + {{b}}"))
              .parameters(
                  Map.of(
                      "a", Map.of("type", "integer", "min", 1, "max", 9),
                      "b", Map.of("type", "integer", "min", 1, "max", 9),
                      "optA", Map.of("type", "integer", "min", 1, "max", 9)))
              .optionsGenerator(Map.of("A", "{{optA}}", "B", "2", "C", "3", "D", "4"))
              .build();
      Map<String, Object> params = Map.of("a", 1, "b", 1, "optA", 9);
      String optionPlaceholderJson =
          "{\"questionText\":\"Q {{a}}\","
              + "\"options\":{},"
              + "\"correctAnswer\":\"B\","
              + "\"explanation\":\"Giải thích\","
              + "\"difficulty\":\"MEDIUM\","
              + "\"usedParameters\":{\"a\":1,\"b\":1,\"optA\":9}}";

      QuestionTemplate fixedTemplate =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .answerFormula("5")
              .templateText(Map.of("vi", "Question static"))
              .parameters(Map.of("a", Map.of("type", "integer", "min", 1, "max", 1)))
              .optionsGenerator(Map.of("A", "10", "B", "11", "C", "12", "D", "13"))
              .build();
      String noCorrectAnswerJson =
          "{\"questionText\":\"Static\","
              + "\"options\":{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\",\"D\":\"4\"},"
              + "\"correctAnswer\":\"A\","
              + "\"explanation\":\"Giải thích\","
              + "\"difficulty\":\"MEDIUM\","
              + "\"usedParameters\":{\"a\":1}}";

      // ===== ACT =====
      GeneratedQuestionSample placeholderResult =
          invokePrivate(
              "parseGeneratedQuestion",
              new Class<?>[] {String.class, QuestionTemplate.class, Map.class, String.class, QuestionDifficulty.class, String.class},
              optionPlaceholderJson,
              dynamicTemplate,
              params,
              "2",
              QuestionDifficulty.MEDIUM,
              "base");

      GeneratedQuestionSample injectedResult =
          invokePrivate(
              "parseGeneratedQuestion",
              new Class<?>[] {String.class, QuestionTemplate.class, Map.class, String.class, QuestionDifficulty.class, String.class},
              noCorrectAnswerJson,
              fixedTemplate,
              Map.of("a", 1),
              "5",
              QuestionDifficulty.MEDIUM,
              "base");

      GeneratedQuestionSample catchResult =
          invokePrivate(
              "parseGeneratedQuestion",
              new Class<?>[] {String.class, QuestionTemplate.class, Map.class, String.class, QuestionDifficulty.class, String.class},
              "invalid-json",
              fixedTemplate,
              Map.of("a", 1),
              "5",
              QuestionDifficulty.MEDIUM,
              "base question");

      // ===== ASSERT =====
      assertNotNull(placeholderResult.getOptions().get("A"));
      assertEquals("A", injectedResult.getCorrectAnswer());
      assertEquals("5", injectedResult.getOptions().get("A"));
      assertEquals("base question", catchResult.getQuestionText());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_build_solution_steps_passthrough() {
      // ===== ARRANGE =====
      String explanation = "Step 1: add. Step 2: conclude.";

      // ===== ACT =====
      String steps = invokePrivate("buildSolutionSteps", new Class<?>[] {String.class}, explanation);

      // ===== ASSERT =====
      assertEquals(explanation, steps);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_resolve_effective_parameters_full_branch_set() throws Exception {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .templateText(Map.of("vi", "A={{a}}, extra={{extra}}"))
              .parameters(Map.of("a", Map.of("type", "integer", "min", 1, "max", 10)))
              .optionsGenerator(Map.of("A", "{{extra}}", "B", "2"))
              .diagramTemplate("d={{extra}}")
              .answerFormula("a+1")
              .build();
      Map<String, Object> fallback = new LinkedHashMap<>();
      fallback.put("a", 2);

      JsonNode notObject = mapper.readTree("{\"usedParameters\":[]}");
      JsonNode objectNode =
          mapper.readTree(
              "{\"usedParameters\":{"
                  + "\"a\":5,"
                  + "\"ghost\":\"x\","
                  + "\"extra\":\"hello\","
                  + "\"badNull\":null"
                  + "}}");

      // ===== ACT =====
      Map<String, Object> unchanged =
          invokePrivate(
              "resolveEffectiveParameters", new Class<?>[] {JsonNode.class, QuestionTemplate.class, Map.class}, notObject, template, fallback);
      Map<String, Object> resolved =
          invokePrivate(
              "resolveEffectiveParameters", new Class<?>[] {JsonNode.class, QuestionTemplate.class, Map.class}, objectNode, template, fallback);

      // ===== ASSERT =====
      assertEquals(2, unchanged.get("a"));
      assertEquals(5, resolved.get("a"));
      assertEquals("hello", resolved.get("extra"));
      assertEquals(null, resolved.get("ghost"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_extract_long_and_double_value_all_text_branches() throws Exception {
      // ===== ARRANGE =====
      JsonNode intText = mapper.readTree("\"42\"");
      JsonNode decimalIntegralText = mapper.readTree("\"10.0\"");
      JsonNode invalidText = mapper.readTree("\"abc\"");
      JsonNode blankText = mapper.readTree("\"   \"");
      JsonNode decimalText = mapper.readTree("\"3,14\"");

      // ===== ACT =====
      Long l1 = invokePrivate("extractLongValue", new Class<?>[] {JsonNode.class}, intText);
      Long l2 = invokePrivate("extractLongValue", new Class<?>[] {JsonNode.class}, decimalIntegralText);
      Long l3 = invokePrivate("extractLongValue", new Class<?>[] {JsonNode.class}, invalidText);
      Long l4 = invokePrivate("extractLongValue", new Class<?>[] {JsonNode.class}, blankText);
      Double d1 = invokePrivate("extractDoubleValue", new Class<?>[] {JsonNode.class}, decimalText);
      Double d2 = invokePrivate("extractDoubleValue", new Class<?>[] {JsonNode.class}, invalidText);

      // ===== ASSERT =====
      assertEquals(42L, l1);
      assertEquals(10L, l2);
      assertEquals(null, l3);
      assertEquals(null, l4);
      assertEquals(3.14, d1);
      assertEquals(null, d2);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_is_ai_parameter_value_allowed_integer_double_and_false_paths() {
      // ===== ARRANGE =====
      Map<String, Object> intDef = new LinkedHashMap<>();
      intDef.put("min", 1);
      intDef.put("max", 5);
      intDef.put("exclude", List.of(2));

      Map<String, Object> doubleDef = new LinkedHashMap<>();
      doubleDef.put("min", 5.0);
      doubleDef.put("max", 1.0);

      // ===== ACT =====
      boolean aZero =
          invokePrivate(
              "isAIParameterValueAllowed",
              new Class<?>[] {String.class, Object.class, String.class, Map.class},
              "a",
              0,
              "integer",
              intDef);
      boolean excluded =
          invokePrivate(
              "isAIParameterValueAllowed",
              new Class<?>[] {String.class, Object.class, String.class, Map.class},
              "b",
              2,
              "integer",
              intDef);
      boolean validInt =
          invokePrivate(
              "isAIParameterValueAllowed",
              new Class<?>[] {String.class, Object.class, String.class, Map.class},
              "b",
              3,
              "integer",
              intDef);
      boolean validDouble =
          invokePrivate(
              "isAIParameterValueAllowed",
              new Class<?>[] {String.class, Object.class, String.class, Map.class},
              "x",
              5.0,
              "double",
              doubleDef);
      boolean invalidDouble =
          invokePrivate(
              "isAIParameterValueAllowed",
              new Class<?>[] {String.class, Object.class, String.class, Map.class},
              "x",
              5.1,
              "double",
              doubleDef);

      // ===== ASSERT =====
      assertFalse(aZero);
      assertFalse(excluded);
      assertTrue(validInt);
      assertTrue(validDouble);
      assertFalse(invalidDouble);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_ai_options_use_template_parameters_both_true_and_false() throws Exception {
      // ===== ARRANGE =====
      JsonNode withPlaceholder = mapper.readTree("{\"A\":\"{{x}}\",\"B\":\"2\"}");
      JsonNode withParamWord = mapper.readTree("{\"A\":\"value x\",\"B\":\"2\"}");
      JsonNode withoutAny = mapper.readTree("{\"A\":\"1\",\"B\":\"2\"}");
      Map<String, Object> params = Map.of("x", 5);

      // ===== ACT =====
      boolean byPlaceholder =
          invokePrivate("aiOptionsUseTemplateParameters", new Class<?>[] {JsonNode.class, Map.class}, withPlaceholder, params);
      boolean byWord =
          invokePrivate("aiOptionsUseTemplateParameters", new Class<?>[] {JsonNode.class, Map.class}, withParamWord, params);
      boolean noMatch =
          invokePrivate("aiOptionsUseTemplateParameters", new Class<?>[] {JsonNode.class, Map.class}, withoutAny, params);

      // ===== ASSERT =====
      assertTrue(byPlaceholder);
      assertTrue(byWord);
      assertFalse(noMatch);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_is_integer_parameter_type_all_supported_aliases_and_false_case() {
      // ===== ACT =====
      boolean integer = invokePrivate("isIntegerParameterType", new Class<?>[] {String.class}, "integer");
      boolean intType = invokePrivate("isIntegerParameterType", new Class<?>[] {String.class}, "int");
      boolean longType = invokePrivate("isIntegerParameterType", new Class<?>[] {String.class}, "long");
      boolean shortType = invokePrivate("isIntegerParameterType", new Class<?>[] {String.class}, "short");
      boolean byteType = invokePrivate("isIntegerParameterType", new Class<?>[] {String.class}, "byte");
      boolean wholeType = invokePrivate("isIntegerParameterType", new Class<?>[] {String.class}, "whole");
      boolean decimalType = invokePrivate("isIntegerParameterType", new Class<?>[] {String.class}, "double");

      // ===== ASSERT =====
      assertTrue(integer);
      assertTrue(intType);
      assertTrue(longType);
      assertTrue(shortType);
      assertTrue(byteType);
      assertTrue(wholeType);
      assertFalse(decimalType);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_evaluate_primary_all_remaining_branches_and_throw_paths() throws Exception {
      // ===== ARRANGE =====
      Method evaluatePrimary =
          AIEnhancementServiceImpl.class.getDeclaredMethod("evaluatePrimary", String.class, int[].class);
      evaluatePrimary.setAccessible(true);

      // ===== ACT & ASSERT =====
      assertThrows(
          Exception.class, () -> evaluatePrimary.invoke(aiEnhancementService, "", new int[] {0}));

      assertThrows(
          Exception.class,
          () -> evaluatePrimary.invoke(aiEnhancementService, "(1+2", new int[] {0}));

      assertThrows(
          Exception.class,
          () -> evaluatePrimary.invoke(aiEnhancementService, "sqrt(9", new int[] {0}));

      assertThrows(
          Exception.class,
          () -> evaluatePrimary.invoke(aiEnhancementService, "_unknown", new int[] {0}));

      double pi = (double) evaluatePrimary.invoke(aiEnhancementService, "pi", new int[] {0});
      double mathPi =
          (double) evaluatePrimary.invoke(aiEnhancementService, "math.pi", new int[] {0});
      double eVal = (double) evaluatePrimary.invoke(aiEnhancementService, "e", new int[] {0});
      double mathE =
          (double) evaluatePrimary.invoke(aiEnhancementService, "math.e", new int[] {0});
      double decimal =
          (double) evaluatePrimary.invoke(aiEnhancementService, "12.5", new int[] {0});
      double unaryMinus =
          (double) evaluatePrimary.invoke(aiEnhancementService, "-(3)", new int[] {0});

      assertEquals(Math.PI, pi, 0.000001);
      assertEquals(Math.PI, mathPi, 0.000001);
      assertEquals(Math.E, eVal, 0.000001);
      assertEquals(Math.E, mathE, 0.000001);
      assertEquals(12.5, decimal, 0.000001);
      assertEquals(-3.0, unaryMinus, 0.000001);

      assertThrows(
          Exception.class,
          () -> evaluatePrimary.invoke(aiEnhancementService, ".", new int[] {0}));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_throw_for_each_evaluate_known_function_when_argument_count_is_invalid() throws Exception {
      // ===== ARRANGE =====
      Method evalKnownFn =
          AIEnhancementServiceImpl.class.getDeclaredMethod("evaluateKnownFunction", String.class, List.class);
      evalKnownFn.setAccessible(true);

      // ===== ACT & ASSERT =====
      assertThrows(
          Exception.class,
          () -> evalKnownFn.invoke(aiEnhancementService, "sqrt", List.of(9.0, 4.0)));
      assertThrows(
          Exception.class,
          () -> evalKnownFn.invoke(aiEnhancementService, "abs", List.of(1.0, 2.0)));
      assertThrows(
          Exception.class,
          () -> evalKnownFn.invoke(aiEnhancementService, "pow", List.of(2.0)));
      assertThrows(
          Exception.class,
          () -> evalKnownFn.invoke(aiEnhancementService, "max", List.of(2.0)));
      assertThrows(
          Exception.class,
          () -> evalKnownFn.invoke(aiEnhancementService, "min", List.of(2.0)));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_build_enhancement_prompt_when_non_mcq_and_context_blank() {
      // ===== ARRANGE =====
      AIEnhancementRequest request =
          AIEnhancementRequest.builder()
              .rawQuestionText("Tinh gia tri bieu thuc 3 + 4.")
              .questionType(QuestionType.SHORT_ANSWER)
              .correctAnswer("7")
              .rawOptions(null)
              .difficulty(QuestionDifficulty.EASY)
              .context("   ")
              .build();

      // ===== ACT =====
      String prompt =
          invokePrivate("buildEnhancementPrompt", new Class<?>[] {AIEnhancementRequest.class}, request);

      // ===== ASSERT =====
      assertFalse(prompt.contains("ORIGINAL OPTIONS"));
      assertFalse(prompt.contains("CONTEXT:"));
      assertTrue(prompt.contains("DIFFICULTY: EASY"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_ai_response_when_optional_fields_are_missing() {
      // ===== ARRANGE =====
      AIEnhancementRequest request =
          AIEnhancementRequest.builder()
              .rawQuestionText("Noi dung goc")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .correctAnswer("2")
              .rawOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .build();
      String minimalJson = "{\"enhancedQuestion\":\"\",\"correctAnswerKey\":\"B\",\"explanation\":\"\"}";

      // ===== ACT =====
      AIEnhancedQuestionResponse response =
          invokePrivate(
              "parseAIResponse",
              new Class<?>[] {String.class, AIEnhancementRequest.class},
              minimalJson,
              request);

      // ===== ASSERT =====
      assertEquals("Noi dung goc", response.getEnhancedQuestionText());
      assertEquals(null, response.getEnhancedOptions());
      assertEquals(null, response.getAlternativeSolutions());
      assertEquals(null, response.getDistractorExplanations());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_extract_json_when_curly_braces_are_missing_or_long_raw_text() {
      // ===== ARRANGE =====
      String noJson = "plain text without braces";
      String longRaw = "x".repeat(520);

      // ===== ACT =====
      String extractedNoJson = invokePrivate("extractJSON", new Class<?>[] {String.class}, noJson);
      String extractedLongRaw = invokePrivate("extractJSON", new Class<?>[] {String.class}, longRaw);

      // ===== ASSERT =====
      assertEquals(noJson, extractedNoJson);
      assertEquals(longRaw, extractedLongRaw);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_clean_option_value_when_input_is_blank_or_cleans_to_empty() {
      // ===== ARRANGE =====
      String blank = "   ";
      String onlyParentheses = "(abc)";

      // ===== ACT =====
      String blankResult = invokePrivate("cleanOptionValue", new Class<?>[] {String.class}, blank);
      String emptiedResult =
          invokePrivate("cleanOptionValue", new Class<?>[] {String.class}, onlyParentheses);

      // ===== ASSERT =====
      assertEquals(blank, blankResult);
      assertEquals(onlyParentheses, emptiedResult);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_options_null_non_object_and_blank_values() throws Exception {
      // ===== ARRANGE =====
      JsonNode nullNode = mapper.readTree("null");
      JsonNode arrayNode = mapper.readTree("[1,2]");
      JsonNode objectWithBlank = mapper.readTree("{\"A\":\"\",\"B\":\"2\"}");

      // ===== ACT =====
      Map<String, String> fromNull =
          invokePrivate("parseCanonicalOptions", new Class<?>[] {JsonNode.class, Map.class}, nullNode, Map.of());
      Map<String, String> fromArray =
          invokePrivate("parseCanonicalOptions", new Class<?>[] {JsonNode.class, Map.class}, arrayNode, Map.of());
      Map<String, String> fromBlankObject =
          invokePrivate(
              "parseCanonicalOptions",
              new Class<?>[] {JsonNode.class, Map.class},
              objectWithBlank,
              Map.of());

      // ===== ASSERT =====
      assertTrue(fromNull.isEmpty());
      assertTrue(fromArray.isEmpty());
      assertEquals("2", fromBlankObject.get("B"));
      assertEquals(null, fromBlankObject.get("A"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_resolve_effective_parameters_when_root_or_template_is_null() throws Exception {
      // ===== ARRANGE =====
      JsonNode root = mapper.readTree("{\"usedParameters\":{\"a\":1}}");
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .parameters(Map.of("a", Map.of("type", "integer", "min", 1, "max", 9)))
              .build();

      // ===== ACT =====
      Map<String, Object> withNullRoot =
          invokePrivate(
              "resolveEffectiveParameters",
              new Class<?>[] {JsonNode.class, QuestionTemplate.class, Map.class},
              null,
              template,
              Map.of("a", 2));
      Map<String, Object> withNullTemplate =
          invokePrivate(
              "resolveEffectiveParameters",
              new Class<?>[] {JsonNode.class, QuestionTemplate.class, Map.class},
              root,
              null,
              Map.of("a", 3));
      Map<String, Object> withNullFallback =
          invokePrivate(
              "resolveEffectiveParameters",
              new Class<?>[] {JsonNode.class, QuestionTemplate.class, Map.class},
              root,
              template,
              null);

      // ===== ASSERT =====
      assertEquals(2, withNullRoot.get("a"));
      assertEquals(3, withNullTemplate.get("a"));
      assertEquals(1, withNullFallback.get("a"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_pick_parameters_for_non_map_integer_exclude_and_double_ranges() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .parameters(
                  Map.of(
                      "legacy", "not-a-map",
                      "a", Map.of("type", "integer", "min", 1, "max", 0, "exclude", List.of(0)),
                      "ratio", Map.of("type", "double", "min", 1.5, "max", 2.5)))
              .build();

      // ===== ACT =====
      Map<String, Object> picked =
          invokePrivate("pickParameters", new Class<?>[] {QuestionTemplate.class, int.class}, template, 1);

      // ===== ASSERT =====
      assertEquals(1, picked.get("legacy"));
      assertTrue(((Number) picked.get("a")).intValue() >= 1);
      assertTrue(((Number) picked.get("ratio")).doubleValue() >= 1.5);
      assertTrue(((Number) picked.get("ratio")).doubleValue() <= 2.5);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_normalize_formula_wrappers_and_fraction_replacement_loops() {
      // ===== ARRANGE =====
      String wrappedRound = "\\(\\frac{1}{2} + 1\\)";
      String wrappedSquare = "\\[\\frac{a}{b}\\]";

      // ===== ACT =====
      String normalizedRound =
          invokePrivate("normalizeFormulaForEvaluation", new Class<?>[] {String.class}, wrappedRound);
      String normalizedSquare =
          invokePrivate("normalizeFormulaForEvaluation", new Class<?>[] {String.class}, wrappedSquare);

      // ===== ASSERT =====
      assertTrue(normalizedRound.contains("((1)/(2)) + 1"));
      assertTrue(normalizedSquare.contains("((a)/(b))"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_generated_question_for_non_mcq_and_missing_fields() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.SHORT_ANSWER)
              .answerFormula("x+1")
              .templateText(Map.of("vi", "Gia tri cua bieu thuc la {{x}}"))
              .parameters(Map.of("x", Map.of("type", "integer", "min", 1, "max", 9)))
              .build();
      Map<String, Object> params = Map.of("x", 2);
      String json =
          "{"
              + "\"questionText\":\"\","
              + "\"correctAnswer\":\"\","
              + "\"explanation\":\"\","
              + "\"solutionSteps\":\"\","
              + "\"difficulty\":\"\","
              + "\"answerCalculation\":\"\""
              + "}";

      // ===== ACT =====
      GeneratedQuestionSample sample =
          invokePrivate(
              "parseCanonicalGeneratedQuestion",
              new Class<?>[] {
                String.class,
                QuestionTemplate.class,
                Map.class,
                QuestionDifficulty.class,
                String.class,
                String.class,
                String.class
              },
              json,
              template,
              params,
              QuestionDifficulty.MEDIUM,
              "Fallback question",
              "Fallback explanation",
              "diagram");

      // ===== ASSERT =====
      assertEquals("Fallback question", sample.getQuestionText());
      assertEquals("x+1", sample.getCorrectAnswer());
      assertEquals("Fallback explanation", sample.getExplanation());
      assertEquals("Fallback explanation", sample.getSolutionSteps());
      assertEquals("x+1", sample.getAnswerCalculation());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_generate_question_from_canonical_with_non_mcq_fallback_path() {
      // ===== ARRANGE =====
      CanonicalQuestion cq = new CanonicalQuestion();
      cq.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
      cq.setProblemText(null);
      cq.setSolutionSteps(null);
      cq.setDiagramDefinition(null);
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.SHORT_ANSWER)
              .templateText(Map.of("vi", "Tinh {{a}} + 1"))
              .answerFormula("a+1")
              .parameters(Map.of("a", Map.of("type", "integer", "min", 1, "max", 1)))
              .build();
      when(geminiService.sendMessage(anyString())).thenThrow(new RuntimeException("offline"));

      // ===== ACT =====
      GeneratedQuestionSample result =
          aiEnhancementService.generateQuestionFromCanonical(cq, template, 0);

      // ===== ASSERT =====
      assertEquals(null, result.getOptions());
      assertEquals("a+1", result.getCorrectAnswer());
      assertEquals("a+1", result.getAnswerCalculation());

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_generated_question_mcq_with_template_option_fallback_and_invalid_key() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .answerFormula("a+b")
              .templateText(Map.of("vi", "Cau hoi {{a}} + {{b}}"))
              .parameters(
                  Map.of(
                      "a", Map.of("type", "integer", "min", 1, "max", 9),
                      "b", Map.of("type", "integer", "min", 1, "max", 9)))
              .optionsGenerator(Map.of("A", "{{a}}", "B", "{{b}}", "C", "10", "D", "11"))
              .build();
      Map<String, Object> params = Map.of("a", 3, "b", 4);
      String json =
          "{"
              + "\"questionText\":\"Q\","
              + "\"options\":{},"
              + "\"correctAnswer\":\"Z\","
              + "\"difficulty\":\"UNKNOWN\","
              + "\"explanation\":\"E\","
              + "\"solutionSteps\":\"\","
              + "\"answerCalculation\":\"\""
              + "}";

      // ===== ACT =====
      GeneratedQuestionSample sample =
          invokePrivate(
              "parseCanonicalGeneratedQuestion",
              new Class<?>[] {
                String.class,
                QuestionTemplate.class,
                Map.class,
                QuestionDifficulty.class,
                String.class,
                String.class,
                String.class
              },
              json,
              template,
              params,
              QuestionDifficulty.HARD,
              "fallbackQ",
              "fallbackE",
              "diagram");

      // ===== ASSERT =====
      assertEquals("A", sample.getCorrectAnswer());
      assertNotNull(sample.getOptions().get("A"));
      assertEquals(QuestionDifficulty.HARD, sample.getCalculatedDifficulty());
      assertEquals("a+b", sample.getAnswerCalculation());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_generated_question_mcq_when_options_exist_and_correct_key_is_valid() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .answerFormula("a+b")
              .templateText(Map.of("vi", "Q"))
              .parameters(Map.of("a", Map.of("type", "integer", "min", 1, "max", 9)))
              .optionsGenerator(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .build();
      String json =
          "{"
              + "\"questionText\":\"Q\","
              + "\"options\":{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\",\"D\":\"4\"},"
              + "\"correctAnswer\":\"B\","
              + "\"explanation\":\"E\","
              + "\"solutionSteps\":\"S\","
              + "\"difficulty\":\"MEDIUM\","
              + "\"answerCalculation\":\"calc\""
              + "}";

      // ===== ACT =====
      GeneratedQuestionSample sample =
          invokePrivate(
              "parseCanonicalGeneratedQuestion",
              new Class<?>[] {
                String.class,
                QuestionTemplate.class,
                Map.class,
                QuestionDifficulty.class,
                String.class,
                String.class,
                String.class
              },
              json,
              template,
              Map.of("a", 1),
              QuestionDifficulty.HARD,
              "fallbackQ",
              "fallbackE",
              "diagram");

      // ===== ASSERT =====
      assertEquals("B", sample.getCorrectAnswer());
      assertEquals("calc", sample.getAnswerCalculation());
      assertEquals("S", sample.getSolutionSteps());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_build_canonical_generation_prompt_for_mcq_with_diagram() {
      // ===== ARRANGE =====
      CanonicalQuestion cq = new CanonicalQuestion();
      cq.setProblemText("Bai toan mau");
      cq.setSolutionSteps("Buoc giai");
      cq.setDiagramDefinition("ve hinh tam giac");
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .build();

      // ===== ACT =====
      String prompt =
          invokePrivate(
              "buildCanonicalGenerationPrompt",
              new Class<?>[] {
                CanonicalQuestion.class,
                QuestionTemplate.class,
                Map.class,
                String.class,
                String.class,
                int.class
              },
              cq,
              template,
              Map.of("a", 1),
              "P",
              "S",
              0);

      // ===== ASSERT =====
      assertTrue(prompt.contains("CANONICAL DIAGRAM"));
      assertTrue(prompt.contains("Provide exactly 4 options"));
      assertTrue(prompt.contains("correctAnswer must be one key"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_find_correct_answer_key_when_options_or_answer_are_null() {
      // ===== ACT =====
      String keyWithNullOptions =
          invokePrivate(
              "findCorrectAnswerKey",
              new Class<?>[] {Map.class, String.class},
              null,
              "2");
      String keyWithNullAnswer =
          invokePrivate(
              "findCorrectAnswerKey",
              new Class<?>[] {Map.class, String.class},
              Map.of("A", "1", "B", "2"),
              null);

      // ===== ASSERT =====
      assertEquals("A", keyWithNullOptions);
      assertEquals("A", keyWithNullAnswer);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_include_context_and_skip_original_options_when_mcq_has_null_options() {
      // ===== ARRANGE =====
      AIEnhancementRequest request =
          AIEnhancementRequest.builder()
              .rawQuestionText("Tinh tong hai so.")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .correctAnswer("5")
              .rawOptions(null)
              .difficulty(QuestionDifficulty.MEDIUM)
              .context("Ngu canh co noi dung bo sung")
              .build();

      // ===== ACT =====
      String prompt =
          invokePrivate("buildEnhancementPrompt", new Class<?>[] {AIEnhancementRequest.class}, request);

      // ===== ASSERT =====
      assertTrue(prompt.contains("CONTEXT: Ngu canh co noi dung bo sung"));
      assertFalse(prompt.contains("ORIGINAL OPTIONS"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_skip_context_when_context_is_null() {
      // ===== ARRANGE =====
      AIEnhancementRequest request =
          AIEnhancementRequest.builder()
              .rawQuestionText("Tinh hieu hai so.")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .correctAnswer("2")
              .rawOptions(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .difficulty(QuestionDifficulty.MEDIUM)
              .context(null)
              .build();

      // ===== ACT =====
      String prompt =
          invokePrivate("buildEnhancementPrompt", new Class<?>[] {AIEnhancementRequest.class}, request);

      // ===== ASSERT =====
      assertTrue(prompt.contains("ORIGINAL OPTIONS"));
      assertFalse(prompt.contains("CONTEXT:"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_return_default_key_when_no_option_matches_correct_answer() {
      // ===== ACT =====
      String result =
          invokePrivate(
              "findCorrectAnswerKey",
              new Class<?>[] {Map.class, String.class},
              Map.of("A", "10", "B", "11"),
              "999");

      // ===== ASSERT =====
      assertEquals("A", result);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_throw_number_format_exception_when_parse_numeric_answer_receives_null() {
      // ===== ARRANGE =====
      Method parseNumeric;
      try {
        parseNumeric = AIEnhancementServiceImpl.class.getDeclaredMethod("parseNumericAnswer", String.class);
        parseNumeric.setAccessible(true);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }

      // ===== ACT & ASSERT =====
      Exception thrown =
          assertThrows(
              Exception.class,
              () -> parseNumeric.invoke(aiEnhancementService, new Object[] {null}));
      assertTrue(thrown.getCause() instanceof NumberFormatException);
      assertTrue(thrown.getCause().getMessage().contains("Answer is null"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_return_false_when_math_symbols_exist_but_other_subject_keyword_present() {
      // ===== ACT =====
      boolean result =
          invokePrivate(
              "isMathematicsContent",
              new Class<?>[] {String.class},
              "Compute 2+3 while discussing history timeline.");

      // ===== ASSERT =====
      assertFalse(result);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_return_true_when_math_symbols_exist_without_math_keywords() {
      // ===== ACT =====
      boolean result =
          invokePrivate("isMathematicsContent", new Class<?>[] {String.class}, "2+3=5");

      // ===== ASSERT =====
      assertTrue(result);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_is_same_answer_original_option_scan_with_null_and_non_matching_values() {
      // ===== ARRANGE =====
      Map<String, String> originalOptions = new LinkedHashMap<>();
      originalOptions.put("A", null);
      originalOptions.put("B", "2");
      Map<String, String> aiOptions = Map.of("B", "2");

      // ===== ACT =====
      boolean same =
          invokePrivate(
              "isSameAnswer",
              new Class<?>[] {String.class, Map.class, String.class, Map.class},
              "2",
              originalOptions,
              "B",
              aiOptions);

      // ===== ASSERT =====
      assertTrue(same);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_is_same_answer_when_original_answer_is_null() {
      // ===== ACT =====
      boolean result =
          invokePrivate(
              "isSameAnswer",
              new Class<?>[] {String.class, Map.class, String.class, Map.class},
              null,
              Map.of("A", "1"),
              "A",
              Map.of("A", "1"));

      // ===== ASSERT =====
      assertFalse(result);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_generate_question_from_canonical_when_problem_and_solution_are_blank_and_diagram_present() {
      // ===== ARRANGE =====
      CanonicalQuestion canonicalQuestion = new CanonicalQuestion();
      canonicalQuestion.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
      canonicalQuestion.setProblemText("   ");
      canonicalQuestion.setSolutionSteps("   ");
      canonicalQuestion.setDiagramDefinition("line {{a}}");

      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .templateText(Map.of("vi", "Bai toan {{a}}"))
              .answerFormula("a+1")
              .parameters(Map.of("a", Map.of("type", "integer", "min", 1, "max", 1)))
              .optionsGenerator(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .build();

      when(geminiService.sendMessage(anyString())).thenThrow(new RuntimeException("offline"));

      // ===== ACT =====
      GeneratedQuestionSample result =
          aiEnhancementService.generateQuestionFromCanonical(canonicalQuestion, template, 0);

      // ===== ASSERT =====
      assertNotNull(result.getQuestionText());
      assertEquals("AI-generated explanation from canonical source", result.getExplanation());
      assertNotNull(result.getDiagramData());
      assertEquals("A", result.getCorrectAnswer());

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_build_canonical_prompt_non_mcq_and_blank_diagram() {
      // ===== ARRANGE =====
      CanonicalQuestion canonicalQuestion = new CanonicalQuestion();
      canonicalQuestion.setProblemText("De bai");
      canonicalQuestion.setSolutionSteps("Loi giai");
      canonicalQuestion.setDiagramDefinition("   ");

      QuestionTemplate template =
          QuestionTemplate.builder().templateType(QuestionType.SHORT_ANSWER).build();

      // ===== ACT =====
      String prompt =
          invokePrivate(
              "buildCanonicalGenerationPrompt",
              new Class<?>[] {
                CanonicalQuestion.class,
                QuestionTemplate.class,
                Map.class,
                String.class,
                String.class,
                int.class
              },
              canonicalQuestion,
              template,
              Map.of("a", 1),
              "P",
              "S",
              1);

      // ===== ASSERT =====
      assertFalse(prompt.contains("CANONICAL DIAGRAM"));
      assertTrue(prompt.contains("For non-MCQ, options can be empty object {}."));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_generated_question_non_mcq_blank_answer_and_template_formula_null() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.SHORT_ANSWER)
              .answerFormula(null)
              .templateText(Map.of("vi", "Q"))
              .parameters(Map.of())
              .build();
      String json =
          "{"
              + "\"questionText\":\"\","
              + "\"explanation\":\"\","
              + "\"solutionSteps\":\"\","
              + "\"correctAnswer\":\"\","
              + "\"difficulty\":\"X\","
              + "\"answerCalculation\":\"\""
              + "}";

      // ===== ACT =====
      GeneratedQuestionSample result =
          invokePrivate(
              "parseCanonicalGeneratedQuestion",
              new Class<?>[] {
                String.class,
                QuestionTemplate.class,
                Map.class,
                QuestionDifficulty.class,
                String.class,
                String.class,
                String.class
              },
              json,
              template,
              Map.of(),
              QuestionDifficulty.MEDIUM,
              "fallback question",
              "fallback explanation",
              "diagram");

      // ===== ASSERT =====
      assertEquals("N/A", result.getCorrectAnswer());
      assertEquals(null, result.getAnswerCalculation());
      assertEquals("fallback question", result.getQuestionText());
      assertEquals("fallback explanation", result.getExplanation());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_generated_question_non_mcq_when_correct_answer_exists() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.SHORT_ANSWER)
              .answerFormula("fallback")
              .templateText(Map.of("vi", "Q"))
              .parameters(Map.of())
              .build();
      String json =
          "{"
              + "\"questionText\":\"Q\","
              + "\"explanation\":\"E\","
              + "\"solutionSteps\":\"S\","
              + "\"correctAnswer\":\"42\","
              + "\"difficulty\":\"MEDIUM\","
              + "\"answerCalculation\":\"A\""
              + "}";

      // ===== ACT =====
      GeneratedQuestionSample result =
          invokePrivate(
              "parseCanonicalGeneratedQuestion",
              new Class<?>[] {
                String.class,
                QuestionTemplate.class,
                Map.class,
                QuestionDifficulty.class,
                String.class,
                String.class,
                String.class
              },
              json,
              template,
              Map.of(),
              QuestionDifficulty.MEDIUM,
              "fallback question",
              "fallback explanation",
              "diagram");

      // ===== ASSERT =====
      assertEquals("42", result.getCorrectAnswer());
      assertEquals("A", result.getAnswerCalculation());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_generated_question_catch_branch_for_non_mcq() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.SHORT_ANSWER)
              .answerFormula("a+1")
              .templateText(Map.of("vi", "Q"))
              .parameters(Map.of("a", Map.of("type", "integer", "min", 1, "max", 1)))
              .build();

      // ===== ACT =====
      GeneratedQuestionSample result =
          invokePrivate(
              "parseCanonicalGeneratedQuestion",
              new Class<?>[] {
                String.class,
                QuestionTemplate.class,
                Map.class,
                QuestionDifficulty.class,
                String.class,
                String.class,
                String.class
              },
              "invalid-json",
              template,
              Map.of("a", 1),
              QuestionDifficulty.HARD,
              "fallback question",
              "fallback explanation",
              "diagram");

      // ===== ASSERT =====
      assertEquals(null, result.getOptions());
      assertEquals("N/A", result.getCorrectAnswer());
      assertEquals("a+1", result.getAnswerCalculation());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_parse_canonical_options_and_ensure_keys_with_blank_and_null_paths() throws Exception {
      // ===== ARRANGE =====
      JsonNode withBlank = mapper.readTree("{\"A\":\"\",\"B\":\"2\"}");

      // ===== ACT =====
      Map<String, String> parsed =
          invokePrivate(
              "parseCanonicalOptions",
              new Class<?>[] {JsonNode.class, Map.class},
              withBlank,
              Map.of());
      assertEquals(null, parsed.get("A"));
      assertEquals("2", parsed.get("B"));

      invokePrivate("ensureCanonicalFourOptionKeys", new Class<?>[] {Map.class}, (Object) null);
      invokePrivate("ensureCanonicalFourOptionKeys", new Class<?>[] {Map.class}, parsed);

      // ===== ASSERT =====
      assertEquals("Lua chon A", parsed.get("A"));
      assertEquals("2", parsed.get("B"));
      assertNotNull(parsed.get("C"));
      assertNotNull(parsed.get("D"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_return_empty_map_when_parse_canonical_options_input_is_null() {
      // ===== ACT =====
      Map<String, String> parsed =
          invokePrivate(
              "parseCanonicalOptions",
              new Class<?>[] {JsonNode.class, Map.class},
              null,
              Map.of());

      // ===== ASSERT =====
      assertTrue(parsed.isEmpty());

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_cover_evaluate_formula_blank_input_and_engine_null_fallback_return_unknown() {
      // ===== ACT =====
      String blankResult =
          invokePrivate("evaluateFormula", new Class<?>[] {String.class, Map.class}, "   ", Map.of());
      String nullResult =
          invokePrivate("evaluateFormula", new Class<?>[] {String.class, Map.class}, null, Map.of());
      String unknownResult =
          invokePrivate("evaluateFormula", new Class<?>[] {String.class, Map.class}, "{{x}}", Map.of());

      // ===== ASSERT =====
      assertEquals("?", blankResult);
      assertEquals("?", nullResult);
      assertEquals("?", unknownResult);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    @Test
    void it_should_return_null_when_clean_option_value_input_is_null() {
      // ===== ACT =====
      String result = invokePrivate("cleanOptionValue", new Class<?>[] {String.class}, (Object) null);

      // ===== ASSERT =====
      assertEquals(null, result);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: parseArithmetic ném lỗi khi còn token dư sau khi parse.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>if (pos[0] != expr.length()) -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw RuntimeException và assert message lỗi cụ thể</li>
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_with_message_when_parse_arithmetic_has_unexpected_token() {
      // ===== ARRANGE =====
      Method parseArithmetic;
      try {
        parseArithmetic = AIEnhancementServiceImpl.class.getDeclaredMethod("parseArithmetic", String.class);
        parseArithmetic.setAccessible(true);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }

      // ===== ACT & ASSERT =====
      Exception thrown =
          assertThrows(
              Exception.class,
              () -> parseArithmetic.invoke(aiEnhancementService, "1+2x"));
      assertTrue(thrown.getCause() instanceof RuntimeException);
      assertTrue(thrown.getCause().getMessage().contains("Unexpected token"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Abnormal case: evaluateKnownFunction rơi vào default switch branch.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>switch default branch -> throw unknown function</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw RuntimeException và assert message chứa tên hàm</li>
     * </ul>
     */
    @Test
    void it_should_throw_unknown_function_message_when_switch_case_does_not_match() throws Exception {
      // ===== ARRANGE =====
      Method evalKnownFn =
          AIEnhancementServiceImpl.class.getDeclaredMethod("evaluateKnownFunction", String.class, List.class);
      evalKnownFn.setAccessible(true);

      // ===== ACT & ASSERT =====
      Exception thrown =
          assertThrows(
              Exception.class,
              () -> evalKnownFn.invoke(aiEnhancementService, "median", List.of(1.0, 2.0)));
      assertTrue(thrown.getCause() instanceof RuntimeException);
      assertTrue(thrown.getCause().getMessage().contains("Unknown function: median"));

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal + Abnormal case: isSameAnswer cover early-return branches và return cuối.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>answerKey == null || options == null -> TRUE branch</li>
     *   <li>aiAnswer == null -> TRUE branch</li>
     *   <li>return normalized1.equals(normalized2) -> TRUE branch</li>
     * </ul>
     */
    @Test
    void it_should_cover_is_same_answer_all_return_paths_for_null_and_string_fallback() {
      // ===== ACT =====
      boolean nullGuardResult =
          invokePrivate(
              "isSameAnswer",
              new Class<?>[] {String.class, Map.class, String.class, Map.class},
              "2",
              Map.of("A", "2"),
              null,
              Map.of("A", "2"));
      boolean missingAiAnswerResult =
          invokePrivate(
              "isSameAnswer",
              new Class<?>[] {String.class, Map.class, String.class, Map.class},
              "2",
              Map.of("A", "2"),
              "B",
              Map.of("A", "2"));
      boolean stringFallbackEqualResult =
          invokePrivate(
              "isSameAnswer",
              new Class<?>[] {String.class, Map.class, String.class, Map.class},
              "AnswerText",
              Map.of("A", "AnswerText"),
              "A",
              Map.of("A", "answertext"));

      // ===== ASSERT =====
      assertFalse(nullGuardResult);
      assertFalse(missingAiAnswerResult);
      assertTrue(stringFallbackEqualResult);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }

    /**
     * Normal case: cover return path của normalizeAnswer khi input null.
     */
    @Test
    void it_should_return_empty_string_when_normalize_answer_input_is_null() {
      // ===== ACT =====
      String normalized = invokePrivate("normalizeAnswer", new Class<?>[] {String.class}, (Object) null);

      // ===== ASSERT =====
      assertEquals("", normalized);

      // ===== VERIFY =====
      verify(geminiService, never()).sendMessage(anyString());
      verifyNoMoreInteractions(geminiService);
    }
  }
}
