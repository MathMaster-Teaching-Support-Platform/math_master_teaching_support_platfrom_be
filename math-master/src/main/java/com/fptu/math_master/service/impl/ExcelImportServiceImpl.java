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
    String diagramTemplate = getCellValueAsString(row, COLUMN_MAPPING.get("diagramTemplate"));
    String optionsJson = getCellValueAsString(row, COLUMN_MAPPING.get("options"));
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

    // Parse templateText
    Map<String, Object> templateText = new HashMap<>();
    if (templateTextVi != null && !templateTextVi.isBlank()) {
      templateText.put("vi", templateTextVi);
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

    // Parse options JSON
    Map<String, Object> optionsGenerator = null;
    if (optionsJson != null && !optionsJson.isBlank()) {
      try {
        optionsGenerator = objectMapper.readValue(optionsJson, new TypeReference<>() {});
      } catch (Exception e) {
        throw new Exception("Invalid options JSON: " + e.getMessage());
      }
    }

    // Parse tags
    java.util.List<com.fptu.math_master.enums.QuestionTag> tags = new java.util.ArrayList<>();
    if (tagsStr != null && !tagsStr.isBlank()) {
      String[] tagArray = tagsStr.split(",");
      for (String tagStr : tagArray) {
        String trimmed = tagStr.trim();
        if (!trimmed.isEmpty()) {
          try {
            // Try to parse as enum name
            tags.add(com.fptu.math_master.enums.QuestionTag.valueOf(trimmed.toUpperCase()));
          } catch (IllegalArgumentException e) {
            // Try Vietnamese name lookup
            com.fptu.math_master.enums.QuestionTag tag = 
                com.fptu.math_master.enums.QuestionTag.fromVietnameseName(trimmed);
            if (tag != null) {
              tags.add(tag);
            }
          }
        }
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
        .templateText(templateText)
        .parameters(parameters)
        .answerFormula(answerFormula)
        .diagramTemplate(diagramTemplate)
        .optionsGenerator(optionsGenerator)
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
    if (request.getTemplateText() == null || request.getTemplateText().isEmpty()) {
      errors.add("templateText: Template text is required");
    }

    if (request.getParameters() == null || request.getParameters().isEmpty()) {
      errors.add("parameters: Parameters are required");
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
            .templateType(request.getTemplateType())
            .templateText(request.getTemplateText())
            .parameters(request.getParameters())
            .answerFormula(request.getAnswerFormula())
            .diagramTemplate(request.getDiagramTemplate())
            .optionsGenerator(request.getOptionsGenerator())
            .constraints(request.getConstraints())
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
    return QuestionTemplateResponse.builder()
        .id(template.getId())
        .name(template.getName())
        .description(template.getDescription())
        .templateType(template.getTemplateType())
        .templateText(template.getTemplateText())
        .parameters(template.getParameters())
        .answerFormula(template.getAnswerFormula())
        .diagramTemplate(template.getDiagramTemplate())
        .optionsGenerator(template.getOptionsGenerator())
        .constraints(template.getConstraints())
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
}
