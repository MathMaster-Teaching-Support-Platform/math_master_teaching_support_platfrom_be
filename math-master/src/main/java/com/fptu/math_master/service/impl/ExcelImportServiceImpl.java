package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.QuestionTemplateBatchImportRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.ExcelPreviewResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateBatchImportResponse;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.service.ExcelImportService;
import com.fptu.math_master.util.SecurityUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
public class ExcelImportServiceImpl implements ExcelImportService {

  QuestionTemplateRepository questionTemplateRepository;
  Validator validator;
  ObjectMapper objectMapper;

  private static final int HEADER_ROW = 0;
  private static final Map<String, Integer> COLUMN_MAPPING = new LinkedHashMap<>();

  static {
    COLUMN_MAPPING.put("name", 0);
    COLUMN_MAPPING.put("description", 1);
    COLUMN_MAPPING.put("templateType", 2);
    COLUMN_MAPPING.put("templateText_vi", 3);
    COLUMN_MAPPING.put("parameters", 4);
    COLUMN_MAPPING.put("answerFormula", 5);
    COLUMN_MAPPING.put("diagramTemplate", 6);
    COLUMN_MAPPING.put("options", 7);
    COLUMN_MAPPING.put("cognitiveLevel", 8);
    COLUMN_MAPPING.put("tags", 9);
    COLUMN_MAPPING.put("isPublic", 10);
  }

  @Override
  public ExcelPreviewResponse previewExcelImport(MultipartFile file) {
    log.info("Previewing Excel import: {}", file.getOriginalFilename());

    validateExcelFile(file);

    List<ExcelPreviewResponse.PreviewRow> previewRows = new ArrayList<>();
    int validCount = 0;
    int invalidCount = 0;

    try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      int totalRows = sheet.getPhysicalNumberOfRows() - 1; // Exclude header

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || isRowEmpty(row)) {
          continue;
        }

