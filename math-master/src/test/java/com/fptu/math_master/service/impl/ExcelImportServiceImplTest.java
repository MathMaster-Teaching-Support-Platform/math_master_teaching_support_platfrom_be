package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.QuestionTemplateBatchImportRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.ExcelPreviewResponse;
import com.fptu.math_master.dto.response.TemplateBatchImportResponse;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionTag;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.util.SecurityUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("ExcelImportServiceImpl - Tests")
@SuppressWarnings("unchecked")
class ExcelImportServiceImplTest extends BaseUnitTest {

  @InjectMocks private ExcelImportServiceImpl excelImportService;

  @Mock private QuestionTemplateRepository questionTemplateRepository;
  @Mock private Validator validator;

  /**
   * Real ObjectMapper — JSON parsing is plain library code we want to exercise
   * for real, not stub call-by-call. Tests can still spy on it via Mockito if
   * they need to assert call counts.
   */
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  // Column layout mirrors ExcelImportServiceImpl
  private static final int COL_NAME = 0;
  private static final int COL_DESCRIPTION = 1;
  private static final int COL_TEMPLATE_TYPE = 2;
  private static final int COL_TEMPLATE_TEXT_VI = 3;
  private static final int COL_PARAMETERS = 4;
  private static final int COL_ANSWER_FORMULA = 5;
  private static final int COL_DIAGRAM_TEMPLATE = 6;
  private static final int COL_SOLUTION_STEPS_TEMPLATE = 7;
  private static final int COL_OPTIONS_GENERATOR = 8;
  private static final int COL_STATEMENT_MUTATIONS = 9;
  private static final int COL_COGNITIVE_LEVEL = 10;
  private static final int COL_TAGS = 11;
  private static final int COL_IS_PUBLIC = 12;
  private static final int COLUMN_COUNT = 13;

  private static final String[] HEADERS = {
    "name",
    "description",
    "templateType",
    "templateText_vi",
    "parameters",
    "answerFormula",
    "diagramTemplate",
    "solutionStepsTemplate",
    "optionsGenerator",
    "statementMutations",
    "cognitiveLevel",
    "tags",
    "isPublic"
  };

  /** Header row + notes row + first data row. Mirrors FIRST_DATA_ROW = 2 in service. */
  private static final int FIRST_DATA_ROW = 2;

  private static final UUID CURRENT_USER_ID =
      UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private QuestionTemplateRequest buildTemplateRequest(
      String name, QuestionType questionType, CognitiveLevel cognitiveLevel) {
    QuestionTemplateRequest.QuestionTemplateRequestBuilder b =
        QuestionTemplateRequest.builder()
            .name(name)
            .description("Mau mo ta cho " + name)
            .templateType(questionType)
            .templateText(Map.of("vi", "Noi dung cau hoi cho " + name))
            .parameters(Map.of("a", Map.of("type", "int", "min", 1, "max", 10)))
            .cognitiveLevel(cognitiveLevel)
            .tags(List.of(QuestionTag.LINEAR_EQUATIONS))
            .isPublic(true);
    if (questionType == QuestionType.MULTIPLE_CHOICE || questionType == QuestionType.SHORT_ANSWER) {
      b.answerFormula("(-b)/a");
    }
    if (questionType == QuestionType.MULTIPLE_CHOICE) {
      b.optionsGenerator(Map.of("A", "1", "B", "2", "C", "3", "D", "4"));
    }
    if (questionType == QuestionType.TRUE_FALSE) {
      b.statementMutations(
          Map.of(
              "clauseTemplates",
              List.of(
                  Map.of("text", "Mệnh đề A", "truthValue", true),
                  Map.of("text", "Mệnh đề B", "truthValue", false))));
    }
    return b.build();
  }

