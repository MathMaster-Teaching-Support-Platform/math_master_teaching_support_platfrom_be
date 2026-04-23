package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.dto.request.QuestionBatchImportRequest;
import com.fptu.math_master.dto.response.QuestionBatchImportResponse;
import com.fptu.math_master.dto.response.QuestionExcelPreviewResponse;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.service.QuestionExcelImportService;
import com.fptu.math_master.service.QuestionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionExcelImportServiceImpl implements QuestionExcelImportService {

  QuestionService questionService;
  Validator validator;
  ObjectMapper objectMapper;

  private static final int HEADER_ROW = 0;
  private static final Map<String, Integer> COLUMN_MAPPING = new LinkedHashMap<>();

  static {
    COLUMN_MAPPING.put("questionText", 0);
    COLUMN_MAPPING.put("questionType", 1);
    COLUMN_MAPPING.put("cognitiveLevel", 2);
    COLUMN_MAPPING.put("points", 3);
    COLUMN_MAPPING.put("correctAnswer", 4);
    COLUMN_MAPPING.put("explanation", 5);
    COLUMN_MAPPING.put("tags", 6);
    COLUMN_MAPPING.put("options", 7);
  }

  @Override
  public QuestionExcelPreviewResponse previewExcelImport(MultipartFile file) {
    log.info("Previewing question Excel import: {}", file.getOriginalFilename());

    validateExcelFile(file);

    List<QuestionExcelPreviewResponse.PreviewRow> previewRows = new ArrayList<>();
    int validCount = 0;
    int invalidCount = 0;

    try (InputStream is = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      int lastRow = sheet.getLastRowNum();

      for (int rowIdx = HEADER_ROW + 1; rowIdx <= lastRow; rowIdx++) {
        Row row = sheet.getRow(rowIdx);
        if (row == null || isRowEmpty(row)) {
          continue;
        }

        try {
          CreateQuestionRequest request = parseRowToRequest(row, rowIdx);
          List<String> errors = validateRequest(request);

          if (errors.isEmpty()) {
            validCount++;
            previewRows.add(
                QuestionExcelPreviewResponse.PreviewRow.builder()
                    .rowNumber(rowIdx + 1)
                    .isValid(true)
                    .data(request)
                    .validationErrors(null)
                    .build());
          } else {
            invalidCount++;
            previewRows.add(
                QuestionExcelPreviewResponse.PreviewRow.builder()
                    .rowNumber(rowIdx + 1)
                    .isValid(false)
                    .data(null)
                    .validationErrors(errors)
                    .build());
          }
        } catch (Exception e) {
          invalidCount++;
          previewRows.add(
              QuestionExcelPreviewResponse.PreviewRow.builder()
                  .rowNumber(rowIdx + 1)
                  .isValid(false)
                  .data(null)
                  .validationErrors(List.of("Row parsing error: " + e.getMessage()))
                  .build());
        }
      }
    } catch (AppException ae) {
      throw ae;
    } catch (Exception e) {
      log.error("Failed to preview question Excel import", e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    return QuestionExcelPreviewResponse.builder()
        .totalRows(validCount + invalidCount)
        .validRows(validCount)
        .invalidRows(invalidCount)
        .rows(previewRows)
        .build();
  }

  @Override
  @Transactional
  public QuestionBatchImportResponse importQuestionsBatch(QuestionBatchImportRequest request) {
    log.info("Batch importing {} questions", request.getQuestions().size());

    int successCount = 0;
    int failedCount = 0;
    List<String> errors = new ArrayList<>();

    for (int i = 0; i < request.getQuestions().size(); i++) {
      CreateQuestionRequest questionRequest = request.getQuestions().get(i);
      try {
        questionService.createQuestion(questionRequest);
        successCount++;
      } catch (Exception e) {
        failedCount++;
        errors.add("Row " + (i + 1) + ": " + e.getMessage());
        log.error("Failed to import question at index {}: {}", i, e.getMessage());
      }
    }

    return QuestionBatchImportResponse.builder()
        .totalRows(request.getQuestions().size())
        .successCount(successCount)
        .failedCount(failedCount)
        .errors(errors.isEmpty() ? null : errors)
        .build();
  }

  @Override
  public byte[] generateExcelTemplate() {
    log.info("Generating Excel template for question import");

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Questions");

      // Header row
      Row headerRow = sheet.createRow(0);
      String[] headers = {
        "questionText",
        "questionType",
        "cognitiveLevel",
        "points",
        "correctAnswer",
        "explanation",
        "tags",
        "options"
      };
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      // Notes row
      Row notesRow = sheet.createRow(1);
      notesRow.createCell(0).setCellValue("(Bắt buộc) Nội dung câu hỏi - Dùng {{tên_tham_số}} cho câu hỏi động");
      notesRow.createCell(1).setCellValue("(Bắt buộc) CHỈ MULTIPLE_CHOICE");
      notesRow.createCell(2).setCellValue("(Bắt buộc) NHAN_BIET | THONG_HIEU | VAN_DUNG | VAN_DUNG_CAO");
      notesRow.createCell(3).setCellValue("(Tuỳ chọn) Điểm, mặc định 1.0");
      notesRow.createCell(4).setCellValue("(Bắt buộc) Đáp án đúng: A, B, C hoặc D");
      notesRow.createCell(5).setCellValue("(Tuỳ chọn) Giải thích đáp án");
      notesRow.createCell(6).setCellValue("(Tuỳ chọn) Nhãn phân cách bằng dấu phẩy, ví dụ: đại số, lớp 10");
      notesRow.createCell(7).setCellValue("(Bắt buộc) JSON với đúng 4 đáp án A, B, C, D");

      // Example row 1: Static question
      Row example1 = sheet.createRow(2);
      example1.createCell(0).setCellValue("Số nào là số nguyên tố?");
      example1.createCell(1).setCellValue("MULTIPLE_CHOICE");
      example1.createCell(2).setCellValue("NHAN_BIET");
      example1.createCell(3).setCellValue("1");
      example1.createCell(4).setCellValue("C");
      example1.createCell(5).setCellValue("Số nguyên tố là số chỉ chia hết cho 1 và chính nó.");
      example1.createCell(6).setCellValue("toán, số học, lớp 6");
      example1.createCell(7).setCellValue("{\"A\":\"4\",\"B\":\"6\",\"C\":\"7\",\"D\":\"9\"}");

      // Example row 2: Dynamic question with {{}} parameters
      Row example2 = sheet.createRow(3);
      example2.createCell(0).setCellValue("Tính diện tích hình tròn có bán kính {{r}} cm");
      example2.createCell(1).setCellValue("MULTIPLE_CHOICE");
      example2.createCell(2).setCellValue("THONG_HIEU");
      example2.createCell(3).setCellValue("1");
      example2.createCell(4).setCellValue("A");
      example2.createCell(5).setCellValue("Công thức: S = π × r²");
      example2.createCell(6).setCellValue("hình học, diện tích, lớp 9");
      example2.createCell(7).setCellValue("{\"A\":\"{{3.14 * r * r}}\",\"B\":\"{{2 * 3.14 * r}}\",\"C\":\"{{r * r}}\",\"D\":\"{{3.14 * r}}\"}");

      // Instructions sheet
      Sheet instructionsSheet = workbook.createSheet("Hướng dẫn");
      
      int rowNum = 0;
      Row titleRow = instructionsSheet.createRow(rowNum++);
      titleRow.createCell(0).setCellValue("HƯỚNG DẪN IMPORT CÂU HỎI");
      
      instructionsSheet.createRow(rowNum++); // Empty row
      
      Row rule1 = instructionsSheet.createRow(rowNum++);
      rule1.createCell(0).setCellValue("QUY TẮC BẮT BUỘC:");
      
      Row rule2 = instructionsSheet.createRow(rowNum++);
      rule2.createCell(0).setCellValue("1. Loại câu hỏi CHỈ được là MULTIPLE_CHOICE (Trắc nghiệm)");
      
      Row rule3 = instructionsSheet.createRow(rowNum++);
      rule3.createCell(0).setCellValue("2. Phải có đúng 4 đáp án với nhãn A, B, C, D");
      
      Row rule4 = instructionsSheet.createRow(rowNum++);
      rule4.createCell(0).setCellValue("3. Đáp án đúng phải là A, B, C hoặc D");
      
      Row rule5 = instructionsSheet.createRow(rowNum++);
      rule5.createCell(0).setCellValue("4. Định dạng options phải là JSON hợp lệ");
      
      instructionsSheet.createRow(rowNum++); // Empty row
      
      Row paramTitle = instructionsSheet.createRow(rowNum++);
      paramTitle.createCell(0).setCellValue("ĐỊNH DẠNG THAM SỐ {{}}:");
      
      Row param1 = instructionsSheet.createRow(rowNum++);
      param1.createCell(0).setCellValue("• Dùng {{tên_tham_số}} cho câu hỏi động");
      
      Row param2 = instructionsSheet.createRow(rowNum++);
      param2.createCell(0).setCellValue("• Ví dụ: {{r}}, {{a}}, {{b}}, {{height}}");
      
      Row param3 = instructionsSheet.createRow(rowNum++);
      param3.createCell(0).setCellValue("• KHÔNG dùng {tên} (ngoặc đơn)");
      
      Row param4 = instructionsSheet.createRow(rowNum++);
      param4.createCell(0).setCellValue("• KHÔNG dùng $tên hoặc định dạng khác");
      
      instructionsSheet.createRow(rowNum++); // Empty row
      
      Row exampleTitle = instructionsSheet.createRow(rowNum++);
      exampleTitle.createCell(0).setCellValue("VÍ DỤ OPTIONS JSON:");
      
      Row example1Title = instructionsSheet.createRow(rowNum++);
      example1Title.createCell(0).setCellValue("Câu hỏi tĩnh:");
      
      Row example1Json = instructionsSheet.createRow(rowNum++);
      example1Json.createCell(0).setCellValue("{\"A\":\"4\",\"B\":\"6\",\"C\":\"7\",\"D\":\"9\"}");
      
      instructionsSheet.createRow(rowNum++); // Empty row
      
      Row example2Title = instructionsSheet.createRow(rowNum++);
      example2Title.createCell(0).setCellValue("Câu hỏi động với tham số:");
      
      Row example2Json = instructionsSheet.createRow(rowNum++);
      example2Json.createCell(0).setCellValue("{\"A\":\"{{3.14 * r * r}}\",\"B\":\"{{2 * 3.14 * r}}\",\"C\":\"{{r * r}}\",\"D\":\"{{3.14 * r}}\"}");
      
      instructionsSheet.createRow(rowNum++); // Empty row
      
      Row errorTitle = instructionsSheet.createRow(rowNum++);
      errorTitle.createCell(0).setCellValue("LỖI THƯỜNG GẶP:");
      
      Row error1 = instructionsSheet.createRow(rowNum++);
      error1.createCell(0).setCellValue("❌ Dùng loại câu hỏi khác MULTIPLE_CHOICE");
      
      Row error2 = instructionsSheet.createRow(rowNum++);
      error2.createCell(0).setCellValue("❌ Thiếu hoặc thừa đáp án (không đúng 4 đáp án)");
      
      Row error3 = instructionsSheet.createRow(rowNum++);
      error3.createCell(0).setCellValue("❌ Đáp án đúng không phải A, B, C hoặc D");
      
      Row error4 = instructionsSheet.createRow(rowNum++);
      error4.createCell(0).setCellValue("❌ JSON không hợp lệ (thiếu dấu ngoặc, dấu phẩy)");
      
      Row error5 = instructionsSheet.createRow(rowNum++);
      error5.createCell(0).setCellValue("❌ Dùng {param} thay vì {{param}}");

      // Auto-size columns
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }
      for (int i = 0; i < 2; i++) {
        instructionsSheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();

    } catch (Exception e) {
      log.error("Failed to generate question Excel template", e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  private void validateExcelFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private boolean isRowEmpty(Row row) {
    for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
      Cell cell = row.getCell(c);
      if (cell != null) {
        String value = getCellValueAsString(row, c);
        if (value != null && !value.isBlank()) {
          return false;
        }
      }
    }
    return true;
  }

  private CreateQuestionRequest parseRowToRequest(Row row, int rowIndex) throws Exception {
    String questionText = getCellValueAsString(row, COLUMN_MAPPING.get("questionText"));
    String questionTypeStr = getCellValueAsString(row, COLUMN_MAPPING.get("questionType"));
    String cognitiveLevelStr = getCellValueAsString(row, COLUMN_MAPPING.get("cognitiveLevel"));
    String pointsStr = getCellValueAsString(row, COLUMN_MAPPING.get("points"));
    String correctAnswer = getCellValueAsString(row, COLUMN_MAPPING.get("correctAnswer"));
    String explanation = getCellValueAsString(row, COLUMN_MAPPING.get("explanation"));
    String tagsStr = getCellValueAsString(row, COLUMN_MAPPING.get("tags"));
    String optionsJson = getCellValueAsString(row, COLUMN_MAPPING.get("options"));

    // Parse questionType
    QuestionType questionType = null;
    if (questionTypeStr != null && !questionTypeStr.isBlank()) {
      try {
        questionType = QuestionType.valueOf(questionTypeStr.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new Exception("Invalid questionType: " + questionTypeStr);
      }
    }

    // Parse cognitiveLevel
    CognitiveLevel cognitiveLevel = null;
    if (cognitiveLevelStr != null && !cognitiveLevelStr.isBlank()) {
      try {
        cognitiveLevel = CognitiveLevel.valueOf(cognitiveLevelStr.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new Exception("Invalid cognitiveLevel: " + cognitiveLevelStr);
      }
    }

    // Parse points
    BigDecimal points = BigDecimal.valueOf(1.0);
    if (pointsStr != null && !pointsStr.isBlank()) {
      try {
        points = new BigDecimal(pointsStr.trim());
      } catch (NumberFormatException e) {
        throw new Exception("Invalid points value: " + pointsStr);
      }
    }

    // Parse tags
    String[] tags = null;
    if (tagsStr != null && !tagsStr.isBlank()) {
      tags = tagsStr.split(",");
      for (int i = 0; i < tags.length; i++) {
        tags[i] = tags[i].trim();
      }
    }

    // Parse options JSON
    Map<String, Object> options = null;
    if (optionsJson != null && !optionsJson.isBlank()) {
      try {
        options = objectMapper.readValue(optionsJson, new TypeReference<>() {});
      } catch (Exception e) {
        throw new Exception("Invalid options JSON: " + e.getMessage());
      }
    }

    return CreateQuestionRequest.builder()
        .questionText(questionText)
        .questionType(questionType)
        .cognitiveLevel(cognitiveLevel)
        .points(points)
        .correctAnswer(correctAnswer)
        .explanation(explanation)
        .tags(tags)
        .options(options)
        .build();
  }

  private String getCellValueAsString(Row row, Integer columnIndex) {
    if (columnIndex == null) {
      return null;
    }
    Cell cell = row.getCell(columnIndex);
    if (cell == null) {
      return null;
    }
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> null;
    };
  }

  private List<String> validateRequest(CreateQuestionRequest request) {
    List<String> errors = new ArrayList<>();
    
    // Standard validation
    Set<ConstraintViolation<CreateQuestionRequest>> violations = validator.validate(request);
    for (ConstraintViolation<CreateQuestionRequest> violation : violations) {
      errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
    }
    
    // RULE 1: Only MULTIPLE_CHOICE type is allowed
    if (request.getQuestionType() != QuestionType.MULTIPLE_CHOICE) {
      errors.add("Chỉ cho phép loại câu hỏi MULTIPLE_CHOICE (Trắc nghiệm). Loại hiện tại: " + request.getQuestionType());
    }
    
    // RULE 2: Must have exactly 4 options (A, B, C, D)
    if (request.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
      if (request.getOptions() == null || request.getOptions().size() != 4) {
        errors.add("Câu hỏi trắc nghiệm phải có đúng 4 đáp án (A, B, C, D). Số đáp án hiện tại: " 
            + (request.getOptions() == null ? 0 : request.getOptions().size()));
      } else {
        // Check option keys are A, B, C, D
        Set<String> expectedKeys = Set.of("A", "B", "C", "D");
        Set<String> actualKeys = request.getOptions().keySet();
        if (!actualKeys.equals(expectedKeys)) {
          errors.add("Các đáp án phải được đánh nhãn A, B, C, D. Nhãn hiện tại: " + actualKeys);
        }
      }
      
      // Check correct answer is one of A, B, C, D
      if (request.getCorrectAnswer() != null) {
        String correctAnswer = request.getCorrectAnswer().trim().toUpperCase();
        if (!correctAnswer.matches("^[A-D]$")) {
          errors.add("Đáp án đúng phải là A, B, C hoặc D. Giá trị hiện tại: " + request.getCorrectAnswer());
        }
      }
    }
    
    // RULE 3: Recommend {{}} parameter format (warning, not error)
    if (request.getQuestionText() != null && !request.getQuestionText().contains("{{")) {
      // This is just a warning - we'll log it but not block import
      log.warn("Question text does not contain {{}} parameters. Consider using {{param}} format for dynamic questions.");
    }
    
    return errors;
  }
}
