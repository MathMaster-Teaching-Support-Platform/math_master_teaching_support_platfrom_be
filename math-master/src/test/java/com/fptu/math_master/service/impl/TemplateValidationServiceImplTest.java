package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.request.UpdateQuestionTemplateRequest;
import com.fptu.math_master.dto.response.TemplateValidationResponse;
import com.fptu.math_master.dto.response.TemplateValidationResponse.IssueSeverity;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionTag;
import com.fptu.math_master.enums.QuestionType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

/**
 * Unit tests for {@link TemplateValidationServiceImpl}.
 *
 * <p>Both {@code validateTemplate} and {@code validateTemplateUpdate} share the same validation
 * pipeline; tests assert equivalent outcomes on matching create/update payloads where applicable.
 */
@DisplayName("TemplateValidationServiceImpl - Tests")
class TemplateValidationServiceImplTest extends BaseUnitTest {

  @InjectMocks private TemplateValidationServiceImpl templateValidationService;

  // ----- Shared builders (realistic data per UT rules) -----

  private Map<String, Object> buildIntegerParameterDef(int min, int max) {
    Map<String, Object> def = new LinkedHashMap<>();
    def.put("type", "integer");
    def.put("min", min);
    def.put("max", max);
    return def;
  }

  private Map<String, Object> buildValidTemplateTextWithPlaceholder(String placeholder) {
    Map<String, Object> text = new LinkedHashMap<>();
    text.put(
        "vi",
        "Giải phương trình với tham số "
            + "{{"
            + placeholder
            + "}} và phép cộng 2 + 3 để kiểm tra toán tử.");
    text.put("en", "Solve using parameter {{" + placeholder + "}} and addition 2 + 3.");
    return text;
  }

  private QuestionTemplateRequest buildMinimalQuestionTemplateRequest(
      Map<String, Object> templateText,
      Map<String, Object> parameters,
      String answerFormula,
      QuestionType templateType,
      Map<String, Object> optionsGenerator,
      String[] constraints,
      List<QuestionTag> tags) {
    return QuestionTemplateRequest.builder()
        .name("Mẫu câu hỏi luyện tập phương trình bậc nhất")
        .description("Mẫu dùng cho lớp Đại số học kỳ 2")
        .templateType(templateType)
        .templateText(templateText)
        .parameters(parameters)
        .answerFormula(answerFormula)
        .optionsGenerator(optionsGenerator)
        .constraints(constraints)
        .cognitiveLevel(CognitiveLevel.VAN_DUNG)
        .tags(tags != null ? tags : List.of(QuestionTag.LINEAR_EQUATIONS))
        .isPublic(Boolean.TRUE)
        .build();
  }

  private UpdateQuestionTemplateRequest buildMinimalUpdateQuestionTemplateRequest(
      Map<String, Object> templateText,
      Map<String, Object> parameters,
      String answerFormula,
      QuestionType templateType,
      Map<String, Object> optionsGenerator,
      String[] constraints,
      List<QuestionTag> tags) {
    return UpdateQuestionTemplateRequest.builder()
        .name("Mẫu câu hỏi luyện tập phương trình bậc nhất")
        .description("Mẫu dùng cho lớp Đại số học kỳ 2")
        .templateType(templateType)
        .templateText(templateText)
        .parameters(parameters)
        .answerFormula(answerFormula)
        .optionsGenerator(optionsGenerator)
        .constraints(constraints)
        .cognitiveLevel(CognitiveLevel.VAN_DUNG)
        .tags(tags != null ? tags : List.of(QuestionTag.LINEAR_EQUATIONS))
        .isPublic(Boolean.TRUE)
        .build();
  }

  /** Assert matching create/update requests yield the same validation summary fields. */
  private void assertEquivalentFullValidation(
      QuestionTemplateRequest createReq, UpdateQuestionTemplateRequest updateReq) {
    TemplateValidationResponse rCreate = templateValidationService.validateTemplate(createReq);
    TemplateValidationResponse rUpdate =
        templateValidationService.validateTemplateUpdate(updateReq);
    assertEquals(rCreate.getIsValid(), rUpdate.getIsValid());
    assertEquals(rCreate.getErrorCount(), rUpdate.getErrorCount());
    assertEquals(rCreate.getWarningCount(), rUpdate.getWarningCount());
    assertNotNull(rCreate.getErrors());
    assertNotNull(rUpdate.getErrors());
  }