  private QuestionTemplate buildSavedTemplate(
      UUID id,
      String name,
      QuestionType questionType,
      CognitiveLevel cognitiveLevel,
      Boolean isPublic,
      UUID createdBy) {
    QuestionTemplate template =
        QuestionTemplate.builder()
            .name(name)
            .description("Mo ta " + name)
            .templateType(questionType)
            .templateText(Map.of("vi", "Cau hoi " + name))
            .parameters(Map.of("x", 2))
            .answerFormula("x + 1")
            .diagramTemplate("tikz")
            .optionsGenerator(Map.of("A", "1"))
            .cognitiveLevel(cognitiveLevel)
            .tags(List.of(QuestionTag.LINEAR_EQUATIONS))
            .isPublic(isPublic)
            .status(TemplateStatus.DRAFT)
            .usageCount(0)
            .avgSuccessRate(new BigDecimal("87.50"))
            .questionBankId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .canonicalQuestionId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .build();
    template.setId(id);
    template.setCreatedBy(createdBy);
    template.setCreatedAt(Instant.parse("2025-01-01T10:15:30Z"));
    template.setUpdatedAt(Instant.parse("2025-01-02T10:15:30Z"));
    return template;
  }

  /**
   * Build a workbook with header row (0), an empty notes row (1), and data rows
   * starting at row 2. Mirrors the layout the service writes / expects.
   */
  private byte[] createWorkbookBytes(RowWriter rowWriter) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Question Templates");
      Row header = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        header.createCell(i).setCellValue(HEADERS[i]);
      }
      sheet.createRow(1); // notes row, intentionally blank in tests
      rowWriter.write(sheet);
      workbook.write(out);
      return out.toByteArray();
    }
  }

  @FunctionalInterface
  private interface RowWriter {
    void write(Sheet sheet);
  }

  private Object invokePrivateMethod(String methodName, Class<?>[] paramTypes, Object... args)
      throws Exception {
    Method method = ExcelImportServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
    method.setAccessible(true);
    return method.invoke(excelImportService, args);
  }

  @Nested
  @DisplayName("previewExcelImport()")
  class PreviewExcelImportTests {

    @Test
    void it_should_return_valid_preview_when_excel_contains_valid_mcq_row() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("Phuong trinh bac nhat");
                row.createCell(COL_DESCRIPTION).setCellValue("Mo ta phuong trinh bac nhat");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("MULTIPLE_CHOICE");
                row.createCell(COL_TEMPLATE_TEXT_VI)
                    .setCellValue("Giai phuong trinh {{a}}x + {{b}} = 0");
                row.createCell(COL_PARAMETERS)
                    .setCellValue(
                        "{\"a\":{\"type\":\"int\",\"min\":1,\"max\":10},"
                            + "\"b\":{\"type\":\"int\",\"min\":-10,\"max\":10}}");
                row.createCell(COL_ANSWER_FORMULA).setCellValue("(-b)/a");
                row.createCell(COL_OPTIONS_GENERATOR)
                    .setCellValue("{\"A\":\"(-b)/a\",\"B\":\"b/a\",\"C\":\"-a\",\"D\":\"a+b\"}");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                row.createCell(COL_TAGS).setCellValue("LINEAR_EQUATIONS, TRIANGLES");
                row.createCell(COL_IS_PUBLIC).setCellValue("yes");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "question-template.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);
      when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of());

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertNotNull(result);
      assertEquals(1, result.getTotalRows());
      assertEquals(1, result.getValidRows());
      assertEquals(0, result.getInvalidRows());
      ExcelPreviewResponse.PreviewRow previewRow = result.getRows().get(0);
      assertAll(
          () -> assertEquals(FIRST_DATA_ROW + 1, previewRow.getRowNumber()),
          () -> assertTrue(previewRow.getIsValid()),
          () -> assertNull(previewRow.getValidationErrors()),
          () -> assertEquals(QuestionType.MULTIPLE_CHOICE, previewRow.getData().getTemplateType()),
          () -> assertEquals(CognitiveLevel.THONG_HIEU, previewRow.getData().getCognitiveLevel()),
          () -> assertTrue(previewRow.getData().getIsPublic()),
          () -> assertEquals(2, previewRow.getData().getTags().size()),
          () -> assertNotNull(previewRow.getData().getOptionsGenerator()),
          () -> assertEquals(4, previewRow.getData().getOptionsGenerator().size()));

      verify(validator, times(1)).validate(any(QuestionTemplateRequest.class));
    }

    @Test
    void it_should_return_valid_preview_when_excel_contains_true_false_row() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("BBT bac ba");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("TRUE_FALSE");
                row.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("Cho hàm số có BBT...");
                row.createCell(COL_PARAMETERS).setCellValue("{\"x1\":1}");
                row.createCell(COL_DIAGRAM_TEMPLATE).setCellValue("\\begin{tikzpicture}...");
                row.createCell(COL_STATEMENT_MUTATIONS)
                    .setCellValue(
                        "{\"clauseTemplates\":["
                            + "{\"text\":\"Mệnh đề A\",\"truthValue\":true},"
                            + "{\"text\":\"Mệnh đề B\",\"truthValue\":false}"
                            + "]}");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                row.createCell(COL_TAGS).setCellValue("FUNCTIONS");
                row.createCell(COL_IS_PUBLIC).setCellValue("TRUE");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "tf-template.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);
      when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of());

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getValidRows());
      QuestionTemplateRequest data = result.getRows().get(0).getData();
      assertAll(
          () -> assertEquals(QuestionType.TRUE_FALSE, data.getTemplateType()),
          () -> assertNotNull(data.getStatementMutations()),
          () -> assertTrue(data.getStatementMutations().get("clauseTemplates") instanceof List<?>),
          () ->
              assertTrue(
                  ((List<?>) data.getStatementMutations().get("clauseTemplates")).size() == 2));
    }

    @Test
    void it_should_return_valid_preview_when_excel_contains_short_answer_row() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("Tich phan");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("SHORT_ANSWER");
                row.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("Tinh I = ...");
                row.createCell(COL_PARAMETERS).setCellValue("{\"a\":1}");
                row.createCell(COL_ANSWER_FORMULA).setCellValue("((a*c+b)^4 - b^4) / (4*a)");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("VAN_DUNG");
                row.createCell(COL_TAGS).setCellValue("INTEGRALS");
                row.createCell(COL_IS_PUBLIC).setCellValue("FALSE");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "sa-template.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);
      when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of());

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getValidRows());
      QuestionTemplateRequest data = result.getRows().get(0).getData();
      assertAll(
          () -> assertEquals(QuestionType.SHORT_ANSWER, data.getTemplateType()),
          () -> assertEquals("((a*c+b)^4 - b^4) / (4*a)", data.getAnswerFormula()),
          () -> assertNull(data.getOptionsGenerator()),
          () -> assertNull(data.getStatementMutations()),
          () -> assertFalse(data.getIsPublic()));
    }

    @Test
    void it_should_mark_mcq_row_invalid_when_options_generator_is_missing() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("MCQ thieu options");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("MULTIPLE_CHOICE");
                row.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("...");
                row.createCell(COL_PARAMETERS).setCellValue("{\"x\":1}");
                row.createCell(COL_ANSWER_FORMULA).setCellValue("x");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                row.createCell(COL_TAGS).setCellValue("FUNCTIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "missing-options.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);
      when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of());

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getInvalidRows());
      assertTrue(
          result.getRows().get(0).getValidationErrors().stream()
              .anyMatch(msg -> msg.contains("optionsGenerator: required for MULTIPLE_CHOICE")));
    }

    @Test
    void it_should_mark_tf_row_invalid_when_statement_mutations_missing() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("TF thieu clauses");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("TRUE_FALSE");
                row.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("...");
                row.createCell(COL_PARAMETERS).setCellValue("{\"x\":1}");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                row.createCell(COL_TAGS).setCellValue("FUNCTIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "tf-missing-clauses.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);
      when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of());

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getInvalidRows());
      assertTrue(
          result.getRows().get(0).getValidationErrors().stream()
              .anyMatch(msg -> msg.contains("statementMutations: required for TRUE_FALSE")));
    }

    @Test
    void it_should_skip_empty_row_and_mark_invalid_template_type_row() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                sheet.createRow(FIRST_DATA_ROW); // empty
                Row invalid = sheet.createRow(FIRST_DATA_ROW + 1);
                invalid.createCell(COL_NAME).setCellValue("Loai sai");
                invalid.createCell(COL_TEMPLATE_TYPE).setCellValue("KHONG_HOP_LE");
                invalid.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("...");
                invalid.createCell(COL_PARAMETERS).setCellValue("{\"a\":1}");
                invalid.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                invalid.createCell(COL_TAGS).setCellValue("LINEAR_EQUATIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "invalid-type.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getTotalRows());
      assertEquals(0, result.getValidRows());
      assertEquals(1, result.getInvalidRows());
      ExcelPreviewResponse.PreviewRow row = result.getRows().get(0);
      assertAll(
          () -> assertEquals(FIRST_DATA_ROW + 2, row.getRowNumber()),
          () -> assertFalse(row.getIsValid()),
          () ->
              assertTrue(
                  row.getValidationErrors().get(0).contains("Invalid templateType: KHONG_HOP_LE")));
      verify(validator, never()).validate(any(QuestionTemplateRequest.class));
    }

    @Test
    void
        it_should_collect_validation_errors_when_request_has_constraint_violation_and_missing_fields()
            throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("Bai toan");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("SHORT_ANSWER");
                row.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("");
                row.createCell(COL_PARAMETERS).setCellValue("");
                row.createCell(COL_ANSWER_FORMULA).setCellValue("x+1");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("VAN_DUNG");
                row.createCell(COL_TAGS).setCellValue("LINEAR_EQUATIONS");
                row.createCell(COL_IS_PUBLIC).setCellValue("1");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "validation-error.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);

      ConstraintViolation<QuestionTemplateRequest> violation =
          (ConstraintViolation<QuestionTemplateRequest>) Mockito.mock(ConstraintViolation.class);
      Path path = Mockito.mock(Path.class);
      when(path.toString()).thenReturn("name");
      when(violation.getPropertyPath()).thenReturn(path);
      when(violation.getMessage()).thenReturn("Template name is required");
      when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of(violation));

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getInvalidRows());
      List<String> errors = result.getRows().get(0).getValidationErrors();
      assertNotNull(errors);
      assertTrue(errors.contains("name: Template name is required"));
      assertTrue(errors.contains("templateText: Template text is required"));
      assertTrue(errors.contains("parameters: Parameters are required"));
    }

    @Test
    void it_should_throw_invalid_key_when_excel_file_is_empty_or_wrong_extension_or_too_large()
        throws Exception {
      MockMultipartFile emptyFile =
          new MockMultipartFile(
              "file",
              "empty.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              new byte[0]);
      MockMultipartFile wrongExtensionFile =
          new MockMultipartFile("file", "templates.csv", "text/csv", "name,description".getBytes());
      byte[] validSmallWorkbook = createWorkbookBytes(sheet -> {});
      byte[] oversizedContent = new byte[10 * 1024 * 1024 + 1];
      System.arraycopy(validSmallWorkbook, 0, oversizedContent, 0, validSmallWorkbook.length);
      MockMultipartFile oversizedFile =
          new MockMultipartFile(
              "file",
              "large.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              oversizedContent);

      AppException emptyEx =
          assertThrows(AppException.class, () -> excelImportService.previewExcelImport(emptyFile));
      AppException wrongExtEx =
          assertThrows(
              AppException.class, () -> excelImportService.previewExcelImport(wrongExtensionFile));
      AppException oversizeEx =
          assertThrows(
              AppException.class, () -> excelImportService.previewExcelImport(oversizedFile));

      assertAll(
          () -> assertEquals(ErrorCode.INVALID_KEY, emptyEx.getErrorCode()),
          () -> assertEquals(ErrorCode.INVALID_KEY, wrongExtEx.getErrorCode()),
          () -> assertEquals(ErrorCode.INVALID_KEY, oversizeEx.getErrorCode()));
    }

    @Test
    void it_should_mark_row_invalid_when_cognitive_level_is_not_supported() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("Bai tap");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("SHORT_ANSWER");
                row.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("...");
                row.createCell(COL_PARAMETERS).setCellValue("{\"x\":1}");
                row.createCell(COL_ANSWER_FORMULA).setCellValue("x");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("KHONG_TON_TAI");
                row.createCell(COL_TAGS).setCellValue("LINEAR_EQUATIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "invalid-cognitive-level.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getInvalidRows());
      assertTrue(
          result.getRows().get(0).getValidationErrors().get(0).contains("Invalid cognitiveLevel"));
    }

    @Test
    void it_should_mark_row_invalid_when_parameters_json_is_malformed() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("Bai toan thong ke");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("SHORT_ANSWER");
                row.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("...");
                row.createCell(COL_PARAMETERS).setCellValue("{invalid-json}");
                row.createCell(COL_ANSWER_FORMULA).setCellValue("x");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                row.createCell(COL_TAGS).setCellValue("LINEAR_EQUATIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "invalid-parameters-json.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getInvalidRows());
      assertTrue(
          result.getRows().get(0).getValidationErrors().get(0).contains("Invalid parameters JSON"));
    }

    @Test
    void it_should_mark_row_invalid_when_options_generator_json_is_malformed() throws Exception {
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(FIRST_DATA_ROW);
                row.createCell(COL_NAME).setCellValue("MCQ JSON loi");
                row.createCell(COL_TEMPLATE_TYPE).setCellValue("MULTIPLE_CHOICE");
                row.createCell(COL_TEMPLATE_TEXT_VI).setCellValue("...");
                row.createCell(COL_PARAMETERS).setCellValue("{\"x\":1}");
                row.createCell(COL_ANSWER_FORMULA).setCellValue("x");
                row.createCell(COL_OPTIONS_GENERATOR).setCellValue("{invalid-options}");
                row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                row.createCell(COL_TAGS).setCellValue("LINEAR_EQUATIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "invalid-options-json.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);

      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      assertEquals(1, result.getInvalidRows());
      assertTrue(
          result
              .getRows()
              .get(0)
              .getValidationErrors()
              .get(0)
              .contains("Invalid optionsGenerator JSON"));
    }

    @Test
    void it_should_throw_invalid_key_when_excel_content_is_corrupted() {
      MockMultipartFile corruptedFile =
          new MockMultipartFile(
              "file",
              "corrupted.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              "this-is-not-an-xlsx-content".getBytes());

      AppException exception =
          assertThrows(
              AppException.class, () -> excelImportService.previewExcelImport(corruptedFile));
      assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
    }

    @Test
    void it_should_throw_invalid_key_when_filename_is_null() {
      org.springframework.web.multipart.MultipartFile file =
          Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
      when(file.isEmpty()).thenReturn(false);
      when(file.getOriginalFilename()).thenReturn(null);

      AppException exception =
          assertThrows(AppException.class, () -> excelImportService.previewExcelImport(file));
      assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
    }

    @Test
    void it_should_handle_all_cell_types_when_getting_cell_value_as_string() throws Exception {
      try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("test");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("ALPHA");
        row.createCell(1).setCellValue(123D);
        row.createCell(2).setCellValue(0.25);
        row.createCell(3).setCellValue(true);
        row.createCell(4).setCellFormula("A1");
        row.createCell(5, CellType.BLANK);

        String stringValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 0);
        String integerValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 1);
        String decimalValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 2);
        String booleanValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 3);
        String formulaValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 4);
        String blankValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 5);
        String nullColumnValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, null);

        assertAll(
            () -> assertEquals("ALPHA", stringValue),
            () -> assertEquals("123", integerValue),
            () -> assertEquals("0.25", decimalValue),
            () -> assertEquals("true", booleanValue),
            () -> assertNotNull(formulaValue),
            () -> assertNull(blankValue),
            () -> assertNull(nullColumnValue));
      }
    }
  }

  @Nested
  @DisplayName("importTemplatesBatch()")
  class ImportTemplatesBatchTests {

    @Test
    void it_should_import_all_templates_successfully_when_all_rows_are_valid() {
      QuestionTemplateRequest first =
          buildTemplateRequest(
              "Phuong trinh bac hai", QuestionType.MULTIPLE_CHOICE, CognitiveLevel.NHAN_BIET);
      QuestionTemplateRequest second =
          buildTemplateRequest("Tich phan", QuestionType.SHORT_ANSWER, CognitiveLevel.VAN_DUNG);
      second.setIsPublic(false);
      QuestionTemplateBatchImportRequest request =
          QuestionTemplateBatchImportRequest.builder().templates(List.of(first, second)).build();

      QuestionTemplate savedFirst =
          buildSavedTemplate(
              UUID.fromString("33333333-3333-3333-3333-333333333333"),
              first.getName(),
              first.getTemplateType(),
              first.getCognitiveLevel(),
              true,
              CURRENT_USER_ID);
      QuestionTemplate savedSecond =
          buildSavedTemplate(
              UUID.fromString("44444444-4444-4444-4444-444444444444"),
              second.getName(),
              second.getTemplateType(),
              second.getCognitiveLevel(),
              false,
              CURRENT_USER_ID);
      when(questionTemplateRepository.save(any(QuestionTemplate.class)))
          .thenReturn(savedFirst)
          .thenReturn(savedSecond);

      TemplateBatchImportResponse result;
      try (MockedStatic<SecurityUtils> securityUtilsMock =
          Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);
        result = excelImportService.importTemplatesBatch(request);
        securityUtilsMock.verify(SecurityUtils::getCurrentUserId, times(1));
      }

      assertNotNull(result);
      assertAll(
          () -> assertEquals(2, result.getTotalRows()),
          () -> assertEquals(2, result.getSuccessCount()),
          () -> assertEquals(0, result.getFailedCount()),
          () -> assertNull(result.getErrors()),
          () -> assertEquals(2, result.getSuccessfulTemplates().size()),
          () ->
              assertEquals(
                  TemplateStatus.DRAFT, result.getSuccessfulTemplates().get(0).getStatus()));

      verify(questionTemplateRepository, times(2)).save(any(QuestionTemplate.class));
    }

    @Test
    void it_should_collect_error_details_when_some_template_rows_fail_to_import() {
      QuestionTemplateRequest first =
          buildTemplateRequest(
              "Bai toan toi uu hoa", QuestionType.SHORT_ANSWER, CognitiveLevel.VAN_DUNG_CAO);
      QuestionTemplateRequest second =
          buildTemplateRequest(
              "Do thi ham bac ba", QuestionType.MULTIPLE_CHOICE, CognitiveLevel.THONG_HIEU);
      QuestionTemplateBatchImportRequest request =
          QuestionTemplateBatchImportRequest.builder().templates(List.of(first, second)).build();

      QuestionTemplate savedSecond =
          buildSavedTemplate(
              UUID.fromString("55555555-5555-5555-5555-555555555555"),
              second.getName(),
              second.getTemplateType(),
              second.getCognitiveLevel(),
              true,
              CURRENT_USER_ID);
      when(questionTemplateRepository.save(any(QuestionTemplate.class)))
          .thenThrow(new RuntimeException("Duplicate template name"))
          .thenReturn(savedSecond);

      TemplateBatchImportResponse result;
      try (MockedStatic<SecurityUtils> securityUtilsMock =
          Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);
        result = excelImportService.importTemplatesBatch(request);
      }

      assertNotNull(result);
      assertAll(
          () -> assertEquals(2, result.getTotalRows()),
          () -> assertEquals(1, result.getSuccessCount()),
          () -> assertEquals(1, result.getFailedCount()),
          () -> assertNotNull(result.getErrors()),
          () -> assertEquals("Duplicate template name", result.getErrors().get(0).getMessage()));
    }

    @Test
    void it_should_default_is_public_to_false_when_request_is_public_is_null() {
      QuestionTemplateRequest requestItem =
          buildTemplateRequest(
              "He phuong trinh", QuestionType.SHORT_ANSWER, CognitiveLevel.NHAN_BIET);
      requestItem.setIsPublic(null);
      QuestionTemplateBatchImportRequest request =
          QuestionTemplateBatchImportRequest.builder().templates(List.of(requestItem)).build();

      QuestionTemplate savedTemplate =
          buildSavedTemplate(
              UUID.fromString("66666666-6666-6666-6666-666666666666"),
              requestItem.getName(),
              requestItem.getTemplateType(),
              requestItem.getCognitiveLevel(),
              false,
              CURRENT_USER_ID);
      when(questionTemplateRepository.save(any(QuestionTemplate.class))).thenReturn(savedTemplate);

      TemplateBatchImportResponse result;
      try (MockedStatic<SecurityUtils> securityUtilsMock =
          Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);
        result = excelImportService.importTemplatesBatch(request);
      }

      assertNotNull(result);
      assertEquals(1, result.getSuccessCount());
      verify(questionTemplateRepository, times(1))
          .save(
              Mockito.argThat(
                  entity -> {
                    assertFalse(entity.getIsPublic());
                    assertEquals(TemplateStatus.DRAFT, entity.getStatus());
                    assertEquals(0, entity.getUsageCount());
                    assertEquals(CURRENT_USER_ID, entity.getCreatedBy());
                    return true;
                  }));
    }
  }

  @Nested
  @DisplayName("generateExcelTemplate()")
  class GenerateExcelTemplateTests {

    @Test
    void it_should_generate_excel_template_with_expected_headers_and_three_examples()
        throws Exception {
      byte[] result = excelImportService.generateExcelTemplate();
      assertNotNull(result);
      assertTrue(result.length > 0);

      try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
        Sheet sheet = workbook.getSheet("Question Templates");
        Sheet instructions = workbook.getSheet("Hướng dẫn");
        assertNotNull(sheet);
        assertNotNull(instructions);

        assertAll(
            () -> assertEquals("name", sheet.getRow(0).getCell(COL_NAME).getStringCellValue()),
            () ->
                assertEquals(
                    "templateType",
                    sheet.getRow(0).getCell(COL_TEMPLATE_TYPE).getStringCellValue()),
            () ->
                assertEquals(
                    "solutionStepsTemplate",
                    sheet.getRow(0).getCell(COL_SOLUTION_STEPS_TEMPLATE).getStringCellValue()),
            () ->
                assertEquals(
                    "statementMutations",
                    sheet.getRow(0).getCell(COL_STATEMENT_MUTATIONS).getStringCellValue()),
            () ->
                assertEquals(
                    "isPublic", sheet.getRow(0).getCell(COL_IS_PUBLIC).getStringCellValue()),
            () -> assertEquals(COLUMN_COUNT, sheet.getRow(0).getLastCellNum()),
            // Examples start at row 2
            () ->
                assertEquals(
                    "MULTIPLE_CHOICE",
                    sheet.getRow(2).getCell(COL_TEMPLATE_TYPE).getStringCellValue()),
            () ->
                assertEquals(
                    "TRUE_FALSE", sheet.getRow(3).getCell(COL_TEMPLATE_TYPE).getStringCellValue()),
            () ->
                assertEquals(
                    "SHORT_ANSWER",
                    sheet.getRow(4).getCell(COL_TEMPLATE_TYPE).getStringCellValue()),
            // Geometry MCQ has TikZ
            () ->
                assertTrue(
                    sheet
                        .getRow(2)
                        .getCell(COL_DIAGRAM_TEMPLATE)
                        .getStringCellValue()
                        .contains("\\begin{tikzpicture}")),
            // TF has \tkzTabInit + clauseTemplates
            () ->
                assertTrue(
                    sheet
                        .getRow(3)
                        .getCell(COL_DIAGRAM_TEMPLATE)
                        .getStringCellValue()
                        .contains("\\tkzTabInit")),
            () ->
                assertTrue(
                    sheet
                        .getRow(3)
                        .getCell(COL_STATEMENT_MUTATIONS)
                        .getStringCellValue()
                        .contains("clauseTemplates")),
            // SA has answerFormula and PGFPlots graph
            () ->
                assertEquals(
                    "((a*c+b)^4 - b^4) / (4*a)",
                    sheet.getRow(4).getCell(COL_ANSWER_FORMULA).getStringCellValue()),
            () ->
                assertTrue(
                    sheet
                        .getRow(4)
                        .getCell(COL_DIAGRAM_TEMPLATE)
                        .getStringCellValue()
                        .contains("\\closedcycle")));
      }
    }

    /**
     * Round-trip: feed the freshly generated template back through preview and
     * confirm all 3 example rows validate. This is the strongest proof that the
     * bundled examples are import-ready.
     */
    @Test
    void it_should_preview_every_example_row_as_valid_when_template_is_round_tripped()
        throws Exception {
      when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of());
      byte[] templateBytes = excelImportService.generateExcelTemplate();
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "question_template_import.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              templateBytes);

      ExcelPreviewResponse response = excelImportService.previewExcelImport(file);

      assertAll(
          () -> assertEquals(3, response.getTotalRows()),
          () -> assertEquals(3, response.getValidRows(), "errors: " + response.getRows()),
          () -> assertEquals(0, response.getInvalidRows()),
          // Geometry MCQ
          () ->
              assertEquals(
                  QuestionType.MULTIPLE_CHOICE,
                  response.getRows().get(0).getData().getTemplateType()),
          () -> assertNotNull(response.getRows().get(0).getData().getOptionsGenerator()),
          () ->
              assertTrue(
                  response
                      .getRows()
                      .get(0)
                      .getData()
                      .getDiagramTemplate()
                      .contains("\\begin{tikzpicture}")),
          // TF variation table
          () ->
              assertEquals(
                  QuestionType.TRUE_FALSE, response.getRows().get(1).getData().getTemplateType()),
          () -> assertNotNull(response.getRows().get(1).getData().getStatementMutations()),
          () ->
              assertTrue(
                  ((List<?>)
                              response
                                  .getRows()
                                  .get(1)
                                  .getData()
                                  .getStatementMutations()
                                  .get("clauseTemplates"))
                          .size()
                      == 4),
          // SA function graph
          () ->
              assertEquals(
                  QuestionType.SHORT_ANSWER, response.getRows().get(2).getData().getTemplateType()),
          () ->
              assertEquals(
                  "((a*c+b)^4 - b^4) / (4*a)",
                  response.getRows().get(2).getData().getAnswerFormula()),
          () -> assertNull(response.getRows().get(2).getData().getOptionsGenerator()),
          () ->
              assertTrue(
                  response
                      .getRows()
                      .get(2)
                      .getData()
                      .getDiagramTemplate()
                      .contains("\\closedcycle")));
    }
  }
}
