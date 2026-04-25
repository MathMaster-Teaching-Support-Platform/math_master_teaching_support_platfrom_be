package com.fptu.math_master.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.QuestionTemplateBatchImportRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.ExcelPreviewResponse;
import com.fptu.math_master.dto.response.TemplateBatchImportResponse;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionTag;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.service.impl.ExcelImportServiceImpl;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ExcelImportServiceTest {

  @Mock private QuestionTemplateRepository questionTemplateRepository;

  @Mock private Validator validator;

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private ExcelImportServiceImpl excelImportService;

  private MultipartFile validExcelFile;
  private QuestionTemplateRequest validRequest;

  @BeforeEach
  void setUp() throws Exception {
    // Create valid request
    Map<String, Object> templateText = new HashMap<>();
    templateText.put("vi", "Giải: {{a}}x + {{b}} = 0");

    Map<String, Object> parameters = new HashMap<>();
    Map<String, Object> paramA = new HashMap<>();
    paramA.put("type", "int");
    paramA.put("min", 1);
    paramA.put("max", 10);
    parameters.put("a", paramA);

    validRequest =
        QuestionTemplateRequest.builder()
            .name("Test Template")
            .description("Test Description")
            .templateType(QuestionType.MULTIPLE_CHOICE)
            .templateText(templateText)
            .parameters(parameters)
            .answerFormula("(-b)/a")
            .cognitiveLevel(CognitiveLevel.THONG_HIEU)
            .tags(List.of(QuestionTag.LINEAR_EQUATIONS, QuestionTag.PROBLEM_SOLVING))
            .isPublic(false)
            .build();

    // Create valid Excel file
    validExcelFile = createValidExcelFile();
  }

  @Test
  void testPreviewExcelImport_ValidFile() throws Exception {
    // Given
    when(validator.validate(any())).thenReturn(Collections.emptySet());

    // When
    ExcelPreviewResponse response = excelImportService.previewExcelImport(validExcelFile);

    // Then
    assertNotNull(response);
    assertTrue(response.getTotalRows() > 0);
    assertEquals(response.getTotalRows(), response.getValidRows() + response.getInvalidRows());
  }

  @Test
  void testImportTemplatesBatch_Success() {
    // Given
    QuestionTemplateBatchImportRequest request =
        QuestionTemplateBatchImportRequest.builder()
            .templates(List.of(validRequest))
            .build();

    QuestionTemplate savedTemplate = new QuestionTemplate();
    savedTemplate.setName("Test Template");

    when(questionTemplateRepository.save(any())).thenReturn(savedTemplate);

    // When
    TemplateBatchImportResponse response = excelImportService.importTemplatesBatch(request);

    // Then
    assertNotNull(response);
    assertEquals(1, response.getTotalRows());
    assertEquals(1, response.getSuccessCount());
    assertEquals(0, response.getFailedCount());
    verify(questionTemplateRepository, times(1)).save(any());
  }

  @Test
  void testImportTemplatesBatch_PartialFailure() {
    // Given
    QuestionTemplateRequest invalidRequest =
        QuestionTemplateRequest.builder()
            .name(null) // Invalid: name is required
            .build();

    QuestionTemplateBatchImportRequest request =
        QuestionTemplateBatchImportRequest.builder()
            .templates(List.of(validRequest, invalidRequest))
            .build();

    QuestionTemplate savedTemplate = new QuestionTemplate();
    when(questionTemplateRepository.save(any()))
        .thenReturn(savedTemplate)
        .thenThrow(new RuntimeException("Validation failed"));

    // When
    TemplateBatchImportResponse response = excelImportService.importTemplatesBatch(request);

    // Then
    assertNotNull(response);
    assertEquals(2, response.getTotalRows());
    assertTrue(response.getSuccessCount() >= 1);
    assertTrue(response.getFailedCount() >= 1);
  }

  @Test
  void testGenerateExcelTemplate() {
    // When
    byte[] template = excelImportService.generateExcelTemplate();

    // Then
    assertNotNull(template);
    assertTrue(template.length > 0);

    // Verify it's a valid Excel file
    assertDoesNotThrow(
        () -> {
          try (InputStream is = new ByteArrayInputStream(template);
              Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            assertTrue(sheet.getPhysicalNumberOfRows() > 0);

            // Verify headers
            Row headerRow = sheet.getRow(0);
            assertEquals("name", headerRow.getCell(0).getStringCellValue());
            assertEquals("templateType", headerRow.getCell(2).getStringCellValue());
          }
        });
  }

  @Test
  void testPreviewExcelImport_InvalidFileType() {
    // Given
    MultipartFile invalidFile =
        new MockMultipartFile("file", "test.txt", "text/plain", "invalid content".getBytes());

    // When & Then
    assertThrows(Exception.class, () -> excelImportService.previewExcelImport(invalidFile));
  }

  @Test
  void testPreviewExcelImport_EmptyFile() {
    // Given
    MultipartFile emptyFile =
        new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", new byte[0]);

    // When & Then
    assertThrows(Exception.class, () -> excelImportService.previewExcelImport(emptyFile));
  }

  private MultipartFile createValidExcelFile() throws Exception {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Templates");

      // Create header row
      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("name");
      headerRow.createCell(1).setCellValue("description");
      headerRow.createCell(2).setCellValue("templateType");
      headerRow.createCell(3).setCellValue("templateText_vi");
      headerRow.createCell(4).setCellValue("parameters");
      headerRow.createCell(5).setCellValue("answerFormula");
      headerRow.createCell(6).setCellValue("options");
      headerRow.createCell(7).setCellValue("cognitiveLevel");
      headerRow.createCell(8).setCellValue("tags");
      headerRow.createCell(9).setCellValue("isPublic");

      // Create data row
      Row dataRow = sheet.createRow(1);
      dataRow.createCell(0).setCellValue("Test Template");
      dataRow.createCell(1).setCellValue("Test Description");
      dataRow.createCell(2).setCellValue("MULTIPLE_CHOICE");
      dataRow.createCell(3).setCellValue("Giải: {{a}}x + {{b}} = 0");
      dataRow
          .createCell(4)
          .setCellValue("{\"a\":{\"type\":\"int\",\"min\":1,\"max\":10}}");
      dataRow.createCell(5).setCellValue("(-b)/a");
      dataRow.createCell(6).setCellValue("{\"A\":\"(-b)/a\",\"B\":\"b/a\"}");
      dataRow.createCell(7).setCellValue("THONG_HIEU");
      dataRow.createCell(8).setCellValue("test, algebra");
      dataRow.createCell(9).setCellValue("false");

      // Write to byte array
      java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
      workbook.write(bos);
      return new MockMultipartFile(
          "file",
          "test.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          bos.toByteArray());
    }
  }
}
