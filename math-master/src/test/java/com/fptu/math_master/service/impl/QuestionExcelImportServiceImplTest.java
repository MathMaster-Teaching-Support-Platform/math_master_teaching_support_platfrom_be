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

  private MultipartFile buildExcelFile(String filename, List<RowConsumer> rowConsumers) throws IOException {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Questions");

      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("questionText");
      headerRow.createCell(1).setCellValue("questionType");
      headerRow.createCell(2).setCellValue("cognitiveLevel");
      headerRow.createCell(3).setCellValue("points");
      headerRow.createCell(4).setCellValue("correctAnswer");
      headerRow.createCell(5).setCellValue("explanation");
      headerRow.createCell(6).setCellValue("tags");
      headerRow.createCell(7).setCellValue("options");

      for (int i = 0; i < rowConsumers.size(); i++) {
        Row row = sheet.createRow(i + 1);
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

    /**
     * Abnormal case: File import null.
     *
     * <p>Input:
     * <ul>
     *   <li>file: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>previewExcelImport() log file name voi file null -> NPE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link NullPointerException} do truy cap file.getOriginalFilename() voi file null</li>
     * </ul>
     */
    @Test
    void it_should_throw_null_pointer_exception_when_file_is_null() {
      // ===== ARRANGE =====
      MultipartFile nullFile = null;

      // ===== ACT & ASSERT =====
      assertThrows(
          NullPointerException.class, () -> questionExcelImportService.previewExcelImport(nullFile));

      // ===== VERIFY =====
      verifyNoMoreInteractions(validator, objectMapper, questionService);
    }

    /**
     * Abnormal case: File rong.
     *
     * <p>Input:
     * <ul>
     *   <li>file: empty xlsx</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateExcelFile() -> file.isEmpty() (TRUE branch, throw INVALID_REQUEST)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} voi error code {@code INVALID_REQUEST}</li>
     * </ul>
     */
    @Test
    void it_should_throw_invalid_request_when_file_is_empty() {
      // ===== ARRANGE =====
      MultipartFile emptyFile =
          new MockMultipartFile(
              "file",
              "question-import.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              new byte[0]);

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> questionExcelImportService.previewExcelImport(emptyFile));
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(validator, objectMapper, questionService);
    }

    /**
     * Abnormal case: Dinh dang file khong phai xlsx.
     *
     * <p>Input:
     * <ul>
     *   <li>file: question-import.csv</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateExcelFile() -> !filename.endsWith(".xlsx") (TRUE branch)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} voi error code {@code INVALID_REQUEST}</li>
     * </ul>
     */
    @Test
    void it_should_throw_invalid_request_when_file_extension_is_not_xlsx() {
      // ===== ARRANGE =====
      MultipartFile csvFile =
          new MockMultipartFile(
              "file",
              "question-import.csv",
              "text/csv",
              "questionText,questionType".getBytes(StandardCharsets.UTF_8));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> questionExcelImportService.previewExcelImport(csvFile));
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(validator, objectMapper, questionService);
    }

    /**
     * Abnormal case: Ten file null.
     *
     * <p>Input:
     * <ul>
     *   <li>file: originalFilename = null, content khong rong</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateExcelFile() -> filename == null (TRUE branch)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} voi error code {@code INVALID_REQUEST}</li>
     * </ul>
     */
    @Test
    void it_should_throw_invalid_request_when_filename_is_null() {
      // ===== ARRANGE =====
      MultipartFile noFilenameFile =
          new MockMultipartFile(
              "file",
              null,
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              "xlsx-content".getBytes(StandardCharsets.UTF_8));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> questionExcelImportService.previewExcelImport(noFilenameFile));
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(validator, objectMapper, questionService);
    }

    /**
     * Normal case: Preview thanh cong voi mot dong hop le.
     *
     * <p>Input:
     * <ul>
     *   <li>row1: MULTIPLE_CHOICE, options A/B/C/D day du, correctAnswer = C</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>row != null && !isRowEmpty(row) (TRUE branch)</li>
     *   <li>parseRowToRequest() parse enum/points/tags/options thanh cong</li>
     *   <li>validateRequest(): errors.isEmpty() (TRUE branch)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Tra ve 1 dong valid, invalidRows = 0</li>
     * </ul>
     */
    @Test
    void it_should_return_valid_preview_row_when_excel_row_is_valid() throws IOException {
      // ===== ARRANGE =====
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(0).setCellValue("So nao la so nguyen to?");
                    row.createCell(1).setCellValue("multiple_choice");
                    row.createCell(2).setCellValue("nhan_biet");
                    row.createCell(3).setCellValue(2.0);
                    row.createCell(4).setCellValue("c");
                    row.createCell(5).setCellValue("Giai thich dap an");
                    row.createCell(6).setCellValue("toan, so hoc");
                    row.createCell(7).setCellValue("{\"A\":\"2\",\"B\":\"3\",\"C\":\"5\",\"D\":\"9\"}");
                  }));

      // ===== ACT =====
      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(1, response.getTotalRows()),
          () -> assertEquals(1, response.getValidRows()),
          () -> assertEquals(0, response.getInvalidRows()),
          () -> assertEquals(1, response.getRows().size()),
          () -> assertTrue(response.getRows().get(0).getIsValid()),
          () -> assertEquals(2, response.getRows().get(0).getData().getPoints().intValue()),
          () -> assertEquals(2, response.getRows().get(0).getData().getTags().length),
          () -> assertEquals("toan", response.getRows().get(0).getData().getTags()[0]),
          () -> assertEquals("c", response.getRows().get(0).getData().getCorrectAnswer()));

      // ===== VERIFY =====
      verify(validator, times(1)).validate(any(CreateQuestionRequest.class));
      verifyNoMoreInteractions(validator, questionService);
    }

    /**
     * Abnormal case: parse row that bai do questionType khong hop le.
     *
     * <p>Input:
     * <ul>
     *   <li>row1: questionType = ESSAY_INVALID</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>parseRowToRequest() -> IllegalArgumentException khi parse questionType (TRUE branch)</li>
     *   <li>preview loop catch(Exception e) cua tung row</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Row duoc danh dau invalid voi thong bao Row parsing error</li>
     * </ul>
     */
    @Test
    void it_should_mark_row_invalid_when_question_type_is_invalid() throws IOException {
      // ===== ARRANGE =====
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(0).setCellValue("Tinh tong 2 so");
                    row.createCell(1).setCellValue("ESSAY_INVALID");
                    row.createCell(2).setCellValue("NHAN_BIET");
                    row.createCell(3).setCellValue(1.0);
                    row.createCell(4).setCellValue("A");
                    row.createCell(7).setCellValue("{\"A\":\"2\",\"B\":\"3\",\"C\":\"4\",\"D\":\"5\"}");
                  }));

      // ===== ACT =====
      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, response.getTotalRows()),
          () -> assertEquals(0, response.getValidRows()),
          () -> assertEquals(1, response.getInvalidRows()),
          () -> assertTrue(response.getRows().get(0).getValidationErrors().get(0).contains("Row parsing error")),
          () -> assertTrue(response.getRows().get(0).getValidationErrors().get(0).contains("Invalid questionType")));

      // ===== VERIFY =====
      verifyNoMoreInteractions(validator, questionService);
    }

    /**
     * Abnormal case: parse row that bai do options JSON khong hop le.
     *
     * <p>Input:
     * <ul>
     *   <li>row1: options json bi loi cu phap</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>parse options -> objectMapper.readValue() throw exception</li>
     *   <li>preview loop catch(Exception e) cua tung row</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Row invalid voi thong bao Invalid options JSON</li>
     * </ul>
     */
    @Test
    void it_should_mark_row_invalid_when_options_json_is_invalid() throws IOException {
      // ===== ARRANGE =====
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(0).setCellValue("Tinh gia tri bieu thuc");
                    row.createCell(1).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(2).setCellValue("THONG_HIEU");
                    row.createCell(3).setCellValue(1.0);
                    row.createCell(4).setCellValue("A");
                    row.createCell(7).setCellValue("{\"A\":\"1\",\"B\":\"2\"");
                  }));

      // ===== ACT =====
      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, response.getInvalidRows()),
          () -> assertTrue(response.getRows().get(0).getValidationErrors().get(0).contains("Invalid options JSON")));

      // ===== VERIFY =====
      verifyNoMoreInteractions(validator, questionService);
    }

    /**
     * Abnormal case: request vi pham business rules cua cau hoi trac nghiem.
     *
     * <p>Input:
     * <ul>
     *   <li>row1: questionType = TRUE_FALSE</li>
     *   <li>row2: MULTIPLE_CHOICE nhung options khong dung 4 dap an</li>
     *   <li>row3: MULTIPLE_CHOICE dung 4 options nhung sai key</li>
     *   <li>row4: MULTIPLE_CHOICE, correctAnswer nam ngoai A-D</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateRequest() -> RULE 1 questionType != MULTIPLE_CHOICE (TRUE branch)</li>
     *   <li>RULE 2 options size != 4 (TRUE branch)</li>
     *   <li>RULE 2 keys != A/B/C/D (TRUE branch)</li>
     *   <li>correctAnswer !matches A-D (TRUE branch)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Tat ca 4 dong deu invalid va co validationErrors phu hop</li>
     * </ul>
     */
    @Test
    void it_should_return_validation_errors_when_business_rules_are_violated() throws IOException {
      // ===== ARRANGE =====
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> {
                    row.createCell(0).setCellValue("Cau hoi dang true false");
                    row.createCell(1).setCellValue("TRUE_FALSE");
                    row.createCell(2).setCellValue("THONG_HIEU");
                    row.createCell(4).setCellValue("A");
                  },
                  row -> {
                    row.createCell(0).setCellValue("Thieu dap an");
                    row.createCell(1).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(2).setCellValue("VAN_DUNG");
                    row.createCell(4).setCellValue("A");
                    row.createCell(7).setCellValue("{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\"}");
                  },
                  row -> {
                    row.createCell(0).setCellValue("Sai nhan dap an");
                    row.createCell(1).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(2).setCellValue("VAN_DUNG_CAO");
                    row.createCell(4).setCellValue("B");
                    row.createCell(7).setCellValue("{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\",\"E\":\"4\"}");
                  },
                  row -> {
                    row.createCell(0).setCellValue("Dap an dung sai format");
                    row.createCell(1).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(2).setCellValue("NHAN_BIET");
                    row.createCell(4).setCellValue("Z");
                    row.createCell(7).setCellValue("{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\",\"D\":\"4\"}");
                  }));

      // ===== ACT =====
      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(4, response.getTotalRows()),
          () -> assertEquals(0, response.getValidRows()),
          () -> assertEquals(4, response.getInvalidRows()),
          () ->
              assertTrue(
                  response.getRows().get(0).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("Chỉ cho phép loại câu hỏi MULTIPLE_CHOICE"))),
          () ->
              assertTrue(
                  response.getRows().get(1).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("phải có đúng 4 đáp án"))),
          () ->
              assertTrue(
                  response.getRows().get(2).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("đánh nhãn A, B, C, D"))),
          () ->
              assertTrue(
                  response.getRows().get(3).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("Đáp án đúng phải là A, B, C hoặc D"))));

      // ===== VERIFY =====
      verify(validator, times(4)).validate(any(CreateQuestionRequest.class));
      verifyNoMoreInteractions(validator, questionService);
    }

    /**
     * Abnormal case: Co violation tu bean validator.
     *
     * <p>Input:
     * <ul>
     *   <li>row1: du lieu co ve hop le, nhung validator tra ve 1 violation</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateRequest() loop qua violations (non-empty branch)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Validation error chua property path va message tu validator</li>
     * </ul>
     */
    @Test
    void it_should_append_constraint_violation_messages_when_validator_returns_violations()
        throws IOException {
      // ===== ARRANGE =====
      @SuppressWarnings("unchecked")
      ConstraintViolation<CreateQuestionRequest> violation = (ConstraintViolation<CreateQuestionRequest>) org.mockito.Mockito.mock(ConstraintViolation.class);
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
                    row.createCell(0).setCellValue("Noi dung cau hoi");
                    row.createCell(1).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(2).setCellValue("NHAN_BIET");
                    row.createCell(4).setCellValue("A");
                    row.createCell(7).setCellValue("{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\",\"D\":\"4\"}");
                  }));

      // ===== ACT =====
      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, response.getInvalidRows()),
          () ->
              assertTrue(
                  response.getRows().get(0).getValidationErrors().stream()
                      .anyMatch(msg -> msg.contains("questionText: Question text is required"))));

      // ===== VERIFY =====
      verify(validator, times(1)).validate(any(CreateQuestionRequest.class));
      verifyNoMoreInteractions(validator, questionService);
    }

    /**
     * Normal case: Bo qua row null/row rong.
     *
     * <p>Input:
     * <ul>
     *   <li>row1: de trong toan bo</li>
     *   <li>row2: co 1 o du lieu hop le</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>if (row == null || isRowEmpty(row)) -> TRUE branch skip row1</li>
     *   <li>isRowEmpty(row) -> FALSE branch voi row2</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Chi tinh row2 la du lieu can validate</li>
     * </ul>
     */
    @Test
    void it_should_skip_empty_rows_when_previewing_excel() throws IOException {
      // ===== ARRANGE =====
      when(validator.validate(any(CreateQuestionRequest.class))).thenReturn(Set.of());
      MultipartFile file =
          buildExcelFile(
              "question-import.xlsx",
              List.of(
                  row -> row.createCell(0).setBlank(),
                  row -> {
                    row.createCell(0).setCellValue("Cau hoi hop le");
                    row.createCell(1).setCellValue("MULTIPLE_CHOICE");
                    row.createCell(2).setCellValue("THONG_HIEU");
                    row.createCell(4).setCellValue("A");
                    row.createCell(7).setCellValue("{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\",\"D\":\"4\"}");
                  }));

      // ===== ACT =====
      QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, response.getTotalRows()),
          () -> assertEquals(1, response.getValidRows()),
          () -> assertEquals(0, response.getInvalidRows()));

      // ===== VERIFY =====
      verify(validator, times(1)).validate(any(CreateQuestionRequest.class));
      verifyNoMoreInteractions(validator, questionService);
    }

    /**
     * Abnormal case: Workbook khong hop le gay exception khi parse.
     *
     * <p>Input:
     * <ul>
     *   <li>file: .xlsx nhung noi dung text khong phai excel binary</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>previewExcelImport() catch(Exception e) ben ngoai vong lap (UNCATEGORIZED_EXCEPTION)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} voi error code {@code UNCATEGORIZED_EXCEPTION}</li>
     * </ul>
     */
    @Test
    void it_should_throw_uncategorized_exception_when_workbook_content_is_corrupted() {
      // ===== ARRANGE =====
      MultipartFile corruptedFile =
          new MockMultipartFile(
              "file",
              "question-import.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              "not-a-real-xlsx".getBytes(StandardCharsets.UTF_8));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> questionExcelImportService.previewExcelImport(corruptedFile));
      assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(validator, questionService);
    }

    /**
     * Normal case: Cover full switch cua getCellValueAsString.
     *
     * <p>Input:
     * <ul>
     *   <li>row gom cell STRING, NUMERIC, BOOLEAN, FORMULA, BLANK va null cell</li>
     *   <li>columnIndex null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>columnIndex == null -> return null</li>
     *   <li>cell == null -> return null</li>
     *   <li>switch case STRING, NUMERIC, BOOLEAN, FORMULA, default(BLANK)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Moi case tra ve dung gia tri theo implementation</li>
     * </ul>
     */
    @Test
    void it_should_cover_all_switch_cases_in_get_cell_value_as_string() throws Exception {
      // ===== ARRANGE =====
      try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("SwitchCoverage");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("question text");
        row.createCell(1).setCellValue(7.0);
        row.createCell(2).setCellValue(true);
        row.createCell(3).setCellFormula("1+2");
        row.createCell(4).setBlank();

        // ===== ACT =====
        String stringValue = invokeGetCellValueAsString(row, 0);
        String numericValue = invokeGetCellValueAsString(row, 1);
        String booleanValue = invokeGetCellValueAsString(row, 2);
        String formulaValue = invokeGetCellValueAsString(row, 3);
        String defaultValue = invokeGetCellValueAsString(row, 4);
        String nullCellValue = invokeGetCellValueAsString(row, 5);
        String nullColumnValue = invokeGetCellValueAsString(row, null);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals("question text", stringValue),
            () -> assertEquals("7", numericValue),
            () -> assertEquals("true", booleanValue),
            () -> assertEquals("1+2", formulaValue),
            () -> assertNull(defaultValue),
            () -> assertNull(nullCellValue),
            () -> assertNull(nullColumnValue));
      }

      // ===== VERIFY =====
      verifyNoMoreInteractions(validator, questionService, objectMapper);
    }
  }

  @Nested
  @DisplayName("importQuestionsBatch()")
  class ImportQuestionsBatchTests {

    /**
     * Normal case: Tat ca cau hoi import thanh cong.
     *
     * <p>Input:
     * <ul>
     *   <li>request.questions: 2 cau hoi hop le</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>for-loop qua tat ca rows</li>
     *   <li>try block success branch cho moi row</li>
     *   <li>errors.isEmpty() == TRUE -> response.errors = null</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>successCount = 2, failedCount = 0, errors = null</li>
     * </ul>
     */
    @Test
    void it_should_return_success_summary_when_all_questions_are_imported_successfully() {
      // ===== ARRANGE =====
      CreateQuestionRequest firstQuestion = buildValidQuestionRequest();
      CreateQuestionRequest secondQuestion = buildValidQuestionRequest();
      secondQuestion.setQuestionText("Tinh dien tich hinh tron co ban kinh {{r}}");
      QuestionBatchImportRequest request =
          QuestionBatchImportRequest.builder().questions(List.of(firstQuestion, secondQuestion)).build();

      // ===== ACT =====
      QuestionBatchImportResponse response = questionExcelImportService.importQuestionsBatch(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(2, response.getTotalRows()),
          () -> assertEquals(2, response.getSuccessCount()),
          () -> assertEquals(0, response.getFailedCount()),
          () -> assertNull(response.getErrors()));

      // ===== VERIFY =====
      verify(questionService, times(1)).createQuestion(firstQuestion);
      verify(questionService, times(1)).createQuestion(secondQuestion);
      verifyNoMoreInteractions(questionService, validator);
    }

    /**
     * Abnormal case: Co row import that bai.
     *
     * <p>Input:
     * <ul>
     *   <li>row1: import thanh cong</li>
     *   <li>row2: createQuestion throw RuntimeException</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>try branch cho row1</li>
     *   <li>catch(Exception e) branch cho row2</li>
     *   <li>errors.isEmpty() == FALSE -> response.errors co gia tri</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>successCount = 1, failedCount = 1, errors chua thong diep row 2</li>
     * </ul>
     */
    @Test
    void it_should_collect_error_messages_when_some_rows_fail_during_batch_import() {
      // ===== ARRANGE =====
      CreateQuestionRequest firstQuestion = buildValidQuestionRequest();
      CreateQuestionRequest secondQuestion = buildValidQuestionRequest();
      secondQuestion.setQuestionText("Cau hoi gay loi khi import");
      QuestionBatchImportRequest request =
          QuestionBatchImportRequest.builder().questions(List.of(firstQuestion, secondQuestion)).build();
      org.mockito.Mockito.doThrow(new RuntimeException("Database timeout"))
          .when(questionService)
          .createQuestion(secondQuestion);

      // ===== ACT =====
      QuestionBatchImportResponse response = questionExcelImportService.importQuestionsBatch(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(2, response.getTotalRows()),
          () -> assertEquals(1, response.getSuccessCount()),
          () -> assertEquals(1, response.getFailedCount()),
          () -> assertNotNull(response.getErrors()),
          () -> assertEquals(1, response.getErrors().size()),
          () -> assertTrue(response.getErrors().get(0).contains("Row 2: Database timeout")));

      // ===== VERIFY =====
      verify(questionService, times(1)).createQuestion(firstQuestion);
      verify(questionService, times(1)).createQuestion(secondQuestion);
      verifyNoMoreInteractions(questionService, validator);
    }

    /**
     * Abnormal case: Danh sach cau hoi rong.
     *
     * <p>Input:
     * <ul>
     *   <li>request.questions: empty list</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>for-loop khong chay lan nao</li>
     *   <li>errors.isEmpty() == TRUE</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>response tong so = 0, success = 0, failed = 0</li>
     * </ul>
     */
    @Test
    void it_should_return_zero_counts_when_batch_request_has_no_questions() {
      // ===== ARRANGE =====
      QuestionBatchImportRequest request =
          QuestionBatchImportRequest.builder().questions(List.of()).build();

      // ===== ACT =====
      QuestionBatchImportResponse response = questionExcelImportService.importQuestionsBatch(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(0, response.getTotalRows()),
          () -> assertEquals(0, response.getSuccessCount()),
          () -> assertEquals(0, response.getFailedCount()),
          () -> assertNull(response.getErrors()));

      // ===== VERIFY =====
      verify(questionService, never()).createQuestion(any(CreateQuestionRequest.class));
      verifyNoMoreInteractions(questionService, validator);
    }
  }

  @Nested
  @DisplayName("generateExcelTemplate()")
  class GenerateExcelTemplateTests {

    /**
     * Normal case: Tao file template thanh cong.
     *
     * <p>Input:
     * <ul>
     *   <li>khong co input parameter</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>generateExcelTemplate() try block thanh cong</li>
     *   <li>tao sheet Questions + Huong dan + du lieu mau</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Tra ve byte[] hop le, doc duoc workbook va dung cau truc mau</li>
     * </ul>
     */
    @Test
    void it_should_generate_excel_template_with_expected_headers_and_instruction_sheet()
        throws IOException {
      // ===== ARRANGE =====
      // No additional setup required.

      // ===== ACT =====
      byte[] templateBytes = questionExcelImportService.generateExcelTemplate();

      // ===== ASSERT =====
      assertNotNull(templateBytes);
      assertTrue(templateBytes.length > 0);

      try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(templateBytes))) {
        Sheet questionsSheet = workbook.getSheet("Questions");
        Sheet instructionSheet = workbook.getSheet("Hướng dẫn");
        assertAll(
            () -> assertNotNull(questionsSheet),
            () -> assertNotNull(instructionSheet),
            () -> assertEquals("questionText", questionsSheet.getRow(0).getCell(0).getStringCellValue()),
            () -> assertEquals("questionType", questionsSheet.getRow(0).getCell(1).getStringCellValue()),
            () -> assertEquals("cognitiveLevel", questionsSheet.getRow(0).getCell(2).getStringCellValue()),
            () -> assertEquals("points", questionsSheet.getRow(0).getCell(3).getStringCellValue()),
            () -> assertEquals("correctAnswer", questionsSheet.getRow(0).getCell(4).getStringCellValue()),
            () -> assertEquals("options", questionsSheet.getRow(0).getCell(7).getStringCellValue()),
            () ->
                assertTrue(
                    questionsSheet
                        .getRow(1)
                        .getCell(1)
                        .getStringCellValue()
                        .contains("CHỈ MULTIPLE_CHOICE")),
            () -> assertEquals("HƯỚNG DẪN IMPORT CÂU HỎI", instructionSheet.getRow(0).getCell(0).getStringCellValue()),
            () ->
                assertTrue(
                    instructionSheet.getRow(3).getCell(0).getStringCellValue().contains("CHỈ được là MULTIPLE_CHOICE")));
      }

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionService, validator);
    }
  }
}