  @Nested
  @DisplayName("validateTemplate() / validateTemplateUpdate() — template text")
  class TemplateTextValidationTests {

    @Test
    @DisplayName("Abnormal: lỗi khi templateText null hoặc rỗng")
    void it_should_add_template_text_error_when_template_text_missing() {
      // ===== ARRANGE =====
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              null,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      UpdateQuestionTemplateRequest updateReq =
          buildMinimalUpdateQuestionTemplateRequest(
              Map.of(),
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse rNull = templateValidationService.validateTemplate(createReq);
      TemplateValidationResponse rEmptyMap =
          templateValidationService.validateTemplateUpdate(updateReq);

      // ===== ASSERT =====
      assertFalse(rNull.getIsValid());
      assertTrue(rNull.getErrorCount() >= 1);
      assertTrue(
          rNull.getErrors().stream()
              .anyMatch(
                  e ->
                      "TEMPLATE_TEXT".equals(e.getCategory())
                          && IssueSeverity.ERROR == e.getSeverity()));

      assertFalse(rEmptyMap.getIsValid());
      assertTrue(
          rEmptyMap.getErrors().stream()
              .anyMatch(e -> "TEMPLATE_TEXT".equals(e.getCategory())));

      // ===== VERIFY =====
      // Không có collaborator được mock; kiểm tra hành vi qua assertions phản hồi ở trên.
    }

    @Test
    @DisplayName("Warning: bản dịch rỗng theo ngôn ngữ")
    void it_should_warn_when_language_variant_has_blank_template_text() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = new LinkedHashMap<>();
      templateText.put("vi", "   ");
      templateText.put("en", "Expression {{" + "a" + "}} for review.");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 2",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      UpdateQuestionTemplateRequest updateReq =
          buildMinimalUpdateQuestionTemplateRequest(
              new LinkedHashMap<>(templateText),
              parameters,
              "a + 2",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      assertEquivalentFullValidation(createReq, updateReq);
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "TEMPLATE_TEXT".equals(w.getCategory())
                          && (w.getField() != null && w.getField().contains("vi"))));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: trộn dấu trừ Unicode và gạch nối")
    void it_should_warn_when_mixed_unicode_and_hyphen_minus_signs() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = new LinkedHashMap<>();
      templateText.put(
          "vi",
          "Biểu thức có phép trừ − 5 và gạch nối - 2 trong cùng dòng với {{a}} + 1.");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      UpdateQuestionTemplateRequest updateReq =
          buildMinimalUpdateQuestionTemplateRequest(
              new LinkedHashMap<>(templateText),
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse rCreate = templateValidationService.validateTemplate(createReq);
      TemplateValidationResponse rUpdate =
          templateValidationService.validateTemplateUpdate(updateReq);

      // ===== ASSERT =====
      assertTrue(
          rCreate.getWarnings().stream()
              .anyMatch(w -> w.getMessage() != null && w.getMessage().contains("minus")));
      assertTrue(
          rUpdate.getWarnings().stream()
              .anyMatch(w -> w.getMessage() != null && w.getMessage().contains("minus")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: placeholder trong template chưa khai báo tham số")
    void it_should_add_error_when_placeholder_not_defined_in_parameters() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("missingParam");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      UpdateQuestionTemplateRequest updateReq =
          buildMinimalUpdateQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      assertEquivalentFullValidation(createReq, updateReq);
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "TEMPLATE_TEXT".equals(e.getCategory())
                          && e.getMessage() != null
                          && e.getMessage().contains("missingParam")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: tham số không xuất hiện trong template text")
    void it_should_warn_when_parameter_defined_but_not_used_in_template() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = new LinkedHashMap<>();
      parameters.put("a", buildIntegerParameterDef(1, 10));
      parameters.put("unusedCoeff", buildIntegerParameterDef(1, 5));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      UpdateQuestionTemplateRequest updateReq =
          buildMinimalUpdateQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "PARAMETERS".equals(w.getCategory())
                          && w.getField() != null
                          && w.getField().contains("unusedCoeff")));

      assertEquivalentFullValidation(createReq, updateReq);

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Normal: giá trị templateText không phải String — dùng toString")
    void it_should_read_template_text_from_non_string_object_values() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = Map.of("vi", Integer.valueOf(42));
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertNotNull(response);
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "PARAMETERS".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("not used")));

