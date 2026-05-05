package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.dto.request.QuestionBatchImportRequest;
import com.fptu.math_master.dto.response.QuestionBatchImportResponse;
import com.fptu.math_master.dto.response.QuestionExcelPreviewResponse;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.service.QuestionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("QuestionExcelImportServiceImpl - Tests")
class QuestionExcelImportServiceImplTest extends BaseUnitTest {

  @InjectMocks private QuestionExcelImportServiceImpl questionExcelImportService;

  @Mock private QuestionService questionService;
  @Mock private Validator validator;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  // Column layout mirrors QuestionExcelImportServiceImpl
  private static final int COL_QUESTION_TYPE = 0;
  private static final int COL_COGNITIVE_LEVEL = 1;
  private static final int COL_POINTS = 2;
  private static final int COL_TAGS = 3;
  private static final int COL_QUESTION_TEXT = 4;
  private static final int COL_DIAGRAM_DATA = 5;
  private static final int COL_OPTION_A = 6;
  private static final int COL_OPTION_B = 7;
  private static final int COL_OPTION_C = 8;
  private static final int COL_OPTION_D = 9;
  private static final int COL_TRUTH_A = 10;
  private static final int COL_TRUTH_B = 11;
  private static final int COL_TRUTH_C = 12;
  private static final int COL_TRUTH_D = 13;
  private static final int COL_CORRECT_ANSWER = 14;
  private static final int COL_VALIDATION_MODE = 15;
  private static final int COL_TOLERANCE = 16;
  private static final int COL_EXPLANATION = 17;
  private static final int COL_SOLUTION_STEPS = 18;
  private static final int COLUMN_COUNT = 19;

  private static final String[] HEADERS = {
    "questionType",
    "cognitiveLevel",
    "points",
    "tags",
    "questionText",
    "diagramData",
    "optionA",
    "optionB",
    "optionC",
    "optionD",
    "truthA",
    "truthB",
    "truthC",
    "truthD",
    "correctAnswer",
    "answerValidationMode",
    "answerTolerance",
    "explanation",
    "solutionSteps"
  };

  private CreateQuestionRequest buildValidQuestionRequest() {
    Map<String, Object> options = new LinkedHashMap<>();
    options.put("A", "2");
    options.put("B", "3");
    options.put("C", "5");
    options.put("D", "9");
    return CreateQuestionRequest.builder()
        .questionText("So nao la so nguyen to?")
        .questionType(QuestionType.MULTIPLE_CHOICE)
        .cognitiveLevel(CognitiveLevel.NHAN_BIET)
        .points(BigDecimal.ONE)
        .correctAnswer("C")
        .explanation("So nguyen to chi chia het cho 1 va chinh no.")
        .tags(new String[] {"toan", "so hoc"})
        .options(options)
        .build();
  }

  private String invokeGetCellValueAsString(Row row, Integer columnIndex) throws Exception {
    Method method =
        QuestionExcelImportServiceImpl.class.getDeclaredMethod(
            "getCellValueAsString", Row.class, Integer.class);
    method.setAccessible(true);
    return (String) method.invoke(questionExcelImportService, row, columnIndex);
  }

