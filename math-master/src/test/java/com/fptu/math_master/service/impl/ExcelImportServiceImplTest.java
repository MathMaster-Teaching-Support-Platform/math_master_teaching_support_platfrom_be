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
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("ExcelImportServiceImpl - Tests")
@SuppressWarnings("unchecked")
class ExcelImportServiceImplTest extends BaseUnitTest {

  @InjectMocks private ExcelImportServiceImpl excelImportService;

  @Mock private QuestionTemplateRepository questionTemplateRepository;
  @Mock private Validator validator;
  @Mock private ObjectMapper objectMapper;

  private static final UUID CURRENT_USER_ID =
      UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private QuestionTemplateRequest buildTemplateRequest(
      String name, QuestionType questionType, CognitiveLevel cognitiveLevel) {
    return QuestionTemplateRequest.builder()
        .name(name)
        .description("Mau mo ta cho " + name)
        .templateType(questionType)
        .templateText(Map.of("vi", "Noi dung cau hoi cho " + name))
        .parameters(Map.of("a", Map.of("type", "int", "min", 1, "max", 10)))
        .answerFormula("(-b)/a")
        .diagramTemplate(null)
        .optionsGenerator(Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
        .constraints(new String[] {"a != 0"})
        .cognitiveLevel(cognitiveLevel)
        .tags(List.of(QuestionTag.LINEAR_EQUATIONS))
        .isPublic(true)
        .build();
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
        .constraints(new String[] {"x > 0"})
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

  private byte[] createWorkbookBytes(RowWriter rowWriter) throws IOException {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Question Templates");
      Row header = sheet.createRow(0);
      String[] headers = {
        "name",
        "description",
        "templateType",
        "templateText_vi",
        "parameters",
        "answerFormula",
        "diagramTemplate",
        "options",
        "cognitiveLevel",
        "tags",
        "isPublic"
      };
      for (int i = 0; i < headers.length; i++) {
        header.createCell(i).setCellValue(headers[i]);
      }
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

    /**
     * Normal case: Parse va validate thanh cong mot dong du lieu hop le.
     *
     * <p>Input:
     * <ul>
     *   <li>Excel file .xlsx co 1 data row hop le voi enum, JSON va tag dung format</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateExcelFile() -> pass tat ca dieu kien</li>
     *   <li>isRowEmpty() -> FALSE branch (dong co du lieu)</li>
     *   <li>parse enum/template JSON/options JSON -> nhanh parse thanh cong</li>
     *   <li>validateRequest() -> nhanh khong co loi</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Tra ve preview gom 1 dong hop le, khong co validation error</li>
     * </ul>
     */
    @Test
    void it_should_return_valid_preview_when_excel_contains_valid_row() throws Exception {
      // ===== ARRANGE =====
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Phuong trinh bac nhat");
                row.createCell(1).setCellValue("Mo ta phuong trinh bac nhat");
                row.createCell(2).setCellValue("MULTIPLE_CHOICE");
                row.createCell(3).setCellValue("Giai phuong trinh {{a}}x + {{b}} = 0");
                row.createCell(4).setCellValue("{\"a\":{\"type\":\"int\",\"min\":1,\"max\":10}}");
                row.createCell(5).setCellValue("(-b)/a");
                row.createCell(7).setCellValue("{\"A\":\"1\",\"B\":\"2\"}");
                row.createCell(8).setCellValue("THONG_HIEU");
                row.createCell(9).setCellValue("LINEAR_EQUATIONS, TRIANGLES");
                row.createCell(10).setCellValue("yes");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "question-template.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);
      when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of());
      when(objectMapper.readValue(Mockito.eq("{\"a\":{\"type\":\"int\",\"min\":1,\"max\":10}}"), Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(Map.of("a", Map.of("type", "int", "min", 1, "max", 10)));
      when(objectMapper.readValue(Mockito.eq("{\"A\":\"1\",\"B\":\"2\"}"), Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(Map.of("A", "1", "B", "2"));

      // ===== ACT =====
      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(1, result.getTotalRows());
      assertEquals(1, result.getValidRows());
      assertEquals(0, result.getInvalidRows());
      assertNotNull(result.getRows());
      assertEquals(1, result.getRows().size());
      ExcelPreviewResponse.PreviewRow previewRow = result.getRows().get(0);
      assertAll(
          () -> assertEquals(2, previewRow.getRowNumber()),
          () -> assertTrue(previewRow.getIsValid()),
          () -> assertNull(previewRow.getValidationErrors()),
          () -> assertNotNull(previewRow.getData()),
          () -> assertEquals(QuestionType.MULTIPLE_CHOICE, previewRow.getData().getTemplateType()),
          () -> assertEquals(CognitiveLevel.THONG_HIEU, previewRow.getData().getCognitiveLevel()),
          () -> assertTrue(previewRow.getData().getIsPublic()),
          () -> assertEquals(2, previewRow.getData().getTags().size()));

      // ===== VERIFY =====
      verify(validator, times(1)).validate(any(QuestionTemplateRequest.class));
      verify(objectMapper, times(1))
          .readValue(
              Mockito.eq("{\"a\":{\"type\":\"int\",\"min\":1,\"max\":10}}"),
              Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class));
      verify(objectMapper, times(1))
          .readValue(
              Mockito.eq("{\"A\":\"1\",\"B\":\"2\"}"),
              Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: Mot dong rong duoc bo qua, mot dong sai enum bi danh dau invalid.
     *
     * <p>Input:
     * <ul>
     *   <li>Excel file co 1 dong rong va 1 dong co templateType khong hop le</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isRowEmpty() -> TRUE branch (dong rong)</li>
     *   <li>parseRowToRequest() -> throw exception khi parse templateType</li>
     *   <li>catch(Exception) trong vong lap preview -> tao parse error row</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Chi co 1 dong trong ket qua va dong nay khong hop le voi parse error</li>
     * </ul>
     */
    @Test
    void it_should_mark_row_invalid_when_row_has_invalid_template_type_and_skip_empty_row()
        throws Exception {
      // ===== ARRANGE =====
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                sheet.createRow(1); // empty row
                Row invalidRow = sheet.createRow(2);
                invalidRow.createCell(0).setCellValue("Dang cau hoi sai type");
                invalidRow.createCell(1).setCellValue("Mo ta");
                invalidRow.createCell(2).setCellValue("KHONG_HOP_LE");
                invalidRow.createCell(3).setCellValue("Noi dung");
                invalidRow.createCell(4).setCellValue("{\"a\":1}");
                invalidRow.createCell(5).setCellValue("a");
                invalidRow.createCell(8).setCellValue("THONG_HIEU");
                invalidRow.createCell(9).setCellValue("LINEAR_EQUATIONS");
                invalidRow.createCell(10).setCellValue("false");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "invalid-template-type.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);

      // ===== ACT =====
      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(1, result.getTotalRows());
      assertEquals(0, result.getValidRows());
      assertEquals(1, result.getInvalidRows());
      assertNotNull(result.getRows());
      assertEquals(1, result.getRows().size());
      ExcelPreviewResponse.PreviewRow previewRow = result.getRows().get(0);
      assertAll(
          () -> assertEquals(3, previewRow.getRowNumber()),
          () -> assertFalse(previewRow.getIsValid()),
          () -> assertNull(previewRow.getData()),
          () -> assertNotNull(previewRow.getValidationErrors()),
          () ->
              assertTrue(
                  previewRow.getValidationErrors().get(0).contains("Invalid templateType: KHONG_HOP_LE")));

      // ===== VERIFY =====
      verify(validator, never()).validate(any(QuestionTemplateRequest.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: Validator tra ve violation va request thieu data bat buoc.
     *
     * <p>Input:
     * <ul>
     *   <li>Excel file co row hop le enum/JSON nhung templateText va parameters trong</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateRequest() -> co Bean Validation violation</li>
     *   <li>validateRequest() -> custom validation cho templateText/parameters</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Dong duoc danh dau invalid voi day du 3 loi validation</li>
     * </ul>
     */
    @Test
    void it_should_collect_validation_errors_when_request_has_constraint_violation_and_missing_fields()
        throws Exception {
      // ===== ARRANGE =====
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Bai toan toi uu");
                row.createCell(1).setCellValue("Mo ta bai toan");
                row.createCell(2).setCellValue("ESSAY");
                row.createCell(3).setCellValue("");
                row.createCell(4).setCellValue("");
                row.createCell(5).setCellValue("x+1");
                row.createCell(8).setCellValue("VAN_DUNG");
                row.createCell(9).setCellValue("LINEAR_EQUATIONS");
                row.createCell(10).setCellValue("1");
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

      // ===== ACT =====
      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(1, result.getTotalRows());
      assertEquals(0, result.getValidRows());
      assertEquals(1, result.getInvalidRows());
      List<String> errors = result.getRows().get(0).getValidationErrors();
      assertNotNull(errors);
      assertEquals(3, errors.size());
      assertTrue(errors.contains("name: Template name is required"));
      assertTrue(errors.contains("templateText: Template text is required"));
      assertTrue(errors.contains("parameters: Parameters are required"));

      // ===== VERIFY =====
      verify(validator, times(1)).validate(any(QuestionTemplateRequest.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: File upload khong hop le (rong/sai duoi/qua lon).
     *
     * <p>Input:
     * <ul>
     *   <li>3 truong hop file invalid: empty, wrong extension va qua 10MB</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateExcelFile(): file.isEmpty() -> TRUE branch</li>
     *   <li>validateExcelFile(): !filename.endsWith(.xlsx) -> TRUE branch</li>
     *   <li>validateExcelFile(): file.getSize() > 10MB -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Tat ca truong hop deu throw {@link AppException} voi {@code INVALID_KEY}</li>
     * </ul>
     */
    @Test
    void it_should_throw_invalid_key_when_excel_file_is_empty_or_wrong_extension_or_too_large()
        throws Exception {
      // ===== ARRANGE =====
      MockMultipartFile emptyFile =
          new MockMultipartFile(
              "file",
              "empty.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              new byte[0]);
      MockMultipartFile wrongExtensionFile =
          new MockMultipartFile(
              "file",
              "templates.csv",
              "text/csv",
              "name,description".getBytes());
      byte[] validSmallWorkbook = createWorkbookBytes(sheet -> {});
      byte[] oversizedContent = new byte[10 * 1024 * 1024 + 1];
      System.arraycopy(validSmallWorkbook, 0, oversizedContent, 0, validSmallWorkbook.length);
      MockMultipartFile oversizedFile =
          new MockMultipartFile(
              "file",
              "large.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              oversizedContent);

      // ===== ACT & ASSERT =====
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

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: Parse row that bai khi cognitiveLevel khong hop le.
     *
     * <p>Input:
     * <ul>
     *   <li>Excel file co cognitiveLevel = "KHONG_TON_TAI"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>parse cognitiveLevel -> IllegalArgumentException branch</li>
     *   <li>catch(Exception) trong vong lap preview</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Row bi danh dau invalid voi thong diep Invalid cognitiveLevel</li>
     * </ul>
     */
    @Test
    void it_should_mark_row_invalid_when_cognitive_level_is_not_supported() throws Exception {
      // ===== ARRANGE =====
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Bai tap vector");
                row.createCell(2).setCellValue("ESSAY");
                row.createCell(3).setCellValue("Noi dung bai tap");
                row.createCell(4).setCellValue("{\"x\":1}");
                row.createCell(5).setCellValue("x");
                row.createCell(8).setCellValue("KHONG_TON_TAI");
                row.createCell(9).setCellValue("LINEAR_EQUATIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "invalid-cognitive-level.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);

      // ===== ACT =====
      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertEquals(1, result.getInvalidRows());
      assertTrue(result.getRows().get(0).getValidationErrors().get(0).contains("Invalid cognitiveLevel"));

      // ===== VERIFY =====
      verify(validator, never()).validate(any(QuestionTemplateRequest.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: Parse row that bai khi parameters JSON khong dung dinh dang.
     *
     * <p>Input:
     * <ul>
     *   <li>Excel file co parameters JSON malformed</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>parse parameters JSON -> catch(Exception) branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Row bi danh dau invalid voi thong diep Invalid parameters JSON</li>
     * </ul>
     */
    @Test
    void it_should_mark_row_invalid_when_parameters_json_is_malformed() throws Exception {
      // ===== ARRANGE =====
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Bai toan thong ke");
                row.createCell(2).setCellValue("ESSAY");
                row.createCell(3).setCellValue("Noi dung");
                row.createCell(4).setCellValue("{invalid-json}");
                row.createCell(5).setCellValue("x");
                row.createCell(8).setCellValue("THONG_HIEU");
                row.createCell(9).setCellValue("LINEAR_EQUATIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "invalid-parameters-json.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);
      when(objectMapper.readValue(Mockito.eq("{invalid-json}"), Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenThrow(new IllegalArgumentException("Unexpected character"));

      // ===== ACT =====
      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertEquals(1, result.getInvalidRows());
      assertTrue(
          result.getRows().get(0).getValidationErrors().get(0).contains("Invalid parameters JSON"));

      // ===== VERIFY =====
      verify(objectMapper, times(1))
          .readValue(
              Mockito.eq("{invalid-json}"),
              Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class));
      verify(validator, never()).validate(any(QuestionTemplateRequest.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: Parse row that bai khi options JSON khong dung dinh dang.
     *
     * <p>Input:
     * <ul>
     *   <li>Excel file co options JSON malformed, parameters JSON hop le</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>parse options JSON -> catch(Exception) branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Row bi danh dau invalid voi thong diep Invalid options JSON</li>
     * </ul>
     */
    @Test
    void it_should_mark_row_invalid_when_options_json_is_malformed() throws Exception {
      // ===== ARRANGE =====
      byte[] bytes =
          createWorkbookBytes(
              sheet -> {
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Bai toan giai tich");
                row.createCell(2).setCellValue("MULTIPLE_CHOICE");
                row.createCell(3).setCellValue("Noi dung");
                row.createCell(4).setCellValue("{\"x\":1}");
                row.createCell(5).setCellValue("x");
                row.createCell(7).setCellValue("{invalid-options}");
                row.createCell(8).setCellValue("THONG_HIEU");
                row.createCell(9).setCellValue("LINEAR_EQUATIONS");
              });
      MockMultipartFile file =
          new MockMultipartFile(
              "file",
              "invalid-options-json.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              bytes);
      when(objectMapper.readValue(Mockito.eq("{\"x\":1}"), Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(Map.of("x", 1));
      when(objectMapper.readValue(Mockito.eq("{invalid-options}"), Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenThrow(new IllegalArgumentException("Unexpected character"));

      // ===== ACT =====
      ExcelPreviewResponse result = excelImportService.previewExcelImport(file);

      // ===== ASSERT =====
      assertEquals(1, result.getInvalidRows());
      assertTrue(result.getRows().get(0).getValidationErrors().get(0).contains("Invalid options JSON"));

      // ===== VERIFY =====
      verify(objectMapper, times(1))
          .readValue(
              Mockito.eq("{\"x\":1}"),
              Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class));
      verify(objectMapper, times(1))
          .readValue(
              Mockito.eq("{invalid-options}"),
              Mockito.any(com.fasterxml.jackson.core.type.TypeReference.class));
      verify(validator, never()).validate(any(QuestionTemplateRequest.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: File co extension .xlsx nhung noi dung bi hu hong.
     *
     * <p>Input:
     * <ul>
     *   <li>Multipart file .xlsx voi bytes khong phai workbook hop le</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>try(InputStream + XSSFWorkbook) cua preview -> throw exception</li>
     *   <li>catch(Exception) ben ngoai -> throw {@link AppException}</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Ngoai le INVALID_KEY duoc nem ra</li>
     * </ul>
     */
    @Test
    void it_should_throw_invalid_key_when_excel_content_is_corrupted() {
      // ===== ARRANGE =====
      MockMultipartFile corruptedFile =
          new MockMultipartFile(
              "file",
              "corrupted.xlsx",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
              "this-is-not-an-xlsx-content".getBytes());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> excelImportService.previewExcelImport(corruptedFile));
      assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: Filename null phai bi chan boi validateExcelFile.
     *
     * <p>Input:
     * <ul>
     *   <li>Multipart file mock co isEmpty = false, filename = null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateExcelFile(): filename == null -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Ngoai le INVALID_KEY duoc nem ra</li>
     * </ul>
     */
    @Test
    void it_should_throw_invalid_key_when_filename_is_null() {
      // ===== ARRANGE =====
      org.springframework.web.multipart.MultipartFile file =
          Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
      when(file.isEmpty()).thenReturn(false);
      when(file.getOriginalFilename()).thenReturn(null);

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> excelImportService.previewExcelImport(file));
      assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());

      // ===== VERIFY =====
      verify(file, times(1)).isEmpty();
      verify(file, times(2)).getOriginalFilename();
      verifyNoMoreInteractions(file, questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Normal case: Private parser phai xu ly day du cac kieu cell va null path.
     *
     * <p>Input:
     * <ul>
     *   <li>Row co du lieu STRING, NUMERIC, BOOLEAN, FORMULA, BLANK va cell null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getCellValueAsString(): columnIndex null branch</li>
     *   <li>getCellValueAsString(): cell null branch</li>
     *   <li>switch cell type: STRING/NUMERIC/BOOLEAN/FORMULA/default</li>
     *   <li>isRowEmpty(): nhanh cell != null nhung CellType.BLANK</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Gia tri string tra ve dung cho tung kieu cell, BLANK/default tra ve null</li>
     * </ul>
     */
    @Test
    void it_should_handle_all_cell_types_and_null_paths_when_getting_cell_value_as_string()
        throws Exception {
      // ===== ARRANGE =====
      Row row;
      try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("test");
        row = sheet.createRow(0);
        row.createCell(0).setCellValue("ALPHA");
        row.createCell(1).setCellValue(123D);
        row.createCell(2).setCellValue(true);
        row.createCell(3).setCellFormula("A1");
        row.createCell(4, CellType.BLANK);

        // ===== ACT =====
        String nullIndexValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, null);
        String nullCellValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 10);
        String stringValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 0);
        String numericValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 1);
        String booleanValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 2);
        String formulaValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 3);
        String blankValue =
            (String)
                invokePrivateMethod(
                    "getCellValueAsString", new Class<?>[] {Row.class, Integer.class}, row, 4);
        Boolean isEmpty =
            (Boolean)
                invokePrivateMethod("isRowEmpty", new Class<?>[] {Row.class}, sheet.createRow(1));
        Row blankCellRow = sheet.createRow(2);
        blankCellRow.createCell(0, CellType.BLANK);
        Boolean isEmptyWithBlankCell =
            (Boolean) invokePrivateMethod("isRowEmpty", new Class<?>[] {Row.class}, blankCellRow);

        // ===== ASSERT =====
        assertAll(
            () -> assertNull(nullIndexValue),
            () -> assertNull(nullCellValue),
            () -> assertEquals("ALPHA", stringValue),
            () -> assertEquals("123", numericValue),
            () -> assertEquals("true", booleanValue),
            () -> assertEquals("A1", formulaValue),
            () -> assertNull(blankValue),
            () -> assertTrue(isEmpty),
            () -> assertTrue(isEmptyWithBlankCell));
      }

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Normal case: parseRowToRequest va validateRequest xu ly du lieu optional/blank dung.
     *
     * <p>Input:
     * <ul>
     *   <li>Row co templateType va cognitiveLevel de trong, tags gom Vietnamese + unknown</li>
     *   <li>Request validate co day du templateText va parameters</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>parse templateType/cognitiveLevel -> nhanh bo qua khi blank</li>
     *   <li>parse tags -> enum fail, Vietnamese fallback success va fallback null</li>
     *   <li>parse isPublic -> nhanh non-blank nhung FALSE value</li>
     *   <li>validateRequest() -> ca 2 custom checks di vao FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Request parse ra dung, co duy nhat tag TRIANGLES va validateRequest khong co loi</li>
     * </ul>
     */
    @Test
    void it_should_parse_optional_blank_fields_and_validate_without_errors_when_data_is_sufficient()
        throws Exception {
      // ===== ARRANGE =====
      try (Workbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("test");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("Bai toan hinh hoc");
        row.createCell(1).setCellValue("Mo ta hinh hoc");
        row.createCell(2).setCellValue("");
        row.createCell(3).setCellValue("");
        row.createCell(4).setCellValue("");
        row.createCell(5).setCellValue("x+1");
        row.createCell(7).setCellValue("");
        row.createCell(8).setCellValue("");
        row.createCell(9).setCellValue("Tam giác, KHONG_HOP_LE");
        row.createCell(10).setCellValue("0");
        when(validator.validate(any(QuestionTemplateRequest.class))).thenReturn(Set.of());

        // ===== ACT =====
        QuestionTemplateRequest parsed =
            (QuestionTemplateRequest)
                invokePrivateMethod(
                    "parseRowToRequest", new Class<?>[] {Row.class, int.class}, row, 1);
        parsed.setTemplateText(Map.of("vi", "Noi dung du"));
        parsed.setParameters(new HashMap<>(Map.of("x", 1)));
        List<String> validationErrors =
            (List<String>)
                invokePrivateMethod(
                    "validateRequest",
                    new Class<?>[] {QuestionTemplateRequest.class},
                    parsed);

        // ===== ASSERT =====
        assertAll(
            () -> assertNull(parsed.getTemplateType()),
            () -> assertNull(parsed.getCognitiveLevel()),
            () -> assertNotNull(parsed.getTags()),
            () -> assertEquals(1, parsed.getTags().size()),
            () -> assertEquals(QuestionTag.TRIANGLES, parsed.getTags().get(0)),
            () -> assertFalse(parsed.getIsPublic()),
            () -> assertTrue(validationErrors.isEmpty()));
      }

      // ===== VERIFY =====
      verify(validator, times(1)).validate(any(QuestionTemplateRequest.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }
  }

  @Nested
  @DisplayName("importTemplatesBatch()")
  class ImportTemplatesBatchTests {

    /**
     * Normal case: Import thanh cong tat ca templates trong request.
     *
     * <p>Input:
     * <ul>
     *   <li>Batch request gom 2 template hop le</li>
     *   <li>SecurityUtils tra ve current user id hop le</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>for-loop import -> nhanh success (save khong throw)</li>
     *   <li>createTemplateEntity(): request.getIsPublic() != null -> dung gia tri request</li>
     *   <li>errors.isEmpty() -> TRUE branch (response.errors = null)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>successCount = 2, failedCount = 0, danh sach errors = null</li>
     * </ul>
     */
    @Test
    void it_should_import_all_templates_successfully_when_all_rows_are_valid() {
      // ===== ARRANGE =====
      QuestionTemplateRequest first =
          buildTemplateRequest("Phuong trinh bac hai", QuestionType.MULTIPLE_CHOICE, CognitiveLevel.NHAN_BIET);
      QuestionTemplateRequest second =
          buildTemplateRequest("Gioi han day so", QuestionType.ESSAY, CognitiveLevel.VAN_DUNG);
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

      // ===== ACT =====
      TemplateBatchImportResponse result;
      try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);
        result = excelImportService.importTemplatesBatch(request);
        securityUtilsMock.verify(SecurityUtils::getCurrentUserId, times(1));
      }

      // ===== ASSERT =====
      assertNotNull(result);
      assertAll(
          () -> assertEquals(2, result.getTotalRows()),
          () -> assertEquals(2, result.getSuccessCount()),
          () -> assertEquals(0, result.getFailedCount()),
          () -> assertNull(result.getErrors()),
          () -> assertNotNull(result.getSuccessfulTemplates()),
          () -> assertEquals(2, result.getSuccessfulTemplates().size()),
          () -> assertEquals("Phuong trinh bac hai", result.getSuccessfulTemplates().get(0).getName()),
          () -> assertEquals("Gioi han day so", result.getSuccessfulTemplates().get(1).getName()),
          () -> assertEquals(TemplateStatus.DRAFT, result.getSuccessfulTemplates().get(0).getStatus()));

      // ===== VERIFY =====
      verify(questionTemplateRepository, times(2)).save(any(QuestionTemplate.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Abnormal case: Co row save bi loi trong qua trinh import.
     *
     * <p>Input:
     * <ul>
     *   <li>Batch request 2 templates, row dau save throw exception, row sau thanh cong</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>for-loop import -> catch(Exception) branch cho row loi</li>
     *   <li>for-loop import -> success branch cho row tiep theo</li>
     *   <li>errors.isEmpty() -> FALSE branch (response.errors co du lieu)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Response ghi nhan dung rowNumber, rowName va message loi</li>
     * </ul>
     */
    @Test
    void it_should_collect_error_details_when_some_template_rows_fail_to_import() {
      // ===== ARRANGE =====
      QuestionTemplateRequest first =
          buildTemplateRequest("Bai toan toi uu hoa", QuestionType.ESSAY, CognitiveLevel.VAN_DUNG_CAO);
      QuestionTemplateRequest second =
          buildTemplateRequest("Do thi ham bac ba", QuestionType.MULTIPLE_CHOICE, CognitiveLevel.THONG_HIEU);
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

      // ===== ACT =====
      TemplateBatchImportResponse result;
      try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);
        result = excelImportService.importTemplatesBatch(request);
      }

      // ===== ASSERT =====
      assertNotNull(result);
      assertAll(
          () -> assertEquals(2, result.getTotalRows()),
          () -> assertEquals(1, result.getSuccessCount()),
          () -> assertEquals(1, result.getFailedCount()),
          () -> assertNotNull(result.getErrors()),
          () -> assertEquals(1, result.getErrors().size()),
          () -> assertEquals(1, result.getErrors().get(0).getRowNumber()),
          () -> assertEquals("Bai toan toi uu hoa", result.getErrors().get(0).getRowName()),
          () -> assertEquals("general", result.getErrors().get(0).getField()),
          () -> assertEquals("Duplicate template name", result.getErrors().get(0).getMessage()),
          () -> assertEquals(1, result.getSuccessfulTemplates().size()),
          () -> assertEquals("Do thi ham bac ba", result.getSuccessfulTemplates().get(0).getName()));

      // ===== VERIFY =====
      verify(questionTemplateRepository, times(2)).save(any(QuestionTemplate.class));
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }

    /**
     * Normal case: isPublic null trong request phai duoc default ve false.
     *
     * <p>Input:
     * <ul>
     *   <li>Batch request co 1 template voi isPublic = null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>createTemplateEntity(): request.getIsPublic() == null -> TRUE branch default false</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Entity luu xuong repository co gia tri isPublic = false</li>
     * </ul>
     */
    @Test
    void it_should_default_is_public_to_false_when_request_is_public_is_null() {
      // ===== ARRANGE =====
      QuestionTemplateRequest requestItem =
          buildTemplateRequest("He phuong trinh", QuestionType.ESSAY, CognitiveLevel.NHAN_BIET);
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

      // ===== ACT =====
      TemplateBatchImportResponse result;
      try (MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);
        result = excelImportService.importTemplatesBatch(request);
      }

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(1, result.getSuccessCount());
      assertEquals(0, result.getFailedCount());

      // ===== VERIFY =====
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
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }
  }

  @Nested
  @DisplayName("generateExcelTemplate()")
  class GenerateExcelTemplateTests {

    /**
     * Normal case: Tao file mau excel thanh cong.
     *
     * <p>Input:
     * <ul>
     *   <li>Khong co input parameter</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>generateExcelTemplate() -> happy path ghi workbook thanh cong</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Byte array tra ve co noi dung, workbook co header + 2 dong example dung cot</li>
     * </ul>
     */
    @Test
    void it_should_generate_excel_template_with_headers_and_example_rows() throws Exception {
      // ===== ACT =====
      byte[] result = excelImportService.generateExcelTemplate();

      // ===== ASSERT =====
      assertNotNull(result);
      assertTrue(result.length > 0);
      try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
        Sheet sheet = workbook.getSheetAt(0);
        assertNotNull(sheet);
        assertEquals("Question Templates", sheet.getSheetName());
        assertEquals("name", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("templateText_vi", sheet.getRow(0).getCell(3).getStringCellValue());
        assertEquals("isPublic", sheet.getRow(0).getCell(10).getStringCellValue());
        assertEquals("Giải phương trình bậc nhất", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals("MULTIPLE_CHOICE", sheet.getRow(1).getCell(2).getStringCellValue());
        assertEquals("VAN_DUNG_CAO", sheet.getRow(2).getCell(8).getStringCellValue());
        assertEquals("true", sheet.getRow(2).getCell(10).getStringCellValue());
      }

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionTemplateRepository, validator, objectMapper);
    }
  }
}