      // ===== VERIFY =====
    }
  }

  @Nested
  @DisplayName("validateTemplate() / validateTemplateUpdate() — parameters")
  class ParametersValidationTests {

    @Test
    @DisplayName("Abnormal: không có tham số")
    void it_should_add_error_when_parameters_null_or_empty() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              null,
              "1",
              QuestionType.SHORT_ANSWER,
              null,
              null,
              List.of(QuestionTag.LINEAR_EQUATIONS));
      UpdateQuestionTemplateRequest updateReq =
          buildMinimalUpdateQuestionTemplateRequest(
              templateText,
              Map.of(),
              "1",
              QuestionType.SHORT_ANSWER,
              null,
              null,
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse r1 = templateValidationService.validateTemplate(createReq);
      TemplateValidationResponse r2 = templateValidationService.validateTemplateUpdate(updateReq);

      // ===== ASSERT =====
      assertFalse(r1.getIsValid());
      assertFalse(r2.getIsValid());
      assertTrue(
          r1.getErrors().stream().anyMatch(e -> "PARAMETERS".equals(e.getCategory())));
      assertTrue(
          r2.getErrors().stream().anyMatch(e -> "PARAMETERS".equals(e.getCategory())));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: định nghĩa tham số không phải object")
    void it_should_add_error_when_parameter_definition_is_not_map() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = new LinkedHashMap<>();
      parameters.put("a", buildIntegerParameterDef(1, 10));
      parameters.put("badDef", "Không phải đối tượng JSON");
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      UpdateQuestionTemplateRequest updateReq =
          buildMinimalUpdateQuestionTemplateRequest(
              templateText,
              new LinkedHashMap<>(parameters),
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      assertEquivalentFullValidation(createReq, updateReq);
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "PARAMETERS".equals(e.getCategory())
                          && e.getField() != null
                          && e.getField().contains("badDef")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: thiếu trường type / type không hợp lệ / type hợp lệ decimal và text")
    void it_should_validate_parameter_type_field() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = new LinkedHashMap<>();
      templateText.put("vi", "Giá trị {{a}} {{b}} {{c}}");
      Map<String, Object> noType = new LinkedHashMap<>();
      noType.put("min", 1);
      noType.put("max", 5);
      Map<String, Object> badType = new LinkedHashMap<>();
      badType.put("type", "fraction");
      badType.put("min", 1);
      badType.put("max", 5);
      Map<String, Object> decimalDef = new LinkedHashMap<>();
      decimalDef.put("type", "decimal");
      decimalDef.put("min", 0);
      decimalDef.put("max", 1);
      Map<String, Object> textDef = new LinkedHashMap<>();
      textDef.put("type", "text");
      textDef.put("min", 0);
      textDef.put("max", 1);
      Map<String, Object> parameters = new LinkedHashMap<>();
      parameters.put("a", noType);
      parameters.put("b", badType);
      parameters.put("c", decimalDef);
      parameters.put("d", textDef);
      templateText.put("en", "Values {{a}} {{b}} {{c}} {{d}}");
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + b + c + d",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      long typeErrors =
          response.getErrors().stream()
              .filter(e -> "PARAMETERS".equals(e.getCategory()) && e.getField() != null)
              .filter(e -> e.getField().contains(".type"))
              .count();
      assertTrue(typeErrors >= 2);

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: thiếu min hoặc max")
    void it_should_warn_when_min_or_max_missing() {
      // ===== ARRANGE =====
      Map<String, Object> templateText =
          new LinkedHashMap<>(
              Map.of(
                  "vi",
                  "Hai tham chiếu {{a}} và {{b}}",
                  "en",
                  "Two references {{a}} and {{b}}"));
      Map<String, Object> onlyMin = new LinkedHashMap<>();
      onlyMin.put("type", "integer");
      onlyMin.put("min", 1);
      Map<String, Object> onlyMax = new LinkedHashMap<>();
      onlyMax.put("type", "integer");
      onlyMax.put("max", 10);
      Map<String, Object> parameters = new LinkedHashMap<>();
      parameters.put("a", onlyMin);
      parameters.put("b", onlyMax);
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + b",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0", "b > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(w -> w.getField() != null && w.getField().endsWith(".min")));
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(w -> w.getField() != null && w.getField().endsWith(".max")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: min không nhỏ hơn max")
    void it_should_add_error_when_min_not_less_than_max() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(10, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "PARAMETERS".equals(e.getCategory())
                          && e.getMessage() != null
                          && e.getMessage().contains("min")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: khoảng min-max quá lớn")
    void it_should_warn_when_parameter_range_exceeds_reasonable_span() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(0, 20000));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a >= 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "PARAMETERS".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("large range")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: min/max không phải số")
    void it_should_add_error_when_min_max_not_numeric() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> def = new LinkedHashMap<>();
      def.put("type", "integer");
      def.put("min", "mot");
      def.put("max", "hai");
      Map<String, Object> parameters = Map.of("a", def);
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"true"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "PARAMETERS".equals(e.getCategory())
                          && e.getMessage() != null
                          && e.getMessage().contains("numeric")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Info: exclude có phần tử và step được khai báo")
    void it_should_add_info_for_exclude_list_and_step() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> def = new LinkedHashMap<>();
      def.put("type", "integer");
      def.put("min", 1);
      def.put("max", 20);
      def.put("exclude", List.of(0, 5));
      def.put("step", 1);
      Map<String, Object> parameters = Map.of("a", def);
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getInfo().stream()
              .anyMatch(i -> i.getField() != null && i.getField().contains("exclude")));
      assertTrue(
          response.getInfo().stream()
              .anyMatch(i -> i.getField() != null && i.getField().contains("step")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("exclude rỗng không tạo info")
    void it_should_not_add_info_when_exclude_list_empty() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> def = new LinkedHashMap<>();
      def.put("type", "integer");
      def.put("min", 1);
      def.put("max", 10);
      def.put("exclude", new ArrayList<Integer>());
      Map<String, Object> parameters = Map.of("a", def);
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(
          response.getInfo().stream()
              .anyMatch(i -> i.getField() != null && i.getField().contains("exclude")));

      // ===== VERIFY =====
    }
  }

  @Nested
  @DisplayName("validateTemplate() / validateTemplateUpdate() — answer formula")
  class AnswerFormulaValidationTests {

    @Test
    @DisplayName("Abnormal: công thức trống")
    void it_should_add_error_when_answer_formula_blank() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "   ",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(e -> "FORMULA".equals(e.getCategory()) && e.getField().contains("answer")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Normal: có ngoặc thì không cảnh báo precedence khi lẫn + và *")
    void it_should_not_warn_precedence_when_parentheses_clarify_mixed_operators() {
      // ===== ARRANGE =====
      Map<String, Object> templateText =
          new LinkedHashMap<>(
              Map.of(
                  "vi",
                  "Biểu thức {{a}} {{b}} {{c}}",
                  "en",
                  "Expression {{a}} {{b}} {{c}}"));
      Map<String, Object> parameters = new LinkedHashMap<>();
      parameters.put("a", buildIntegerParameterDef(1, 5));
      parameters.put("b", buildIntegerParameterDef(1, 5));
      parameters.put("c", buildIntegerParameterDef(1, 5));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "(a + b) * c",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0", "b > 0", "c > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "FORMULA".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("parentheses")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: công thức tính ra null")
    void it_should_add_error_when_formula_evaluates_to_null() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "null",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "FORMULA".equals(e.getCategory())
                          && e.getMessage() != null
                          && e.getMessage().contains("null")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: cộng trừ lẫn nhân chia thiếu dấu ngoặc")
    void it_should_warn_when_mixed_precedence_without_parentheses() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + b * c",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      Map<String, Object> extendedParams = new LinkedHashMap<>();
      extendedParams.put("a", buildIntegerParameterDef(1, 5));
      extendedParams.put("b", buildIntegerParameterDef(1, 5));
      extendedParams.put("c", buildIntegerParameterDef(1, 5));
      createReq.setParameters(extendedParams);
      createReq.setTemplateText(
          Map.of(
              "vi",
              "Biểu thức {{a}} {{b}} {{c}}",
              "en",
              "Expression {{a}} {{b}} {{c}}"));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "FORMULA".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("parentheses")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: phép chia theo tham số")
    void it_should_warn_on_division_by_parameter_token() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(2, 20));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "100 / a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a != 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "FORMULA".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("divides")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: công thức sai cú pháp")
    void it_should_add_error_when_formula_evaluation_fails() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + + +",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "FORMULA".equals(e.getCategory())
                          && e.getMessage() != null
                          && e.getMessage().contains("Invalid formula")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: công thức dùng tham số chưa định nghĩa")
    void it_should_add_error_when_formula_uses_unknown_parameter() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + unknownToken",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "FORMULA".equals(e.getCategory())
                          && e.getMessage() != null
                          && e.getMessage().contains("undefined parameter")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: kết quả công thức không phải số")
    void it_should_warn_when_formula_result_not_number() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "'ketQuaChuoi'",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "FORMULA".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("not a number")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Từ khóa JavaScript trong công thức không bị coi là tham số")
    void it_should_ignore_javascript_keywords_in_formula_parameter_scan() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "true ? a : 0",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "FORMULA".equals(e.getCategory())
                          && e.getMessage() != null
                          && e.getMessage().contains("true")));

      // ===== VERIFY =====
    }
  }

  @Nested
  @DisplayName("validateTemplate() / validateTemplateUpdate() — constraints")
  class ConstraintsValidationTests {

    @Test
    @DisplayName("Warning: không có ràng buộc")
    void it_should_warn_when_constraints_missing() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText, parameters, "a + 1", QuestionType.SHORT_ANSWER, null, null,
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(w -> "CONSTRAINTS".equals(w.getCategory())));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: ràng buộc rỗng theo chỉ số")
    void it_should_warn_when_constraint_entry_is_blank() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      String[] constraints = new String[] {"a > 0", "", "answer > 0"};
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              constraints,
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "CONSTRAINTS".equals(w.getCategory())
                          && w.getField() != null
                          && w.getField().contains("[1]")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Abnormal: cú pháp ràng buộc không hợp lệ")
    void it_should_add_error_when_constraint_syntax_invalid() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a ==== 1"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(
                  e ->
                      "CONSTRAINTS".equals(e.getCategory())
                          && e.getMessage() != null
                          && e.getMessage().contains("Invalid constraint")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: biểu thức ràng buộc không trả về boolean")
    void it_should_warn_when_constraint_does_not_evaluate_to_boolean() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a + 1"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "CONSTRAINTS".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("boolean")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Ràng buộc hợp lệ trả về boolean — không báo lỗi cú pháp")
    void it_should_accept_boolean_constraint_when_syntax_valid() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0", "answer >= 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(
          response.getErrors().stream()
              .anyMatch(e -> "CONSTRAINTS".equals(e.getCategory())));

      // ===== VERIFY =====
    }
  }

  @Nested
  @DisplayName("validateTemplate() / validateTemplateUpdate() — options generator")
  class OptionsGeneratorValidationTests {

    @Test
    @DisplayName("Abnormal: MULTIPLE_CHOICE thiếu optionsGenerator")
    void it_should_add_error_when_mcq_missing_options_generator() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.MULTIPLE_CHOICE,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(
          response.getErrors().stream()
              .anyMatch(e -> "OPTIONS_GENERATOR".equals(e.getCategory())));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: thiếu count / count ngoài khoảng 2–6 / count hợp lệ")
    void it_should_warn_on_options_count_configuration() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      Map<String, Object> genNoCount = new LinkedHashMap<>();
      genNoCount.put("distractors", List.of("Sai dấu", "Nhầm hệ số"));
      Map<String, Object> genLowCount = new LinkedHashMap<>();
      genLowCount.put("count", 1);
      genLowCount.put("distractors", List.of("Loi A"));
      Map<String, Object> genOk = new LinkedHashMap<>();
      genOk.put("count", 4);
      genOk.put("distractors", List.of("Loi B", "Loi C"));
      genOk.put("type", "numeric");

      QuestionTemplateRequest reqNoCount =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.MULTIPLE_CHOICE,
              genNoCount,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      QuestionTemplateRequest reqLow =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.MULTIPLE_CHOICE,
              genLowCount,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      QuestionTemplateRequest reqOk =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.MULTIPLE_CHOICE,
              genOk,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse r1 = templateValidationService.validateTemplate(reqNoCount);
      TemplateValidationResponse r2 = templateValidationService.validateTemplate(reqLow);
      TemplateValidationResponse r3 = templateValidationService.validateTemplate(reqOk);

      // ===== ASSERT =====
      assertTrue(
          r1.getWarnings().stream()
              .anyMatch(w -> w.getField() != null && w.getField().contains("count")));
      assertTrue(
          r2.getWarnings().stream()
              .anyMatch(
                  w ->
                      w.getMessage() != null
                          && w.getMessage().contains("between 2 and 6")));
      assertFalse(
          r3.getWarnings().stream()
              .anyMatch(
                  w ->
                      w.getField() != null
                          && w.getField().contains("count")
                          && w.getMessage() != null
                          && w.getMessage().contains("between 2 and 6")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: không có distractors / list rỗng / có distractors — info")
    void it_should_handle_distractors_presence_and_list_size() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      Map<String, Object> noDistractors = new LinkedHashMap<>();
      noDistractors.put("count", 4);
      Map<String, Object> emptyDistractors = new LinkedHashMap<>();
      emptyDistractors.put("count", 4);
      emptyDistractors.put("distractors", new ArrayList<String>());
      Map<String, Object> withDistractors = new LinkedHashMap<>();
      withDistractors.put("count", 4);
      withDistractors.put("distractors", List.of("Quên chia", "Nhầm dấu"));
      withDistractors.put("type", "around_answer");

      QuestionTemplateRequest rNo =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.MULTIPLE_CHOICE,
              noDistractors,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      QuestionTemplateRequest rEmpty =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.MULTIPLE_CHOICE,
              emptyDistractors,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));
      QuestionTemplateRequest rWith =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.MULTIPLE_CHOICE,
              withDistractors,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse vNo = templateValidationService.validateTemplate(rNo);
      TemplateValidationResponse vEmpty = templateValidationService.validateTemplate(rEmpty);
      TemplateValidationResponse vWith = templateValidationService.validateTemplate(rWith);

      // ===== ASSERT =====
      assertTrue(
          vNo.getWarnings().stream()
              .anyMatch(
                  w ->
                      w.getField() != null && w.getField().contains("distractors")));
      assertTrue(
          vEmpty.getWarnings().stream()
              .anyMatch(w -> w.getMessage() != null && w.getMessage().contains("empty")));
      assertTrue(
          vWith.getInfo().stream()
              .anyMatch(i -> i.getMessage() != null && i.getMessage().contains("distractor")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: type options không phải numeric hoặc around_answer")
    void it_should_warn_when_options_type_not_recommended_for_math() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      Map<String, Object> gen = new LinkedHashMap<>();
      gen.put("count", 4);
      gen.put("distractors", List.of("A"));
      gen.put("type", "alphabetic");
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.MULTIPLE_CHOICE,
              gen,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      w.getField() != null
                          && w.getField().contains("type")
                          && w.getMessage() != null
                          && w.getMessage().contains("numeric")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: optionsGenerator có nhưng không phải câu trắc nghiệm")
    void it_should_warn_when_options_generator_present_for_non_mcq() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      Map<String, Object> gen = Map.of("count", 4);
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              gen,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "OPTIONS_GENERATOR".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("ignored")));

      // ===== VERIFY =====
    }
  }

  @Nested
  @DisplayName("validateTemplate() / validateTemplateUpdate() — tags")
  class TagsValidationTests {

    @Test
    @DisplayName("Warning: không có tag")
    void it_should_warn_when_tags_missing_or_empty() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of());

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(w -> "TAGS".equals(w.getCategory())));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Warning: phần tử tag null")
    void it_should_warn_when_tag_list_contains_null_entries() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      List<QuestionTag> tags = new ArrayList<>();
      tags.add(QuestionTag.QUADRATIC_EQUATIONS);
      tags.add(null);
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              tags);

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getWarnings().stream()
              .anyMatch(
                  w ->
                      "TAGS".equals(w.getCategory())
                          && w.getMessage() != null
                          && w.getMessage().contains("null")));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Info: số lượng tag")
    void it_should_add_info_message_for_tag_count() {
      // ===== ARRANGE =====
      Map<String, Object> templateText = buildValidTemplateTextWithPlaceholder("a");
      Map<String, Object> parameters = Map.of("a", buildIntegerParameterDef(1, 10));
      QuestionTemplateRequest createReq =
          buildMinimalQuestionTemplateRequest(
              templateText,
              parameters,
              "a",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS, QuestionTag.INEQUALITIES));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.validateTemplate(createReq);

      // ===== ASSERT =====
      assertTrue(
          response.getInfo().stream()
              .anyMatch(
                  i ->
                      "TAGS".equals(i.getCategory())
                          && i.getMessage() != null
                          && i.getMessage().contains("tag")));

      // ===== VERIFY =====
    }
  }

  @Nested
  @DisplayName("quickValidate()")
  class QuickValidateTests {

    @Test
    @DisplayName("Hợp lệ nhanh khi đủ templateText, parameters, answerFormula")
    void it_should_return_valid_when_required_fields_present() {
      // ===== ARRANGE =====
      UpdateQuestionTemplateRequest request =
          buildMinimalUpdateQuestionTemplateRequest(
              buildValidTemplateTextWithPlaceholder("a"),
              Map.of("a", buildIntegerParameterDef(1, 10)),
              "(a + 3) / 2",
              QuestionType.SHORT_ANSWER,
              null,
              new String[] {"a > 0"},
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.quickValidate(request);

      // ===== ASSERT =====
      assertNotNull(response);
      assertTrue(response.getIsValid());
      assertEquals(0, response.getErrorCount());
      assertEquals(0, response.getWarningCount());

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Lỗi khi thiếu templateText hoặc parameters hoặc answerFormula")
    void it_should_accumulate_errors_when_multiple_required_fields_missing() {
      // ===== ARRANGE =====
      UpdateQuestionTemplateRequest request =
          UpdateQuestionTemplateRequest.builder()
              .name("Kiểm tra nhanh mẫu câu hỏi")
              .description("Payload thiếu trường bắt buộc")
              .templateType(QuestionType.SHORT_ANSWER)
              .templateText(null)
              .parameters(null)
              .answerFormula("   ")
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .tags(List.of(QuestionTag.STATISTICS))
              .build();

      // ===== ACT =====
      TemplateValidationResponse response = templateValidationService.quickValidate(request);

      // ===== ASSERT =====
      assertFalse(response.getIsValid());
      assertTrue(response.getErrorCount() >= 3);
      assertTrue(
          response.getErrors().stream().anyMatch(e -> "templateText".equals(e.getField())));
      assertTrue(response.getErrors().stream().anyMatch(e -> "parameters".equals(e.getField())));
      assertTrue(
          response.getErrors().stream().anyMatch(e -> "answerFormula".equals(e.getField())));

      // ===== VERIFY =====
    }

    @Test
    @DisplayName("Lỗi khi templateText rỗng hoặc parameters rỗng map")
    void it_should_reject_empty_template_text_or_empty_parameter_map() {
      // ===== ARRANGE =====
      UpdateQuestionTemplateRequest emptyText =
          buildMinimalUpdateQuestionTemplateRequest(
              Map.of(),
              Map.of("a", buildIntegerParameterDef(1, 5)),
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              null,
              List.of(QuestionTag.LINEAR_EQUATIONS));
      UpdateQuestionTemplateRequest emptyParams =
          buildMinimalUpdateQuestionTemplateRequest(
              buildValidTemplateTextWithPlaceholder("a"),
              Map.of(),
              "a + 1",
              QuestionType.SHORT_ANSWER,
              null,
              null,
              List.of(QuestionTag.LINEAR_EQUATIONS));

      // ===== ACT =====
      TemplateValidationResponse rText = templateValidationService.quickValidate(emptyText);
      TemplateValidationResponse rParams = templateValidationService.quickValidate(emptyParams);

      // ===== ASSERT =====
      assertFalse(rText.getIsValid());
      assertFalse(rParams.getIsValid());

      // ===== VERIFY =====
    }
  }

  @Nested
  @DisplayName("validateTemplateUpdate() — smoke")
  class ValidateTemplateUpdateSmokeTests {

    @Test
    @DisplayName("Cập nhật mẫu hợp lệ cơ bản")
    void it_should_return_valid_for_well_formed_update_request() {
      // ===== ARRANGE =====
      Map<String, Object> options = new LinkedHashMap<>();
      options.put("count", 4);
      options.put("distractors", List.of("Nham tich phan", "Nham cong"));
      options.put("type", "numeric");
      UpdateQuestionTemplateRequest request =
          buildMinimalUpdateQuestionTemplateRequest(
              buildValidTemplateTextWithPlaceholder("coeff"),
              Map.of("coeff", buildIntegerParameterDef(2, 12)),
              "coeff * 2",
              QuestionType.MULTIPLE_CHOICE,
              options,
              new String[] {"coeff > 0"},
              List.of(QuestionTag.DERIVATIVES));

      // ===== ACT =====
      TemplateValidationResponse response =
          templateValidationService.validateTemplateUpdate(request);

      // ===== ASSERT =====
      assertNotNull(response);
      assertNotNull(response.getErrors());
      assertNotNull(response.getWarnings());

      // ===== VERIFY =====
    }
  }
}