  /**
   * Build a workbook with header + an empty notes row + the given data rows.
   * The service skips rows below FIRST_DATA_ROW = 2, so tests must seed data
   * starting at row index 2.
   */
  private MultipartFile buildExcelFile(String filename, List<RowConsumer> rowConsumers)
      throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Questions");

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        headerRow.createCell(i).setCellValue(HEADERS[i]);
      }
      sheet.createRow(1); // notes row (left blank in tests)

      for (int i = 0; i < rowConsumers.size(); i++) {
        Row row = sheet.createRow(i + 2);
        rowConsumers.get(i).accept(row);
      }

      workbook.write(out);
      return new MockMultipartFile(
          "file",
          filename,
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          out.toByteArray());
    }
  }

  @FunctionalInterface
  private interface RowConsumer {
    void accept(Row row);
  }

  @Nested
  @DisplayName("previewExcelImport()")
  class PreviewExcelImportTests {

    @Test
    void it_should_throw_null_pointer_exception_when_file_is_null() {
      MultipartFile nullFile = null;
      assertThrows(
          NullPointerException.class,
          () -> questionExcelImportService.previewExcelImport(nullFile));
      verifyNoMoreInteractions(validator, objectMapper, questionService);
    }

    @Test
    void it_should_throw_invalid_request_when_file_is_empty() {
      MultipartFile emptyFile =
          new MockMultipartFile(
              "file",
              "question-import.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              new byte[0]);
      AppException exception =
          assertThrows(
              AppException.class, () -> questionExcelImportService.previewExcelImport(emptyFile));
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
      verifyNoMoreInteractions(validator, objectMapper, questionService);
    }

    @Test
    void it_should_throw_invalid_request_when_file_extension_is_not_xlsx() {
      MultipartFile csvFile =
          new MockMultipartFile(
              "file",
              "question-import.csv",
              "text/csv",
              "questionText,questionType".getBytes(StandardCharsets.UTF_8));
      AppException exception =
          assertThrows(
              AppException.class, () -> questionExcelImportService.previewExcelImport(csvFile));
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
      verifyNoMoreInteractions(validator, objectMapper, questionService);
    }

    @Test
    void it_should_throw_invalid_request_when_filename_is_null() {
      MultipartFile noFilenameFile =
          new MockMultipartFile(
              "file",
              null,
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              "xlsx-content".getBytes(StandardCharsets.UTF_8));
      AppException exception =
          assertThrows(
              AppException.class,
              () -> questionExcelImportService.previewExcelImport(noFilenameFile));
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
      verifyNoMoreInteractions(validator, objectMapper, questionService);
    }

    @Test
    void it_should_return_valid_preview_row_for_multiple_choice() throws IOException {
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("multiple_choice");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("nhan_biet");
                    row.createCell(COL_POINTS).setCellValue(2.0);
                    row.createCell(COL_TAGS).setCellValue("toan, so hoc");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Số nào là số nguyên tố?");
                    row.createCell(COL_OPTION_A).setCellValue("$2$");
                    row.createCell(COL_OPTION_B).setCellValue("$3$");
                    row.createCell(COL_OPTION_C).setCellValue("$5$");
                    row.createCell(COL_OPTION_D).setCellValue("$9$");
                    row.createCell(COL_CORRECT_ANSWER).setCellValue("c");
                    row.createCell(COL_EXPLANATION).setCellValue("Giải thích đáp án");
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(1, response.getTotalRows()),
          () -> assertEquals(1, response.getValidRows()),
          () -> assertEquals(0, response.getInvalidRows()),
          () -> assertTrue(response.getRows().get(0).getIsValid()),
          () -> assertEquals(2, response.getRows().get(0).getData().getPoints().intValue()),
          () -> assertEquals(2, response.getRows().get(0).getData().getTags().length),
          () -> assertEquals("toan", response.getRows().get(0).getData().getTags()[0]),
          () -> assertEquals("C", response.getRows().get(0).getData().getCorrectAnswer()),
          () -> assertEquals(4, response.getRows().get(0).getData().getOptions().size()));

      verify(validator, times(1)).validate(any(CreateQuestionRequest.class));
      verifyNoMoreInteractions(validator, questionService);
    }

    @Test
    void it_should_preserve_latex_diagram_for_geometry_mcq() throws IOException {
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      String tikz =
          "\\begin{tikzpicture}[scale=0.8]\n"
              + "\\coordinate (O) at (0,4);\n"
              + "\\coordinate (A) at (-2,0);\n"
              + "\\end{tikzpicture}";

      MultipartFile file =
          buildExcelFile(
              "geometry-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("VAN_DUNG");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Hình nón. Đoạn $OH$ bằng?");
                    row.createCell(COL_DIAGRAM_DATA).setCellValue(tikz);
                    row.createCell(COL_OPTION_A).setCellValue("a");
                    row.createCell(COL_OPTION_B).setCellValue("b");
                    row.createCell(COL_OPTION_C).setCellValue("c");
                    row.createCell(COL_OPTION_D).setCellValue("d");
                    row.createCell(COL_CORRECT_ANSWER).setCellValue("C");
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertAll(
          () -> assertEquals(1, response.getValidRows()),
          () -> assertEquals(tikz, response.getRows().get(0).getData().getDiagramData()),
          () ->
              assertTrue(
                  response
                      .getRows()
                      .get(0)
                      .getData()
                      .getDiagramData()
                      .contains("\\begin{tikzpicture}")));
    }

    @Test
    void it_should_parse_true_false_with_truth_columns() throws IOException {
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      MultipartFile file =
          buildExcelFile(
              "tf-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("TRUE_FALSE");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                    row.createCell(COL_QUESTION_TEXT)
                        .setCellValue("Cho hàm $f$. Xét các phát biểu:");
                    row.createCell(COL_OPTION_A).setCellValue("PB A");
                    row.createCell(COL_OPTION_B).setCellValue("PB B");
                    row.createCell(COL_OPTION_C).setCellValue("PB C");
                    row.createCell(COL_OPTION_D).setCellValue("PB D");
                    row.createCell(COL_TRUTH_A).setCellValue("TRUE");
                    row.createCell(COL_TRUTH_B).setCellValue("FALSE");
                    row.createCell(COL_TRUTH_C).setCellValue("Đ");
                    row.createCell(COL_TRUTH_D).setCellValue("S");
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertEquals(1, response.getValidRows());
      CreateQuestionRequest data = response.getRows().get(0).getData();
      assertAll(
          () -> assertEquals(QuestionType.TRUE_FALSE, data.getQuestionType()),
          () -> assertEquals("A,C", data.getCorrectAnswer()),
          () -> assertEquals(4, data.getOptions().size()),
          () -> assertNotNull(data.getGenerationMetadata()),
          () -> assertNotNull(data.getGenerationMetadata().get("tfClauses")));
    }

    @Test
    void it_should_mark_tf_invalid_when_truth_value_missing() throws IOException {
      MultipartFile file =
          buildExcelFile(
              "tf-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("TRUE_FALSE");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Q");
                    row.createCell(COL_OPTION_A).setCellValue("A");
                    row.createCell(COL_OPTION_B).setCellValue("B");
                    row.createCell(COL_TRUTH_A).setCellValue("TRUE");
                    // truthB intentionally missing
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertAll(
          () -> assertEquals(1, response.getInvalidRows()),
          () ->
              assertTrue(
                  response.getRows().get(0).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("Missing/invalid truth value"))));
    }

    @Test
    void it_should_parse_short_answer_with_validation_mode() throws IOException {
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      MultipartFile file =
          buildExcelFile(
              "sa-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("SHORT_ANSWER");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("VAN_DUNG");
                    row.createCell(COL_POINTS).setCellValue(0.25);
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Tính cực đại của $y=x^3-3x$");
                    row.createCell(COL_DIAGRAM_DATA)
                        .setCellValue("\\begin{tikzpicture}\\end{tikzpicture}");
                    row.createCell(COL_CORRECT_ANSWER).setCellValue("2");
                    row.createCell(COL_VALIDATION_MODE).setCellValue("NUMERIC");
                    row.createCell(COL_TOLERANCE).setCellValue(0.001);
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertEquals(1, response.getValidRows());
      CreateQuestionRequest data = response.getRows().get(0).getData();
      assertAll(
          () -> assertEquals(QuestionType.SHORT_ANSWER, data.getQuestionType()),
          () -> assertNull(data.getOptions()),
          () -> assertEquals("2", data.getCorrectAnswer()),
          () -> assertEquals(new BigDecimal("0.25"), data.getPoints()),
          () -> assertEquals("NUMERIC", data.getGenerationMetadata().get("answerValidationMode")),
          () ->
              assertEquals(
                  0.001,
                  ((Number) data.getGenerationMetadata().get("answerTolerance")).doubleValue()));
    }

    @Test
    void it_should_mark_row_invalid_when_question_type_is_invalid() throws IOException {
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("ESSAY_INVALID");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("NHAN_BIET");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Bad type");
                    row.createCell(COL_OPTION_A).setCellValue("a");
                    row.createCell(COL_OPTION_B).setCellValue("b");
                    row.createCell(COL_OPTION_C).setCellValue("c");
                    row.createCell(COL_OPTION_D).setCellValue("d");
                    row.createCell(COL_CORRECT_ANSWER).setCellValue("A");
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertAll(
          () -> assertEquals(1, response.getInvalidRows()),
          () ->
              assertTrue(
                  response
                      .getRows()
                      .get(0)
                      .getValidationErrors()
                      .get(0)
                      .contains("Row parsing error")),
          () ->
              assertTrue(
                  response
                      .getRows()
                      .get(0)
                      .getValidationErrors()
                      .get(0)
                      .contains("Invalid questionType")));
    }

    @Test
    void it_should_return_validation_errors_when_business_rules_are_violated() throws IOException {
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  // MCQ missing option D
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("VAN_DUNG");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Thieu dap an");
                    row.createCell(COL_OPTION_A).setCellValue("1");
                    row.createCell(COL_OPTION_B).setCellValue("2");
                    row.createCell(COL_OPTION_C).setCellValue("3");
                    row.createCell(COL_CORRECT_ANSWER).setCellValue("A");
                  },
                  // MCQ correctAnswer outside A-D
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("NHAN_BIET");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Sai dap an");
                    row.createCell(COL_OPTION_A).setCellValue("1");
                    row.createCell(COL_OPTION_B).setCellValue("2");
                    row.createCell(COL_OPTION_C).setCellValue("3");
                    row.createCell(COL_OPTION_D).setCellValue("4");
                    row.createCell(COL_CORRECT_ANSWER).setCellValue("Z");
                  },
                  // SA missing correctAnswer
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("SHORT_ANSWER");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("VAN_DUNG");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("SA missing answer");
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertAll(
          () -> assertEquals(3, response.getTotalRows()),
          () -> assertEquals(0, response.getValidRows()),
          () -> assertEquals(3, response.getInvalidRows()),
          () ->
              assertTrue(
                  response.getRows().get(0).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("phải có đủ 4 đáp án"))),
          () ->
              assertTrue(
                  response.getRows().get(1).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("correctAnswer phải là A, B, C hoặc D"))),
          () ->
              assertTrue(
                  response.getRows().get(2).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("cần điền correctAnswer"))));

      verify(validator, times(3)).validate(any(CreateQuestionRequest.class));
      verifyNoMoreInteractions(validator, questionService);
    }

    @Test
    void it_should_append_constraint_violation_messages_when_validator_returns_violations()
        throws IOException {
      @SuppressWarnings("unchecked")
      ConstraintViolation<CreateQuestionRequest> violation =
          (ConstraintViolation<CreateQuestionRequest>)
              org.mockito.Mockito.mock(ConstraintViolation.class);
      Path propertyPath = org.mockito.Mockito.mock(Path.class);
      when(propertyPath.toString()).thenReturn("questionText");
      when(violation.getPropertyPath()).thenReturn(propertyPath);
      when(violation.getMessage()).thenReturn("Question text is required");
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of(violation));

      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("NHAN_BIET");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Noi dung cau hoi");
                    row.createCell(COL_OPTION_A).setCellValue("1");
                    row.createCell(COL_OPTION_B).setCellValue("2");
                    row.createCell(COL_OPTION_C).setCellValue("3");
                    row.createCell(COL_OPTION_D).setCellValue("4");
                    row.createCell(COL_CORRECT_ANSWER).setCellValue("A");
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertAll(
          () -> assertEquals(1, response.getInvalidRows()),
          () ->
              assertTrue(
                  response.getRows().get(0).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("questionText: Question text is required"))));
    }

    @Test
    void it_should_skip_empty_rows_when_previewing_excel() throws IOException {
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> row.createCell(0).setBlank(),
                  row -> {
                    row.createCell(COL_QUESTION_TYPE).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(COL_COGNITIVE_LEVEL).setCellValue("THONG_HIEU");
                    row.createCell(COL_QUESTION_TEXT).setCellValue("Cau hoi hop le");
                    row.createCell(COL_OPTION_A).setCellValue("1");
                    row.createCell(COL_OPTION_B).setCellValue("2");
                    row.createCell(COL_OPTION_C).setCellValue("3");
                    row.createCell(COL_OPTION_D).setCellValue("4");
                    row.createCell(COL_CORRECT_ANSWER).setCellValue("A");
                  }));

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertAll(
          () -> assertEquals(1, response.getTotalRows()),
          () -> assertEquals(1, response.getValidRows()),
          () -> assertEquals(0, response.getInvalidRows()));
    }

    @Test
    void it_should_throw_uncategorized_exception_when_workbook_content_is_corrupted() {
      MultipartFile corruptedFile =
          new MockMultipartFile(
              "file",
              "question-import.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              "not-a-real-xlsx".getBytes(StandardCharsets.UTF_8));

      AppException exception =
          assertThrows(
              AppException.class,
              () -> questionExcelImportService.previewExcelImport(corruptedFile));
      assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION, exception.getErrorCode());
    }

    @Test
    void it_should_cover_all_switch_cases_in_get_cell_value_as_string() throws Exception {
      try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("SwitchCoverage");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("question text");
        row.createCell(1).setCellValue(7.0);
        row.createCell(2).setCellValue(0.25);
        row.createCell(3).setCellValue(true);
        row.createCell(4).setCellFormula("1+2");
        row.createCell(5).setBlank();

        String stringValue = invokeGetCellValueAsString(row, 0);
        String integerValue = invokeGetCellValueAsString(row, 1);
        String decimalValue = invokeGetCellValueAsString(row, 2);
        String booleanValue = invokeGetCellValueAsString(row, 3);
        String formulaValue = invokeGetCellValueAsString(row, 4);
        String defaultValue = invokeGetCellValueAsString(row, 5);
        String nullCellValue = invokeGetCellValueAsString(row, 6);
        String nullColumnValue = invokeGetCellValueAsString(row, null);

        assertAll(
            () -> assertEquals("question text", stringValue),
            () -> assertEquals("7", integerValue),
            () -> assertEquals("0.25", decimalValue),
            () -> assertEquals("true", booleanValue),
            // formula cell evaluates lazily; either the formula text or its cached value is fine
            () -> assertNotNull(formulaValue),
            () -> assertNull(defaultValue),
            () -> assertNull(nullCellValue),
            () -> assertNull(nullColumnValue));
      }
    }
  }

  @Nested
  @DisplayName("importQuestionsBatch()")
  class ImportQuestionsBatchTests {

    @Test
    void it_should_return_success_summary_when_all_questions_are_imported_successfully() {
      CreateQuestionRequest firstQuestion = buildValidQuestionRequest();
      CreateQuestionRequest secondQuestion = buildValidQuestionRequest();
      secondQuestion.setQuestionText("Tinh dien tich hinh tron co ban kinh r");
      QuestionBatchImportRequest request =
          QuestionBatchImportRequest.builder()
              .questions(List.of(firstQuestion, secondQuestion))
              .build();

      QuestionBatchImportResponse response =
          questionExcelImportService.importQuestionsBatch(request);

      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(2, response.getTotalRows()),
          () -> assertEquals(2, response.getSuccessCount()),
          () -> assertEquals(0, response.getFailedCount()),
          () -> assertNull(response.getErrors()));

      verify(questionService, times(1)).createQuestion(firstQuestion);
      verify(questionService, times(1)).createQuestion(secondQuestion);
      verifyNoMoreInteractions(questionService, validator);
    }

    @Test
    void it_should_collect_error_messages_when_some_rows_fail_during_batch_import() {
      CreateQuestionRequest firstQuestion = buildValidQuestionRequest();
      CreateQuestionRequest secondQuestion = buildValidQuestionRequest();
      secondQuestion.setQuestionText("Cau hoi gay loi khi import");
      QuestionBatchImportRequest request =
          QuestionBatchImportRequest.builder()
              .questions(List.of(firstQuestion, secondQuestion))
              .build();
      org.mockito.Mockito.doThrow(new RuntimeException("Database timeout"))
          .when(questionService)
          .createQuestion(secondQuestion);

      QuestionBatchImportResponse response =
          questionExcelImportService.importQuestionsBatch(request);

      assertAll(
          () -> assertEquals(2, response.getTotalRows()),
          () -> assertEquals(1, response.getSuccessCount()),
          () -> assertEquals(1, response.getFailedCount()),
          () -> assertNotNull(response.getErrors()),
          () -> assertEquals(1, response.getErrors().size()),
          () -> assertTrue(response.getErrors().get(0).contains("Row 2: Database timeout")));

      verify(questionService, times(1)).createQuestion(firstQuestion);
      verify(questionService, times(1)).createQuestion(secondQuestion);
      verifyNoMoreInteractions(questionService, validator);
    }

    @Test
    void it_should_return_zero_counts_when_batch_request_has_no_questions() {
      QuestionBatchImportRequest request =
          QuestionBatchImportRequest.builder().questions(List.of()).build();

      QuestionBatchImportResponse response =
          questionExcelImportService.importQuestionsBatch(request);

      assertAll(
          () -> assertEquals(0, response.getTotalRows()),
          () -> assertEquals(0, response.getSuccessCount()),
          () -> assertEquals(0, response.getFailedCount()),
          () -> assertNull(response.getErrors()));

      verify(questionService, never()).createQuestion(any(CreateQuestionRequest.class));
      verifyNoMoreInteractions(questionService, validator);
    }
  }

  @Nested
  @DisplayName("generateExcelTemplate()")
  class GenerateExcelTemplateTests {

    @Test
    void it_should_generate_excel_template_with_expected_headers_and_instruction_sheet()
        throws IOException {
      byte[] templateBytes = questionExcelImportService.generateExcelTemplate();

      assertNotNull(templateBytes);
      assertTrue(templateBytes.length > 0);

      try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(templateBytes))) {
        Sheet questionsSheet = workbook.getSheet("Questions");
        Sheet instructionSheet = workbook.getSheet("Hướng dẫn");
        assertAll(
            () -> assertNotNull(questionsSheet),
            () -> assertNotNull(instructionSheet),
            () ->
                assertEquals(
                    "questionType",
                    questionsSheet.getRow(0).getCell(COL_QUESTION_TYPE).getStringCellValue()),
            () ->
                assertEquals(
                    "questionText",
                    questionsSheet.getRow(0).getCell(COL_QUESTION_TEXT).getStringCellValue()),
            () ->
                assertEquals(
                    "diagramData",
                    questionsSheet.getRow(0).getCell(COL_DIAGRAM_DATA).getStringCellValue()),
            () ->
                assertEquals(
                    "truthA", questionsSheet.getRow(0).getCell(COL_TRUTH_A).getStringCellValue()),
            () ->
                assertEquals(
                    "answerValidationMode",
                    questionsSheet.getRow(0).getCell(COL_VALIDATION_MODE).getStringCellValue()),
            () ->
                assertEquals(
                    "solutionSteps",
                    questionsSheet.getRow(0).getCell(COL_SOLUTION_STEPS).getStringCellValue()),
            () -> assertEquals(COLUMN_COUNT, questionsSheet.getRow(0).getLastCellNum()),
            () ->
                assertTrue(
                    questionsSheet
                        .getRow(1)
                        .getCell(COL_QUESTION_TYPE)
                        .getStringCellValue()
                        .contains("MULTIPLE_CHOICE | TRUE_FALSE | SHORT_ANSWER")),
            () ->
                assertEquals(
                    "HƯỚNG DẪN IMPORT CÂU HỎI",
                    instructionSheet.getRow(0).getCell(0).getStringCellValue()));
      }
    }

    @Test
    void it_should_include_three_question_type_examples_in_template() throws IOException {
      byte[] templateBytes = questionExcelImportService.generateExcelTemplate();
      try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(templateBytes))) {
        Sheet sheet = workbook.getSheet("Questions");
        // Examples start at row 2
        assertAll(
            () ->
                assertEquals(
                    "MULTIPLE_CHOICE",
                    sheet.getRow(2).getCell(COL_QUESTION_TYPE).getStringCellValue()),
            () ->
                assertEquals(
                    "MULTIPLE_CHOICE",
                    sheet.getRow(3).getCell(COL_QUESTION_TYPE).getStringCellValue()),
            () ->
                assertEquals(
                    "TRUE_FALSE", sheet.getRow(4).getCell(COL_QUESTION_TYPE).getStringCellValue()),
            () ->
                assertEquals(
                    "TRUE_FALSE", sheet.getRow(5).getCell(COL_QUESTION_TYPE).getStringCellValue()),
            () ->
                assertEquals(
                    "SHORT_ANSWER",
                    sheet.getRow(6).getCell(COL_QUESTION_TYPE).getStringCellValue()),
            // Geometry MCQ has TikZ in diagramData
            () ->
                assertTrue(
                    sheet
                        .getRow(3)
                        .getCell(COL_DIAGRAM_DATA)
                        .getStringCellValue()
                        .contains("\\begin{tikzpicture}")),
            // SA has NUMERIC validation mode
            () ->
                assertEquals(
                    "NUMERIC", sheet.getRow(6).getCell(COL_VALIDATION_MODE).getStringCellValue()));
      }
    }

    /**
     * Round-trip guarantee: feeding the generated template back through the
     * preview pipeline must validate every example row. This is the strongest
     * proof that the bundled examples are complete and import-ready.
     */
    @Test
    void it_should_preview_every_example_row_as_valid_when_template_is_round_tripped()
        throws IOException {
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());

      byte[] templateBytes = questionExcelImportService.generateExcelTemplate();
      MultipartFile file =
          new MockMultipartFile(
              "file",
              "question_import.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              templateBytes);

      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      assertAll(
          () -> assertEquals(5, response.getTotalRows()),
          () -> assertEquals(5, response.getValidRows(), "errors: " + response.getRows()),
          () -> assertEquals(0, response.getInvalidRows()),
          // standard MCQ
          () ->
              assertEquals(
                  QuestionType.MULTIPLE_CHOICE,
                  response.getRows().get(0).getData().getQuestionType()),
          () -> assertEquals("A", response.getRows().get(0).getData().getCorrectAnswer()),
          // geometry MCQ — TikZ preserved verbatim
          () ->
              assertTrue(
                  response
                      .getRows()
                      .get(1)
                      .getData()
                      .getDiagramData()
                      .contains("\\begin{tikzpicture}")),
          () -> assertEquals("B", response.getRows().get(1).getData().getCorrectAnswer()),
          // statement TF — correctAnswer derived from truth columns
          () ->
              assertEquals(
                  QuestionType.TRUE_FALSE, response.getRows().get(2).getData().getQuestionType()),
          () -> assertEquals("A,B", response.getRows().get(2).getData().getCorrectAnswer()),
          () -> assertNotNull(response.getRows().get(2).getData().getGenerationMetadata()),
          // variation-table TF — correctAnswer auto-derived (left blank in template)
          () -> assertEquals("A,B,D", response.getRows().get(3).getData().getCorrectAnswer()),
          () ->
              assertTrue(
                  response.getRows().get(3).getData().getDiagramData().contains("\\tkzTabInit")),
          // SA — answerValidationMode + tolerance plumbed through
          () ->
              assertEquals(
                  QuestionType.SHORT_ANSWER, response.getRows().get(4).getData().getQuestionType()),
          () -> assertNull(response.getRows().get(4).getData().getOptions()),
          () -> assertEquals("2", response.getRows().get(4).getData().getCorrectAnswer()),
          () ->
              assertEquals(
                  "NUMERIC",
                  response
                      .getRows()
                      .get(4)
                      .getData()
                      .getGenerationMetadata()
                      .get("answerValidationMode")),
          () ->
              assertTrue(
                  response
                      .getRows()
                      .get(4)
                      .getData()
                      .getDiagramData()
                      .contains("\\addplot[blue, thick]")));
    }
  }
}