        try {
          QuestionTemplateRequest request = parseRowToRequest(row, i);
          List<String> errors = validateRequest(request);

          boolean isValid = errors.isEmpty();
          if (isValid) {
            validCount++;
          } else {
            invalidCount++;
          }

          previewRows.add(
              ExcelPreviewResponse.PreviewRow.builder()
                  .rowNumber(i + 1)
                  .isValid(isValid)
                  .data(request)
                  .validationErrors(errors.isEmpty() ? null : errors)
                  .build());

        } catch (Exception e) {
          log.error("Error parsing row {}: {}", i + 1, e.getMessage());
          invalidCount++;
          previewRows.add(
              ExcelPreviewResponse.PreviewRow.builder()
                  .rowNumber(i + 1)
                  .isValid(false)
                  .data(null)
                  .validationErrors(List.of("Parse error: " + e.getMessage()))
                  .build());
        }
      }

      return ExcelPreviewResponse.builder()
          .totalRows(previewRows.size())
          .validRows(validCount)
          .invalidRows(invalidCount)
          .rows(previewRows)
          .build();

    } catch (Exception e) {
      log.error("Failed to preview Excel file: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.INVALID_KEY);
    }
  }

  @Override
  @Transactional
  public TemplateBatchImportResponse importTemplatesBatch(
      QuestionTemplateBatchImportRequest request) {
    log.info("Importing {} templates in batch", request.getTemplates().size());

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    List<QuestionTemplateResponse> successfulTemplates = new ArrayList<>();
    List<TemplateBatchImportResponse.ImportErrorDetail> errors = new ArrayList<>();

    int rowNumber = 1;
    for (QuestionTemplateRequest templateRequest : request.getTemplates()) {
      try {
        QuestionTemplate template = createTemplateEntity(templateRequest, currentUserId);
        template = questionTemplateRepository.save(template);
        successfulTemplates.add(mapToResponse(template));
        log.debug("Successfully imported template: {}", template.getName());

      } catch (Exception e) {
        log.error(
            "Failed to import template '{}': {}", templateRequest.getName(), e.getMessage());
        errors.add(
            TemplateBatchImportResponse.ImportErrorDetail.builder()
                .rowNumber(rowNumber)
                .rowName(templateRequest.getName())
                .field("general")
                .message(e.getMessage())
                .build());
      }
      rowNumber++;
    }

    return TemplateBatchImportResponse.builder()
        .totalRows(request.getTemplates().size())
        .successCount(successfulTemplates.size())
        .failedCount(errors.size())
        .successfulTemplates(successfulTemplates)
        .errors(errors.isEmpty() ? null : errors)
        .build();
  }

  private void validateExcelFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    // Max 10MB
    if (file.getSize() > 10 * 1024 * 1024) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }
  }

  private boolean isRowEmpty(Row row) {
    for (int i = 0; i < 11; i++) {
      Cell cell = row.getCell(i);
      if (cell != null && cell.getCellType() != CellType.BLANK) {
        return false;
      }
    }
    return true;
  }

  private QuestionTemplateRequest parseRowToRequest(Row row, int rowIndex) throws Exception {
    String name = getCellValueAsString(row, COLUMN_MAPPING.get("name"));
    String description = getCellValueAsString(row, COLUMN_MAPPING.get("description"));
    String templateTypeStr = getCellValueAsString(row, COLUMN_MAPPING.get("templateType"));
    String templateTextVi = getCellValueAsString(row, COLUMN_MAPPING.get("templateText_vi"));
    String parametersJson = getCellValueAsString(row, COLUMN_MAPPING.get("parameters"));
    String answerFormula = getCellValueAsString(row, COLUMN_MAPPING.get("answerFormula"));
    String cognitiveLevelStr = getCellValueAsString(row, COLUMN_MAPPING.get("cognitiveLevel"));
    String tagsStr = getCellValueAsString(row, COLUMN_MAPPING.get("tags"));
    String isPublicStr = getCellValueAsString(row, COLUMN_MAPPING.get("isPublic"));

    // Parse templateType
    QuestionType templateType = null;
    if (templateTypeStr != null && !templateTypeStr.isBlank()) {
      try {
        templateType = QuestionType.valueOf(templateTypeStr.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new Exception("Invalid templateType: " + templateTypeStr);
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

    // Parse templateText (kept for backward compat) and use as content
    String content = (templateTextVi != null && !templateTextVi.isBlank()) ? templateTextVi.trim() : null;
    Map<String, Object> templateText = new HashMap<>();
    if (content != null) {
      templateText.put("vi", content);
    }

    // Parse parameters JSON
    Map<String, Object> parameters = null;
    if (parametersJson != null && !parametersJson.isBlank()) {
      try {
        parameters = objectMapper.readValue(parametersJson, new TypeReference<>() {});
      } catch (Exception e) {
        throw new Exception("Invalid parameters JSON: " + e.getMessage());
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

    // Parse isPublic
    Boolean isPublic = false;
    if (isPublicStr != null && !isPublicStr.isBlank()) {
      isPublic =
          isPublicStr.trim().equalsIgnoreCase("true")
              || isPublicStr.trim().equalsIgnoreCase("1")
              || isPublicStr.trim().equalsIgnoreCase("yes");
    }

    return QuestionTemplateRequest.builder()
        .name(name)
        .description(description)
        .templateType(templateType)
        .content(content)
        .templateText(templateText)
        .parameters(parameters)
        .answerFormula(answerFormula)
        .cognitiveLevel(cognitiveLevel)
        .tags(tags)
        .isPublic(isPublic)
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

  private List<String> validateRequest(QuestionTemplateRequest request) {
    List<String> errors = new ArrayList<>();

    // Use Bean Validation
    Set<ConstraintViolation<QuestionTemplateRequest>> violations = validator.validate(request);
    for (ConstraintViolation<QuestionTemplateRequest> violation : violations) {
      errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
    }

    // Additional custom validations
    boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
    boolean hasTemplateText = request.getTemplateText() != null && !request.getTemplateText().isEmpty();
    if (!hasContent && !hasTemplateText) {
      errors.add("content/templateText: Template content is required");
    }

    return errors;
  }

  private QuestionTemplate createTemplateEntity(
      QuestionTemplateRequest request, UUID currentUserId) {
    QuestionTemplate template =
        QuestionTemplate.builder()
            .questionBankId(request.getQuestionBankId())
            .canonicalQuestionId(request.getCanonicalQuestionId())
            .name(request.getName())
            .description(request.getDescription())
            .templateType(request.getTemplateType() != null ? request.getTemplateType() : com.fptu.math_master.enums.QuestionType.MULTIPLE_CHOICE)
            .content(request.getContent())
            .templateText(request.getTemplateText())
            .parameters(request.getParameters() != null ? request.getParameters() : Collections.emptyMap())
            .answerFormula(request.getAnswerFormula())
            .solution(request.getSolution())
            .cognitiveLevel(request.getCognitiveLevel())
            .tags(request.getTags())
            .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
            .status(TemplateStatus.DRAFT)
            .usageCount(0)
            .build();
    template.setCreatedBy(currentUserId);
    return template;
  }

  private QuestionTemplateResponse mapToResponse(QuestionTemplate template) {
    String displayContent = template.getContent();
    if ((displayContent == null || displayContent.isBlank()) && template.getTemplateText() != null) {
      Object vi = template.getTemplateText().get("vi");
      if (vi instanceof String s) displayContent = s;
    }
    return QuestionTemplateResponse.builder()
        .id(template.getId())
        .name(template.getName())
        .description(template.getDescription())
        .templateType(template.getTemplateType())
        .content(displayContent)
        .templateText(template.getTemplateText())
        .parameters(template.getParameters())
        .answerFormula(template.getAnswerFormula())
        .solution(template.getSolution())
        .cognitiveLevel(template.getCognitiveLevel())
        .tags(template.getTags())
        .isPublic(template.getIsPublic())
        .status(template.getStatus())
        .usageCount(template.getUsageCount())
        .avgSuccessRate(template.getAvgSuccessRate())
        .createdBy(template.getCreatedBy())
        .createdAt(template.getCreatedAt())
        .updatedAt(template.getUpdatedAt())
        .build();
  }

  @Override
  public byte[] generateExcelTemplate() {
    log.info("Generating Excel template for download");

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Question Templates");

      // Create header row
      Row headerRow = sheet.createRow(0);
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
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
      }

      // Create example row 1
      Row example1 = sheet.createRow(1);
      example1.createCell(0).setCellValue("Giải phương trình bậc nhất");
      example1.createCell(1).setCellValue("Phương trình bậc nhất một ẩn cơ bản");
      example1.createCell(2).setCellValue("MULTIPLE_CHOICE");
      example1.createCell(3).setCellValue("Giải phương trình: {{a}}x + {{b}} = 0");
      example1
          .createCell(4)
          .setCellValue(
              "{\"a\":{\"type\":\"int\",\"min\":1,\"max\":10},\"b\":{\"type\":\"int\",\"min\":-10,\"max\":10}}");
      example1.createCell(5).setCellValue("(-b)/a");
      example1.createCell(6).setCellValue("");
      example1
          .createCell(7)
          .setCellValue("{\"A\":\"(-b)/a\",\"B\":\"b/a\",\"C\":\"-a/b\",\"D\":\"a+b\"}");
      example1.createCell(8).setCellValue("THONG_HIEU");
      example1.createCell(9).setCellValue("đại số, phương trình, lớp 9");
      example1.createCell(10).setCellValue("false");

      // Create example row 2 - with TikZ diagram
      Row example2 = sheet.createRow(2);
      example2.createCell(0).setCellValue("Đồ thị hàm số bậc ba");
      example2.createCell(1).setCellValue("Vẽ và phân tích đồ thị hàm số y = ax³ - 3ax");
      example2.createCell(2).setCellValue("MULTIPLE_CHOICE");
      example2
          .createCell(3)
          .setCellValue("Cho hàm số y = {{a}}x³ - 3{{a}}x. Tìm giá trị cực đại của hàm số.");
      example2
          .createCell(4)
          .setCellValue("{\"a\":{\"type\":\"int\",\"min\":1,\"max\":5}}");
      example2.createCell(5).setCellValue("2*a");
      example2
          .createCell(6)
          .setCellValue(
              "\\begin{tikzpicture}\\begin{axis}[axis lines = middle,xmin=-3, xmax=3,ymin=-10, ymax=10,samples=100]\\addplot[blue, thick]{{{a}}*x^3 - 3*{{a}}*x};\\addplot[only marks, red] coordinates {(-1, {{2*a}})(1, {{-2*a}})(0, 0)};\\end{axis}\\end{tikzpicture}");
      example2
          .createCell(7)
          .setCellValue("{\"A\":\"2*a\",\"B\":\"-2*a\",\"C\":\"a\",\"D\":\"3*a\"}");
      example2.createCell(8).setCellValue("VAN_DUNG_CAO");
      example2.createCell(9).setCellValue("giải tích, đồ thị, lớp 12");
      example2.createCell(10).setCellValue("true");

      // Auto-size columns
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();

    } catch (Exception e) {
      log.error("Failed to generate Excel template: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.INVALID_KEY);
    }
  }

  /**
   * Column format (0-indexed):
   *   0: title    – required
   *   1: content  – required, may contain {{param}} placeholders
   *   2: answer   – required, answer formula
   *   3: level    – required, one of NHAN_BIET / THONG_HIEU / VAN_DUNG / VAN_DUNG_CAO
   *   4: param_json – optional, JSON object e.g. {"a":{"type":"int","min":1,"max":10}}
   */
  @Override
  @Transactional
  public TemplateBatchImportResponse importFromExcel(MultipartFile file) {
    log.info("Simple Excel import: {}", file.getOriginalFilename());

    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    List<QuestionTemplate> toSave = new ArrayList<>();
    List<TemplateBatchImportResponse.ImportErrorDetail> errors = new ArrayList<>();
    int totalRows = 0;

    try (InputStream is = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(is)) {

      Sheet sheet = workbook.getSheetAt(0);

      for (Row row : sheet) {
        if (row.getRowNum() == HEADER_ROW) continue; // skip header

        // Skip blank rows
        boolean allBlank = true;
        for (int c = 0; c <= 4; c++) {
          Cell cell = row.getCell(c);
          if (cell != null && cell.getCellType() != CellType.BLANK) {
            allBlank = false;
            break;
          }
        }
        if (allBlank) continue;

        totalRows++;
        int rowNum = row.getRowNum() + 1; // 1-based for error reporting

        String title   = getCellStringValue(row, 0);
        String content = getCellStringValue(row, 1);
        String answer  = getCellStringValue(row, 2);
        String levelStr = getCellStringValue(row, 3);
        String paramJson = getCellStringValue(row, 4);

        // Validate required fields
        if (title == null || title.isBlank()) {
          errors.add(TemplateBatchImportResponse.ImportErrorDetail.builder()
              .rowNumber(rowNum).field("title").message("Tiêu đề (cột 1) không được để trống").build());
          continue;
        }
        if (content == null || content.isBlank()) {
          errors.add(TemplateBatchImportResponse.ImportErrorDetail.builder()
              .rowNumber(rowNum).rowName(title).field("content").message("Nội dung (cột 2) không được để trống").build());
          continue;
        }
        if (answer == null || answer.isBlank()) {
          errors.add(TemplateBatchImportResponse.ImportErrorDetail.builder()
              .rowNumber(rowNum).rowName(title).field("answer").message("Đáp án (cột 3) không được để trống").build());
          continue;
        }

        // Parse cognitive level
        CognitiveLevel cognitiveLevel;
        try {
          cognitiveLevel = CognitiveLevel.valueOf(levelStr == null ? "" : levelStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
          errors.add(TemplateBatchImportResponse.ImportErrorDetail.builder()
              .rowNumber(rowNum).rowName(title).field("level")
              .message("Mức độ '" + levelStr + "' không hợp lệ. Dùng: NHAN_BIET / THONG_HIEU / VAN_DUNG / VAN_DUNG_CAO").build());
          continue;
        }

        // Parse parameters JSON
        Map<String, Object> parameters = new HashMap<>();
        if (paramJson != null && !paramJson.isBlank()) {
          try {
            parameters = objectMapper.readValue(paramJson, new TypeReference<Map<String, Object>>() {});
          } catch (Exception e) {
            errors.add(TemplateBatchImportResponse.ImportErrorDetail.builder()
                .rowNumber(rowNum).rowName(title).field("param_json")
                .message("JSON tham số không hợp lệ: " + e.getMessage()).build());
            continue;
          }
        }

        // Build templateText for backward compat
        Map<String, Object> templateText = new HashMap<>();
        templateText.put("vi", content);

        QuestionTemplate template = QuestionTemplate.builder()
            .name(title)
            .templateType(QuestionType.MULTIPLE_CHOICE)
            .templateText(templateText)
            .content(content)
            .parameters(parameters)
            .answerFormula(answer)
            .cognitiveLevel(cognitiveLevel)
            .tags(new String[]{"excel-import"})
            .isPublic(false)
            .status(TemplateStatus.DRAFT)
            .usageCount(0)
            .build();
        template.setCreatedBy(currentUserId);

        toSave.add(template);
      }

    } catch (Exception e) {
      log.error("Failed to parse Excel file: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    List<QuestionTemplate> saved = questionTemplateRepository.saveAll(toSave);
    List<QuestionTemplateResponse> successfulTemplates = saved.stream()
        .map(this::mapTemplateToResponse)
        .collect(java.util.stream.Collectors.toList());

    log.info("Excel import complete: {} saved, {} errors (total rows: {})",
        saved.size(), errors.size(), totalRows);

    return TemplateBatchImportResponse.builder()
        .totalRows(totalRows)
        .successCount(saved.size())
        .failedCount(errors.size())
        .successfulTemplates(successfulTemplates)
        .errors(errors)
        .build();
  }

  private QuestionTemplateResponse mapTemplateToResponse(QuestionTemplate template) {
    return QuestionTemplateResponse.builder()
        .id(template.getId())
        .createdBy(template.getCreatedBy())
        .name(template.getName())
        .content(template.getContent())
        .templateType(template.getTemplateType())
        .parameters(template.getParameters())
        .answerFormula(template.getAnswerFormula())
        .cognitiveLevel(template.getCognitiveLevel())
        .tags(template.getTags())
        .isPublic(template.getIsPublic())
        .status(template.getStatus())
        .usageCount(template.getUsageCount())
        .createdAt(template.getCreatedAt())
        .build();
  }

  private String getCellStringValue(Row row, int colIndex) {
    Cell cell = row.getCell(colIndex);
    if (cell == null) return null;
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue().trim();
      case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> null;
    };
  }
}

