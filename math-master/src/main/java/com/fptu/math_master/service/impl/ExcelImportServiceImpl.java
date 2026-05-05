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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel import for QuestionTemplate. The downloadable template ships three
 * fully-structured examples — geometry MCQ (pyramid), TRUE_FALSE (variation
 * table), and SHORT_ANSWER (function graph integral) — so teachers can copy
 * the layout and adapt it.
 *
 * <p>Each row is one template. {@code {{param}}} placeholders inside
 * {@code templateText_vi}, {@code answerFormula}, {@code diagramTemplate},
 * {@code solutionStepsTemplate}, {@code optionsGenerator}, and
 * {@code statementMutations} are filled in at generation time by the AI flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExcelImportServiceImpl implements ExcelImportService {

  QuestionTemplateRepository questionTemplateRepository;
  Validator validator;
  ObjectMapper objectMapper;

  // ---- Column layout ---------------------------------------------------------
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

  private static final int HEADER_ROW = 0;
  private static final int NOTES_ROW = 1;
  private static final int FIRST_DATA_ROW = 2;

  // ---- Public API ------------------------------------------------------------

  @Override
  public ExcelPreviewResponse previewExcelImport(MultipartFile file) {
    log.info("Previewing Excel import: {}", file.getOriginalFilename());

    validateExcelFile(file);

    List<ExcelPreviewResponse.PreviewRow> previewRows = new ArrayList<>();
    int validCount = 0;
    int invalidCount = 0;

    try (InputStream is = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(is)) {

      Sheet sheet = workbook.getSheetAt(0);

      for (int i = FIRST_DATA_ROW; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || isRowEmpty(row)) {
          continue;
        }

        try {
          QuestionTemplateRequest request = parseRowToRequest(row);
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
        log.error("Failed to import template '{}': {}", templateRequest.getName(), e.getMessage());
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

  // ---- Validation ------------------------------------------------------------

  private void validateExcelFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    if (file.getSize() > 10 * 1024 * 1024) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }
  }

  private boolean isRowEmpty(Row row) {
    short first = row.getFirstCellNum();
    short last = row.getLastCellNum();
    if (first < 0) return true;
    int upper = Math.max(last, HEADERS.length);
    for (int c = 0; c < upper; c++) {
      String value = getCellValueAsString(row, c);
      if (value != null && !value.isBlank()) {
        return false;
      }
    }
    return true;
  }

  private QuestionTemplateRequest parseRowToRequest(Row row) throws Exception {
    String name = getCellValueAsString(row, COL_NAME);
    String description = getCellValueAsString(row, COL_DESCRIPTION);
    String templateTypeStr = getCellValueAsString(row, COL_TEMPLATE_TYPE);
    String templateTextVi = getCellValueAsString(row, COL_TEMPLATE_TEXT_VI);
    String parametersJson = getCellValueAsString(row, COL_PARAMETERS);
    String answerFormula = getCellValueAsString(row, COL_ANSWER_FORMULA);
    String diagramTemplate = getCellValueAsString(row, COL_DIAGRAM_TEMPLATE);
    String solutionStepsTemplate = getCellValueAsString(row, COL_SOLUTION_STEPS_TEMPLATE);
    String optionsJson = getCellValueAsString(row, COL_OPTIONS_GENERATOR);
    String statementMutationsJson = getCellValueAsString(row, COL_STATEMENT_MUTATIONS);
    String cognitiveLevelStr = getCellValueAsString(row, COL_COGNITIVE_LEVEL);
    String tagsStr = getCellValueAsString(row, COL_TAGS);
    String isPublicStr = getCellValueAsString(row, COL_IS_PUBLIC);

    QuestionType templateType = parseEnum(QuestionType.class, templateTypeStr, "templateType");
    CognitiveLevel cognitiveLevel =
        parseEnum(CognitiveLevel.class, cognitiveLevelStr, "cognitiveLevel");

    Map<String, Object> templateText = new HashMap<>();
    if (templateTextVi != null && !templateTextVi.isBlank()) {
      templateText.put("vi", templateTextVi);
    }

    Map<String, Object> parameters = parseJsonMap(parametersJson, "parameters");
    Map<String, Object> optionsGenerator = parseJsonMap(optionsJson, "optionsGenerator");
    Map<String, Object> statementMutations =
        parseJsonMap(statementMutationsJson, "statementMutations");

    java.util.List<com.fptu.math_master.enums.QuestionTag> tags = new java.util.ArrayList<>();
    if (tagsStr != null && !tagsStr.isBlank()) {
      for (String raw : tagsStr.split(",")) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) continue;
        try {
          tags.add(com.fptu.math_master.enums.QuestionTag.valueOf(trimmed.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
          com.fptu.math_master.enums.QuestionTag tag =
              com.fptu.math_master.enums.QuestionTag.fromVietnameseName(trimmed);
          if (tag != null) tags.add(tag);
          // else: silently drop unknown free-form tags
        }
      }
    }

    Boolean isPublic = false;
    if (isPublicStr != null && !isPublicStr.isBlank()) {
      String v = isPublicStr.trim();
      isPublic = v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes");
    }

    return QuestionTemplateRequest.builder()
        .name(name)
        .description(description)
        .templateType(templateType)
        .templateText(templateText)
        .parameters(parameters)
        .answerFormula(emptyToNull(answerFormula))
        .diagramTemplate(emptyToNull(diagramTemplate))
        .solutionStepsTemplate(emptyToNull(solutionStepsTemplate))
        .optionsGenerator(optionsGenerator)
        .statementMutations(statementMutations)
        .cognitiveLevel(cognitiveLevel)
        .tags(tags)
        .isPublic(isPublic)
        .build();
  }

  private Map<String, Object> parseJsonMap(String json, String field) throws Exception {
    if (json == null || json.isBlank()) return null;
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      throw new Exception("Invalid " + field + " JSON: " + e.getMessage());
    }
  }

  private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field)
      throws Exception {
    if (value == null || value.isBlank()) return null;
    try {
      return Enum.valueOf(type, value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new Exception("Invalid " + field + ": " + value);
    }
  }

  private String emptyToNull(String s) {
    return (s == null || s.isBlank()) ? null : s;
  }

  /** Render a numeric cell without losing decimals. Whole numbers stay integer-formatted. */
  private String formatNumeric(double value) {
    if (Double.isInfinite(value) || Double.isNaN(value)) return null;
    if (value == Math.floor(value)) {
      return String.valueOf((long) value);
    }
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  private String getCellValueAsString(Row row, Integer columnIndex) {
    if (columnIndex == null) {
      return null;
    }

    Cell cell = row.getCell(columnIndex);
    if (cell == null) {
      return null;
    }
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      type = cell.getCachedFormulaResultType();
    }
    return switch (type) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> formatNumeric(cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> cell.getCellFormula();
      default -> null;
    };
  }

  private List<String> validateRequest(QuestionTemplateRequest request) {
    List<String> errors = new ArrayList<>();

    Set<ConstraintViolation<QuestionTemplateRequest>> violations = validator.validate(request);
    for (ConstraintViolation<QuestionTemplateRequest> violation : violations) {
      errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
    }

    if (request.getTemplateText() == null || request.getTemplateText().isEmpty()) {
      errors.add("templateText: Template text is required");
    }
    if (request.getParameters() == null || request.getParameters().isEmpty()) {
      errors.add("parameters: Parameters are required");
    }

    QuestionType type = request.getTemplateType();
    if (type == null) {
      // bean-validation already complained; nothing more to add
      return errors;
    }
    switch (type) {
      case MULTIPLE_CHOICE -> {
        if (request.getAnswerFormula() == null || request.getAnswerFormula().isBlank()) {
          errors.add("answerFormula: required for MULTIPLE_CHOICE");
        }
        if (request.getOptionsGenerator() == null || request.getOptionsGenerator().isEmpty()) {
          errors.add("optionsGenerator: required for MULTIPLE_CHOICE (JSON of A/B/C/D formulas)");
        }
      }
      case TRUE_FALSE -> {
        Map<String, Object> sm = request.getStatementMutations();
        if (sm == null || !sm.containsKey("clauseTemplates")) {
          errors.add(
              "statementMutations: required for TRUE_FALSE (JSON with clauseTemplates list)");
        } else if (sm.get("clauseTemplates") instanceof List<?> list && list.isEmpty()) {
          errors.add("statementMutations.clauseTemplates: must contain at least one clause");
        }
      }
      case SHORT_ANSWER -> {
        if (request.getAnswerFormula() == null || request.getAnswerFormula().isBlank()) {
          errors.add("answerFormula: required for SHORT_ANSWER");
        }
      }
      default ->
          errors.add(
              "templateType '"
                  + type
                  + "' is not supported by Excel import; use MULTIPLE_CHOICE, "
                  + "TRUE_FALSE, or SHORT_ANSWER");
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
            .solutionStepsTemplate(request.getSolutionStepsTemplate())
            .optionsGenerator(request.getOptionsGenerator())
            .statementMutations(request.getStatementMutations())
            .globalConstraints(request.getConstraints())
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

  // ---- Template generation ---------------------------------------------------

  @Override
  public byte[] generateExcelTemplate() {
    log.info("Generating Excel template for download");

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      writeTemplatesSheet(workbook);
      writeInstructionsSheet(workbook);

      workbook.write(out);
      return out.toByteArray();

    } catch (Exception e) {
      log.error("Failed to generate Excel template: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.INVALID_KEY);
    }
  }

  private void writeTemplatesSheet(Workbook workbook) {
    Sheet sheet = workbook.createSheet("Question Templates");

    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);

    CellStyle wrap = workbook.createCellStyle();
    wrap.setWrapText(true);
    wrap.setVerticalAlignment(VerticalAlignment.TOP);

    CellStyle noteStyle = workbook.createCellStyle();
    Font noteFont = workbook.createFont();
    noteFont.setItalic(true);
    noteFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
    noteStyle.setFont(noteFont);
    noteStyle.setWrapText(true);

    Row headerRow = sheet.createRow(HEADER_ROW);
    for (int i = 0; i < HEADERS.length; i++) {
      Cell c = headerRow.createCell(i);
      c.setCellValue(HEADERS[i]);
      c.setCellStyle(headerStyle);
    }

    Row notes = sheet.createRow(NOTES_ROW);
    String[] noteText = new String[HEADERS.length];
    noteText[COL_NAME] = "(Bắt buộc) Tên ngắn gọn của template";
    noteText[COL_DESCRIPTION] = "(Tuỳ chọn) Mô tả chi tiết, mục tiêu sử dụng";
    noteText[COL_TEMPLATE_TYPE] = "(Bắt buộc) MULTIPLE_CHOICE | TRUE_FALSE | SHORT_ANSWER";
    noteText[COL_TEMPLATE_TEXT_VI] =
        "(Bắt buộc) Đề bài tiếng Việt, dùng {{tên_tham_số}} để chèn tham số. Hỗ trợ LaTeX $...$";
    noteText[COL_PARAMETERS] =
        "(Bắt buộc) JSON khai báo tham số, vd " + "{\"a\":{\"type\":\"int\",\"min\":1,\"max\":5}}";
    noteText[COL_ANSWER_FORMULA] =
        "MCQ + SA: công thức tính đáp án, dùng tham số gốc (vd \"(-b)/a\"). TF: bỏ trống.";
    noteText[COL_DIAGRAM_TEMPLATE] =
        "(Tuỳ chọn) LaTeX/TikZ với {{tham_số}}, hỗ trợ \\begin{tikzpicture}...\\end{tikzpicture}";
    noteText[COL_SOLUTION_STEPS_TEMPLATE] =
        "(Tuỳ chọn) Các bước giải mẫu, hỗ trợ LaTeX và {{tham_số}}";
    noteText[COL_OPTIONS_GENERATOR] =
        "MCQ: JSON {\"A\":\"công thức\",\"B\":\"công thức\",\"C\":\"công thức\",\"D\":\"công thức\"}."
            + " Mỗi công thức dùng các tham số gốc (vd \"sqrt(3)/4*a^2\") và sẽ được tính ra số."
            + " 1 trong 4 phải khớp answerFormula. TF/SA: bỏ trống.";
    noteText[COL_STATEMENT_MUTATIONS] =
        "TF: JSON {\"clauseTemplates\":[{\"text\":\"...\",\"truthValue\":true},...]}. MCQ/SA: bỏ trống.";
    noteText[COL_COGNITIVE_LEVEL] = "(Bắt buộc) NHAN_BIET | THONG_HIEU | VAN_DUNG | VAN_DUNG_CAO";
    noteText[COL_TAGS] =
        "(Bắt buộc) Tag tiếng Việt phân tách bởi dấu phẩy, vd \"Hình học không gian, Tích phân\"";
    noteText[COL_IS_PUBLIC] = "(Tuỳ chọn) TRUE | FALSE - mặc định FALSE";
    for (int i = 0; i < noteText.length; i++) {
      Cell c = notes.createCell(i);
      c.setCellValue(noteText[i] == null ? "" : noteText[i]);
      c.setCellStyle(noteStyle);
    }

    int r = FIRST_DATA_ROW;
    writeExampleGeometryMcq(sheet, r++, wrap);
    writeExampleVariationTableTf(sheet, r++, wrap);
    writeExampleFunctionGraphSa(sheet, r++, wrap);

    int[] widths = new int[HEADERS.length];
    widths[COL_NAME] = 36;
    widths[COL_DESCRIPTION] = 50;
    widths[COL_TEMPLATE_TYPE] = 16;
    widths[COL_TEMPLATE_TEXT_VI] = 60;
    widths[COL_PARAMETERS] = 50;
    widths[COL_ANSWER_FORMULA] = 50;
    widths[COL_DIAGRAM_TEMPLATE] = 70;
    widths[COL_SOLUTION_STEPS_TEMPLATE] = 60;
    widths[COL_OPTIONS_GENERATOR] = 70;
    widths[COL_STATEMENT_MUTATIONS] = 70;
    widths[COL_COGNITIVE_LEVEL] = 16;
    widths[COL_TAGS] = 38;
    widths[COL_IS_PUBLIC] = 10;
    for (int i = 0; i < widths.length; i++) {
      sheet.setColumnWidth(i, widths[i] * 256);
    }
    sheet.createFreezePane(0, FIRST_DATA_ROW);
  }

  private void writeExampleGeometryMcq(Sheet sheet, int rowIdx, CellStyle wrap) {
    Row row = sheet.createRow(rowIdx);
    set(row, COL_NAME, "Diện tích toàn phần khối chóp tam giác đều");
    set(
        row,
        COL_DESCRIPTION,
        "MCQ hình học không gian: tính diện tích toàn phần khối chóp tam giác đều S.ABC "
            + "cạnh đáy {{a}}, chiều cao {{h}}",
        wrap);
    set(row, COL_TEMPLATE_TYPE, "MULTIPLE_CHOICE");
    set(
        row,
        COL_TEMPLATE_TEXT_VI,
        "Cho khối chóp tam giác đều $S.ABC$ có cạnh đáy bằng ${{a}}$ cm và chiều cao "
            + "$SO = {{h}}$ cm. Diện tích toàn phần của khối chóp bằng?",
        wrap);
    set(
        row,
        COL_PARAMETERS,
        "{\"a\":{\"type\":\"int\",\"min\":2,\"max\":8},"
            + "\"h\":{\"type\":\"int\",\"min\":3,\"max\":10}}",
        wrap);
    set(row, COL_ANSWER_FORMULA, "sqrt(3)/4*a^2 + 3/2*a*sqrt(h^2 + a^2/12)", wrap);
    set(
        row,
        COL_DIAGRAM_TEMPLATE,
        "\\begin{tikzpicture}[scale=0.85,\n"
            + "  x={(0.9cm,-0.35cm)}, y={(0.9cm,0.35cm)}, z={(0cm,1cm)}]\n"
            + "\\coordinate (A) at (0,0,0);\n"
            + "\\coordinate (B) at ({{a}},0,0);\n"
            + "\\coordinate (C) at ({{a}}/2,{{a}}*0.866,0);\n"
            + "\\coordinate (O) at ({{a}}/2,{{a}}*0.289,0);\n"
            + "\\coordinate (S) at ({{a}}/2,{{a}}*0.289,{{h}});\n"
            + "\\draw[thick] (A)--(B)--(C)--cycle;\n"
            + "\\draw[thick] (A)--(S);\n"
            + "\\draw[thick] (B)--(S);\n"
            + "\\draw[thick] (C)--(S);\n"
            + "\\draw[dashed] (S)--(O);\n"
            + "\\node[below left] at (A) {$A$};\n"
            + "\\node[below right] at (B) {$B$};\n"
            + "\\node[above] at (C) {$C$};\n"
            + "\\node[above right] at (S) {$S$};\n"
            + "\\node[below] at (O) {$O$};\n"
            + "\\draw[<->,gray] ($(S)+(0.4,0,0)$)--($(O)+(0.4,0,0)$) node[midway,right]"
            + "{${{h}}$};\n"
            + "\\end{tikzpicture}",
        wrap);
    set(
        row,
        COL_SOLUTION_STEPS_TEMPLATE,
        "Bước 1: Diện tích đáy tam giác đều cạnh ${{a}}$ là $S_{\\text{đáy}} = "
            + "\\dfrac{\\sqrt{3}}{4}{{a}}^2$.\n"
            + "Bước 2: Trung đoạn $SM = \\sqrt{SO^2 + OM^2} = "
            + "\\sqrt{{{h}}^2 + \\dfrac{{{a}}^2}{12}}$.\n"
            + "Bước 3: Diện tích xung quanh $S_{xq} = \\dfrac{1}{2} \\cdot 3{{a}} \\cdot SM = "
            + "\\dfrac{3}{2}{{a}}\\sqrt{{{h}}^2 + \\dfrac{{{a}}^2}{12}}$.\n"
            + "Bước 4: $S_{tp} = S_{\\text{đáy}} + S_{xq}$.",
        wrap);
    // optionsGenerator holds 4 EVALUABLE formulas — each encodes a real
    // student error so distractors are pedagogically meaningful (not noise).
    // The BE pre-evaluates these to numbers before generating the question,
    // so all 4 options come out as consistent decimal values.
    //   A: full S_tp                     = √3/4·a² + 3/2·a·√(h² + a²/12)
    //   B: forgot base area               =          3/2·a·√(h² + a²/12)
    //   C: used circumradius a/√3         = √3/4·a² + 3/2·a·√(h² + a²/3)
    //                                       (very close to A — tempting)
    //   D: used h directly as slant       = √3/4·a² + 3/2·a·h
    set(
        row,
        COL_OPTIONS_GENERATOR,
        "{\"A\":\"sqrt(3)/4*a^2 + 3/2*a*sqrt(h^2 + a^2/12)\","
            + "\"B\":\"3/2*a*sqrt(h^2 + a^2/12)\","
            + "\"C\":\"sqrt(3)/4*a^2 + 3/2*a*sqrt(h^2 + a^2/3)\","
            + "\"D\":\"sqrt(3)/4*a^2 + 3/2*a*h\"}",
        wrap);
    // statementMutations intentionally empty for MCQ
    set(row, COL_STATEMENT_MUTATIONS, "");
    set(row, COL_COGNITIVE_LEVEL, "THONG_HIEU");
    set(row, COL_TAGS, "Hình học không gian");
    set(row, COL_IS_PUBLIC, "TRUE");
  }

  private void writeExampleVariationTableTf(Sheet sheet, int rowIdx, CellStyle wrap) {
    Row row = sheet.createRow(rowIdx);
    set(row, COL_NAME, "Đúng/Sai từ bảng biến thiên hàm bậc ba");
    set(
        row,
        COL_DESCRIPTION,
        "TRUE_FALSE: Đọc bảng biến thiên hàm bậc 3 $y = ax^3 + bx^2 + cx + d$, xác định 4 mệnh đề "
            + "đúng/sai về cực trị, đồng biến, nghịch biến.",
        wrap);
    set(row, COL_TEMPLATE_TYPE, "TRUE_FALSE");
    set(
        row,
        COL_TEMPLATE_TEXT_VI,
        "Cho hàm số $y = f(x)$ có bảng biến thiên như sau. Trong các mệnh đề dưới đây, mệnh đề "
            + "nào đúng, mệnh đề nào sai?",
        wrap);
    set(
        row,
        COL_PARAMETERS,
        "{\"x1\":{\"type\":\"int\",\"min\":-4,\"max\":-1},"
            + "\"x2\":{\"type\":\"int\",\"min\":1,\"max\":4},"
            + "\"y1\":{\"type\":\"int\",\"min\":1,\"max\":10},"
            + "\"y2\":{\"type\":\"int\",\"min\":-10,\"max\":-1}}",
        wrap);
    // TF has no answerFormula
    set(row, COL_ANSWER_FORMULA, "");
    set(
        row,
        COL_DIAGRAM_TEMPLATE,
        "\\begin{tikzpicture}\n"
            + "\\tkzTabInit[lgt=2, espcl=2.5]{$x$/1, $f'(x)$/1, $f(x)$/2}"
            + "{$-\\infty$, ${{x1}}$, ${{x2}}$, $+\\infty$}\n"
            + "\\tkzTabLine{, +, 0, -, 0, +}\n"
            + "\\tkzTabVar{-/$-\\infty$, +/${{y1}}$, -/${{y2}}$, +/$+\\infty$}\n"
            + "\\end{tikzpicture}",
        wrap);
    set(
        row,
        COL_SOLUTION_STEPS_TEMPLATE,
        "Bước 1: Đọc dấu của $f'(x)$ trên từng khoảng — đổi từ $+$ sang $-$ tại ${{x1}}$ ⇒ cực "
            + "đại $f({{x1}}) = {{y1}}$.\n"
            + "Bước 2: $f'$ đổi từ $-$ sang $+$ tại ${{x2}}$ ⇒ cực tiểu $f({{x2}}) = {{y2}}$.\n"
            + "Bước 3: Hàm có $2$ cực trị; nghịch biến trên $({{x1}};{{x2}})$, "
            + "không đồng biến trên toàn $\\mathbb{R}$.",
        wrap);
    // MCQ-only fields stay blank
    set(row, COL_OPTIONS_GENERATOR, "");
    set(
        row,
        COL_STATEMENT_MUTATIONS,
        "{\"clauseTemplates\":["
            + "{\"text\":\"Hàm số đạt cực đại bằng ${{y1}}$\",\"truthValue\":true,"
            + "\"cognitiveLevel\":\"THONG_HIEU\"},"
            + "{\"text\":\"Hàm số đồng biến trên khoảng $(-\\\\infty; +\\\\infty)$\","
            + "\"truthValue\":false,\"cognitiveLevel\":\"THONG_HIEU\"},"
            + "{\"text\":\"Hàm số có đúng hai điểm cực trị\",\"truthValue\":true,"
            + "\"cognitiveLevel\":\"NHAN_BIET\"},"
            + "{\"text\":\"Hàm số đạt cực tiểu bằng ${{y1}}$\",\"truthValue\":false,"
            + "\"cognitiveLevel\":\"THONG_HIEU\"}"
            + "]}",
        wrap);
    set(row, COL_COGNITIVE_LEVEL, "THONG_HIEU");
    set(row, COL_TAGS, "Hàm số, Đạo hàm");
    set(row, COL_IS_PUBLIC, "TRUE");
  }

  private void writeExampleFunctionGraphSa(Sheet sheet, int rowIdx, CellStyle wrap) {
    Row row = sheet.createRow(rowIdx);
    set(row, COL_NAME, "Tính tích phân xác định bằng đổi biến");
    set(
        row,
        COL_DESCRIPTION,
        "SHORT_ANSWER: Tính tích phân có dạng $\\int (ax+b)^n\\,dx$ trên đoạn $[0,c]$, đáp án là "
            + "phân số tối giản hoặc số nguyên.",
        wrap);
    set(row, COL_TEMPLATE_TYPE, "SHORT_ANSWER");
    set(
        row,
        COL_TEMPLATE_TEXT_VI,
        "Tính tích phân $I = \\displaystyle\\int_0^{{{c}}} ({{a}}x + {{b}})^3\\,dx$. "
            + "Điền kết quả dưới dạng phân số tối giản hoặc số nguyên.",
        wrap);
    set(
        row,
        COL_PARAMETERS,
        "{\"a\":{\"type\":\"int\",\"min\":1,\"max\":3},"
            + "\"b\":{\"type\":\"int\",\"min\":1,\"max\":5},"
            + "\"c\":{\"type\":\"int\",\"min\":1,\"max\":3}}",
        wrap);
    set(row, COL_ANSWER_FORMULA, "((a*c+b)^4 - b^4) / (4*a)");
    set(
        row,
        COL_DIAGRAM_TEMPLATE,
        "\\begin{tikzpicture}\n"
            + "\\begin{axis}[\n"
            + "    axis lines = middle,\n"
            + "    xmin=-0.5, xmax={{c}}+0.5,\n"
            + "    ymin=-1, ymax=({{a}}*{{c}}+{{b}})^3+2,\n"
            + "    samples=100,\n"
            + "    xlabel={$x$}, ylabel={$y$}\n"
            + "]\n"
            + "\\addplot[blue, thick, domain=0:{{c}}]{({{a}}*x+{{b}})^3};\n"
            + "\\addplot[fill=blue!15, opacity=0.6, domain=0:{{c}}]{({{a}}*x+{{b}})^3} "
            + "\\closedcycle;\n"
            + "\\addplot[only marks, mark=*, mark size=2pt] coordinates "
            + "{(0,{{b}}^3) ({{c}},({{a}}*{{c}}+{{b}})^3)};\n"
            + "\\end{axis}\n"
            + "\\end{tikzpicture}",
        wrap);
    set(
        row,
        COL_SOLUTION_STEPS_TEMPLATE,
        "Bước 1: Đặt $u = {{a}}x + {{b}}$ ⇒ $du = {{a}}\\,dx$, đổi cận $x=0 \\Rightarrow "
            + "u={{b}}$, $x={{c}} \\Rightarrow u={{a}} \\cdot {{c}} + {{b}}$.\n"
            + "Bước 2: $I = \\dfrac{1}{{{a}}}\\displaystyle\\int_{{{b}}}^{{{a}} \\cdot {{c}} + "
            + "{{b}}} u^3\\,du = \\dfrac{1}{4{{a}}}\\big[u^4\\big]_{{{b}}}^{{{a}} \\cdot {{c}} + "
            + "{{b}}}$.\n"
            + "Bước 3: $I = \\dfrac{({{a}} \\cdot {{c}} + {{b}})^4 - {{b}}^4}{4{{a}}}$.",
        wrap);
    // No options for SA, no clauses
    set(row, COL_OPTIONS_GENERATOR, "");
    set(row, COL_STATEMENT_MUTATIONS, "");
    set(row, COL_COGNITIVE_LEVEL, "THONG_HIEU");
    set(row, COL_TAGS, "Tích phân, Đạo hàm");
    set(row, COL_IS_PUBLIC, "TRUE");
  }

  private void set(Row row, int col, String value) {
    Cell c = row.createCell(col);
    if (value != null) c.setCellValue(value);
  }

  private void set(Row row, int col, String value, CellStyle style) {
    Cell c = row.createCell(col);
    if (value != null) c.setCellValue(value);
    if (style != null) c.setCellStyle(style);
  }

  private void writeInstructionsSheet(Workbook workbook) {
    Sheet sheet = workbook.createSheet("Hướng dẫn");
    int r = 0;
    sheet.createRow(r++).createCell(0).setCellValue("HƯỚNG DẪN IMPORT TEMPLATE CÂU HỎI");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("LOẠI TEMPLATE HỖ TRỢ:");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("• MULTIPLE_CHOICE — cần answerFormula + optionsGenerator (JSON A/B/C/D)");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "• TRUE_FALSE — cần statementMutations.clauseTemplates (mỗi clause có text + truthValue)");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("• SHORT_ANSWER — cần answerFormula, không cần options/statements");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("THAM SỐ {{}}:");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "• Khai báo trong cột parameters: {\"a\":{\"type\":\"int\",\"min\":1,\"max\":5}}");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "• Sử dụng {{a}} trong templateText_vi, answerFormula, diagramTemplate, "
                + "optionsGenerator, statementMutations");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "• Tính sẵn biểu thức: dùng {{2*a}}, {{a/2}} - hệ thống sẽ tính khi sinh câu hỏi");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("LATEX:");
    sheet.createRow(r++).createCell(0).setCellValue("• Inline: $...$  ·  Block: $$...$$");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("• Trong cell Excel KHÔNG cần escape backslash — dán nguyên \\dfrac, \\sqrt");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "• Trong JSON (optionsGenerator, statementMutations) PHẢI escape \\\\ để JSON hợp lệ");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "• diagramTemplate giữ nguyên xuống dòng — dán cả khối \\begin{tikzpicture}...\\end{tikzpicture}");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("3 VÍ DỤ TRONG SHEET 'Question Templates':");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("Dòng 3: Geometry MCQ — diện tích khối chóp tam giác đều (TikZ 3D)");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "Dòng 4: TRUE_FALSE — bảng biến thiên hàm bậc ba (\\tkzTabInit), 4 mệnh đề "
                + "với truthValue");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "Dòng 5: SHORT_ANSWER — tích phân $\\int_0^c (ax+b)^3\\,dx$ với đồ thị PGFPlots "
                + "(vùng tô màu)");

    sheet.setColumnWidth(0, 130 * 256);
  }
}
