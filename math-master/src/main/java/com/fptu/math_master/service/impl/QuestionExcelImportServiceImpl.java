package com.fptu.math_master.service.impl;

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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel import for the question bank. The template is one row per question and
 * supports MULTIPLE_CHOICE (incl. geometry diagrams), TRUE_FALSE (statement-based
 * with per-statement truth values), and SHORT_ANSWER (with grading mode +
 * tolerance and optional graph LaTeX).
 *
 * <p>LaTeX content is read straight from STRING cells, so multi-line TikZ blocks
 * are preserved byte-for-byte.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionExcelImportServiceImpl implements QuestionExcelImportService {

  QuestionService questionService;
  Validator validator;
  ObjectMapper objectMapper;

  // ---- Column layout ---------------------------------------------------------
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

  private static final int HEADER_ROW = 0;
  private static final int NOTES_ROW = 1;
  private static final int FIRST_DATA_ROW = 2;

  private static final Set<String> TRUTH_TRUE = Set.of("TRUE", "T", "1", "ĐÚNG", "DUNG", "Đ", "D");
  private static final Set<String> TRUTH_FALSE = Set.of("FALSE", "F", "0", "SAI", "S");
  private static final Set<String> VALIDATION_MODES = Set.of("EXACT", "NUMERIC", "REGEX");

  // ---- Public API ------------------------------------------------------------

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

      for (int rowIdx = FIRST_DATA_ROW; rowIdx <= lastRow; rowIdx++) {
        Row row = sheet.getRow(rowIdx);
        if (row == null || isRowEmpty(row)) {
          continue;
        }

        try {
          CreateQuestionRequest request = parseRowToRequest(row);
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

      writeQuestionsSheet(workbook);
      writeInstructionsSheet(workbook);

      workbook.write(out);
      return out.toByteArray();

    } catch (Exception e) {
      log.error("Failed to generate question Excel template", e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  // ---- Template generation ---------------------------------------------------

  private void writeQuestionsSheet(Workbook workbook) {
    Sheet sheet = workbook.createSheet("Questions");

    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);

    CellStyle wrapStyle = workbook.createCellStyle();
    wrapStyle.setWrapText(true);
    wrapStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);

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
    noteText[COL_QUESTION_TYPE] = "(Bắt buộc) MULTIPLE_CHOICE | TRUE_FALSE | SHORT_ANSWER";
    noteText[COL_COGNITIVE_LEVEL] = "(Bắt buộc) NHAN_BIET | THONG_HIEU | VAN_DUNG | VAN_DUNG_CAO";
    noteText[COL_POINTS] = "(Tuỳ chọn) Số điểm, hỗ trợ thập phân (vd 0.25). Mặc định 1.0";
    noteText[COL_TAGS] = "(Tuỳ chọn) Các tag, phân tách bởi dấu phẩy";
    noteText[COL_QUESTION_TEXT] =
        "(Bắt buộc) Nội dung câu hỏi - hỗ trợ LaTeX: $...$ inline, $$...$$ block";
    noteText[COL_DIAGRAM_DATA] =
        "(Tuỳ chọn) LaTeX/TikZ cho hình vẽ, đồ thị, bảng biến thiên - giữ nguyên xuống dòng";
    noteText[COL_OPTION_A] = "MCQ: nội dung đáp án A | TF: phát biểu A";
    noteText[COL_OPTION_B] = "MCQ: nội dung đáp án B | TF: phát biểu B";
    noteText[COL_OPTION_C] = "MCQ: nội dung đáp án C | TF: phát biểu C";
    noteText[COL_OPTION_D] = "MCQ: nội dung đáp án D | TF: phát biểu D";
    noteText[COL_TRUTH_A] = "TF: TRUE/FALSE (hoặc Đ/S, 1/0) cho phát biểu A";
    noteText[COL_TRUTH_B] = "TF: TRUE/FALSE cho phát biểu B";
    noteText[COL_TRUTH_C] = "TF: TRUE/FALSE cho phát biểu C";
    noteText[COL_TRUTH_D] = "TF: TRUE/FALSE cho phát biểu D";
    noteText[COL_CORRECT_ANSWER] =
        "MCQ: A|B|C|D · TF: tự suy ra từ truthA-D (có thể bỏ trống) · SA: giá trị/biểu thức đáp án";
    noteText[COL_VALIDATION_MODE] = "SA: EXACT | NUMERIC | REGEX (mặc định EXACT)";
    noteText[COL_TOLERANCE] = "SA: sai số khi NUMERIC (vd 0.001)";
    noteText[COL_EXPLANATION] = "(Tuỳ chọn) Lời giải thích, hỗ trợ LaTeX";
    noteText[COL_SOLUTION_STEPS] = "(Tuỳ chọn) Các bước giải chi tiết, hỗ trợ LaTeX";
    for (int i = 0; i < noteText.length; i++) {
      Cell c = notes.createCell(i);
      c.setCellValue(noteText[i] == null ? "" : noteText[i]);
      c.setCellStyle(noteStyle);
    }

    // Example rows
    int r = FIRST_DATA_ROW;
    writeExampleStandardMcq(sheet, r++, wrapStyle);
    writeExampleGeometryMcq(sheet, r++, wrapStyle);
    writeExampleTrueFalse(sheet, r++, wrapStyle);
    writeExampleVariationTableTf(sheet, r++, wrapStyle);
    writeExampleShortAnswer(sheet, r++, wrapStyle);

    int[] widths = new int[HEADERS.length];
    widths[COL_QUESTION_TYPE] = 18;
    widths[COL_COGNITIVE_LEVEL] = 16;
    widths[COL_POINTS] = 8;
    widths[COL_TAGS] = 22;
    widths[COL_QUESTION_TEXT] = 50;
    widths[COL_DIAGRAM_DATA] = 60;
    widths[COL_OPTION_A] = 26;
    widths[COL_OPTION_B] = 26;
    widths[COL_OPTION_C] = 26;
    widths[COL_OPTION_D] = 26;
    widths[COL_TRUTH_A] = 8;
    widths[COL_TRUTH_B] = 8;
    widths[COL_TRUTH_C] = 8;
    widths[COL_TRUTH_D] = 8;
    widths[COL_CORRECT_ANSWER] = 22;
    widths[COL_VALIDATION_MODE] = 18;
    widths[COL_TOLERANCE] = 12;
    widths[COL_EXPLANATION] = 50;
    widths[COL_SOLUTION_STEPS] = 50;
    for (int i = 0; i < widths.length; i++) {
      sheet.setColumnWidth(i, widths[i] * 256);
    }
    sheet.createFreezePane(0, FIRST_DATA_ROW);
  }

  private void writeExampleStandardMcq(Sheet sheet, int rowIdx, CellStyle wrap) {
    Row row = sheet.createRow(rowIdx);
    set(row, COL_QUESTION_TYPE, "MULTIPLE_CHOICE");
    set(row, COL_COGNITIVE_LEVEL, "THONG_HIEU");
    set(row, COL_POINTS, "1.0");
    set(row, COL_TAGS, "đại số, phương trình bậc hai, lớp 10");
    set(row, COL_QUESTION_TEXT, "Tập nghiệm của phương trình $x^{2} - 5x + 6 = 0$ là:", wrap);
    // Standard MCQ — no diagram needed
    set(row, COL_DIAGRAM_DATA, "");
    set(row, COL_OPTION_A, "$\\{2;\\,3\\}$");
    set(row, COL_OPTION_B, "$\\{-2;\\,-3\\}$");
    set(row, COL_OPTION_C, "$\\{1;\\,6\\}$");
    set(row, COL_OPTION_D, "$\\{-1;\\,-6\\}$");
    // truth columns intentionally empty for MCQ
    set(row, COL_CORRECT_ANSWER, "A");
    // validation mode / tolerance unused for MCQ
    set(row, COL_VALIDATION_MODE, "");
    set(row, COL_TOLERANCE, "");
    set(
        row,
        COL_EXPLANATION,
        "Áp dụng định lý Viète: nếu $x_1, x_2$ là nghiệm thì $x_1 + x_2 = 5$ và $x_1 \\cdot x_2 = 6$. "
            + "Hai số thoả mãn là $2$ và $3$, do đó tập nghiệm là $\\{2;\\,3\\}$.",
        wrap);
    set(
        row,
        COL_SOLUTION_STEPS,
        "Bước 1: Tính $\\Delta = (-5)^{2} - 4 \\cdot 1 \\cdot 6 = 25 - 24 = 1 > 0$.\n"
            + "Bước 2: Áp dụng công thức nghiệm "
            + "$x_{1,2} = \\dfrac{5 \\pm \\sqrt{1}}{2}$.\n"
            + "Bước 3: $x_1 = 3$, $x_2 = 2$ ⇒ tập nghiệm $\\{2;\\,3\\}$.",
        wrap);
  }

  private void writeExampleGeometryMcq(Sheet sheet, int rowIdx, CellStyle wrap) {
    Row row = sheet.createRow(rowIdx);
    set(row, COL_QUESTION_TYPE, "MULTIPLE_CHOICE");
    set(row, COL_COGNITIVE_LEVEL, "VAN_DUNG");
    set(row, COL_POINTS, "1.0");
    set(row, COL_TAGS, "hình học không gian, hình nón, lớp 12");
    set(
        row,
        COL_QUESTION_TEXT,
        "Cho hình nón có đỉnh $O$, đáy là đường tròn tâm $O'$ bán kính $r = 2$ và chiều cao "
            + "$OO' = 4$. Gọi $H$ là điểm thuộc trục $OO'$ sao cho mặt phẳng qua $H$ vuông góc "
            + "với trục cắt nón theo một đường tròn bán kính $x = 1$. Tính độ dài đoạn $OH$.",
        wrap);
    set(
        row,
        COL_DIAGRAM_DATA,
        "\\begin{tikzpicture}[scale=0.8]\n"
            + "\\coordinate (O) at (0,4);\n"
            + "\\coordinate (A) at (-2,0);\n"
            + "\\coordinate (B) at (2,0);\n"
            + "\\coordinate (O1) at (0,0);\n"
            + "\\draw (O) -- (A);\n"
            + "\\draw (O) -- (B);\n"
            + "\\draw[dashed] (A) arc (180:360:2 and 0.6);\n"
            + "\\draw (B) arc (0:180:2 and 0.6);\n"
            + "\\coordinate (H) at (0,2);\n"
            + "\\draw[dashed] (-1,2) arc (180:360:1 and 0.3);\n"
            + "\\draw (-1,2) arc (180:0:1 and 0.3);\n"
            + "\\draw[dashed] (H) -- (-1,2);\n"
            + "\\draw[dashed] (H) -- (1,2);\n"
            + "\\draw[dashed] (O) -- (O1);\n"
            + "\\draw[dashed] (H) -- (0,0);\n"
            + "\\node at (0,4.3) {$O$};\n"
            + "\\node at (0,-0.3) {$O'$};\n"
            + "\\node at (0,2.2) {$H$};\n"
            + "\\node at (0.4,1) {$x$};\n"
            + "\\node at (2.5,2) {$h$};\n"
            + "\\end{tikzpicture}",
        wrap);
    set(row, COL_OPTION_A, "$OH = 1$");
    set(row, COL_OPTION_B, "$OH = 2$");
    set(row, COL_OPTION_C, "$OH = 3$");
    set(row, COL_OPTION_D, "$OH = 4$");
    // truth columns unused for MCQ
    set(row, COL_CORRECT_ANSWER, "B");
    set(row, COL_VALIDATION_MODE, "");
    set(row, COL_TOLERANCE, "");
    set(
        row,
        COL_EXPLANATION,
        "Hai tam giác $OHM$ (với $M$ trên đường tròn cắt) và $OO'A$ đồng dạng do cùng vuông tại "
            + "trục, suy ra $\\dfrac{OH}{OO'} = \\dfrac{x}{r}$. Thay số: "
            + "$OH = OO' \\cdot \\dfrac{x}{r} = 4 \\cdot \\dfrac{1}{2} = 2$.",
        wrap);
    set(
        row,
        COL_SOLUTION_STEPS,
        "Bước 1: Đặt $OH = d$. Mặt phẳng qua $H$ vuông góc trục cắt nón theo đường tròn tâm $H$, "
            + "bán kính $x$.\n"
            + "Bước 2: Theo tính chất hình nón: "
            + "$\\dfrac{x}{r} = \\dfrac{d}{OO'}$ ⇒ $d = OO' \\cdot \\dfrac{x}{r}$.\n"
            + "Bước 3: Thay $r=2$, $OO'=4$, $x=1$: $d = 4 \\cdot \\dfrac{1}{2} = 2$.\n"
            + "Bước 4: Kết luận $OH = 2$ ⇒ chọn B.",
        wrap);
  }

  private void writeExampleTrueFalse(Sheet sheet, int rowIdx, CellStyle wrap) {
    Row row = sheet.createRow(rowIdx);
    set(row, COL_QUESTION_TYPE, "TRUE_FALSE");
    set(row, COL_COGNITIVE_LEVEL, "THONG_HIEU");
    set(row, COL_POINTS, "1.0");
    set(row, COL_TAGS, "khảo sát hàm số, đồ thị, lớp 12");
    set(
        row,
        COL_QUESTION_TEXT,
        "Cho hàm số $f(x) = x^{3} - 3x$ có đồ thị như hình vẽ. Xét tính đúng/sai của các "
            + "phát biểu sau:",
        wrap);
    set(
        row,
        COL_DIAGRAM_DATA,
        "\\begin{tikzpicture}\n"
            + "\\begin{axis}[axis lines = middle, xmin=-3, xmax=3, ymin=-10, ymax=10, samples=100]\n"
            + "\\addplot[blue, thick]{x^3 - 3*x};\n"
            + "\\addplot[only marks, red] coordinates {(-1, 2) (1, -2) (0, 0)};\n"
            + "\\end{axis}\n"
            + "\\end{tikzpicture}",
        wrap);
    set(row, COL_OPTION_A, "Hàm số đạt cực đại tại $x = -1$ và giá trị cực đại bằng $2$.");
    set(row, COL_OPTION_B, "$f(0) = 0$.");
    set(row, COL_OPTION_C, "Đồ thị hàm số có tiệm cận ngang.");
    set(row, COL_OPTION_D, "Hàm số đồng biến trên $\\mathbb{R}$.");
    set(row, COL_TRUTH_A, "TRUE");
    set(row, COL_TRUTH_B, "TRUE");
    set(row, COL_TRUTH_C, "FALSE");
    set(row, COL_TRUTH_D, "FALSE");
    // correctAnswer auto-derived from truthA-D; supplying it is optional but illustrative
    set(row, COL_CORRECT_ANSWER, "A,B");
    // SA-only fields stay empty
    set(row, COL_VALIDATION_MODE, "");
    set(row, COL_TOLERANCE, "");
    set(
        row,
        COL_EXPLANATION,
        "A đúng: $f'(x) = 3(x^{2} - 1) = 0 \\Leftrightarrow x = \\pm 1$; tại $x=-1$, $f(-1)=2$ là "
            + "cực đại.\n"
            + "B đúng: $f(0) = 0^{3} - 3 \\cdot 0 = 0$.\n"
            + "C sai: đa thức bậc ba không có tiệm cận ngang.\n"
            + "D sai: hàm chỉ đồng biến trên $(-\\infty,-1)$ và $(1,+\\infty)$, nghịch biến "
            + "trên $(-1,1)$.",
        wrap);
    set(
        row,
        COL_SOLUTION_STEPS,
        "Bước 1: Tính $f'(x) = 3x^{2} - 3 = 3(x-1)(x+1)$.\n"
            + "Bước 2: $f'(x)=0 \\Leftrightarrow x = \\pm 1$. Lập bảng biến thiên.\n"
            + "Bước 3: $f(-1) = (-1)^{3} - 3(-1) = -1 + 3 = 2$ ⇒ cực đại tại $x=-1$, $f_{CD}=2$.\n"
            + "Bước 4: $f(0) = 0$. Bậc ba không có tiệm cận, không đồng biến toàn $\\mathbb{R}$.\n"
            + "Bước 5: Các phát biểu đúng là A và B.",
        wrap);
  }

  private void writeExampleVariationTableTf(Sheet sheet, int rowIdx, CellStyle wrap) {
    Row row = sheet.createRow(rowIdx);
    set(row, COL_QUESTION_TYPE, "TRUE_FALSE");
    set(row, COL_COGNITIVE_LEVEL, "VAN_DUNG");
    set(row, COL_POINTS, "1.0");
    set(row, COL_TAGS, "bảng biến thiên, cực trị, lớp 12");
    set(
        row,
        COL_QUESTION_TEXT,
        "Cho hàm số $y = f(x)$ xác định trên $\\mathbb{R}$ có bảng biến thiên như hình bên. "
            + "Xét tính đúng/sai của các phát biểu:",
        wrap);
    set(
        row,
        COL_DIAGRAM_DATA,
        "\\begin{tikzpicture}\n"
            + "\\tkzTabInit[lgt=2, espcl=2]"
            + " {$x$/1, $y'$/1, $y$/2}{$-\\infty$, $-1$, $1$, $+\\infty$}\n"
            + "\\tkzTabLine{, +, 0, -, 0, +}\n"
            + "\\tkzTabVar{-/$-\\infty$, +/$2$, -/$-2$, +/$+\\infty$}\n"
            + "\\end{tikzpicture}",
        wrap);
    set(row, COL_OPTION_A, "Hàm số đạt cực đại tại $x = -1$ và $f_{CD} = 2$.");
    set(row, COL_OPTION_B, "Hàm số đạt cực tiểu tại $x = 1$ và $f_{CT} = -2$.");
    set(
        row,
        COL_OPTION_C,
        "$\\lim\\limits_{x \\to -\\infty} f(x) = +\\infty$ và $\\lim\\limits_{x \\to +\\infty} f(x) = -\\infty$.");
    set(row, COL_OPTION_D, "Hàm số nghịch biến trên khoảng $(-1;\\,1)$.");
    set(row, COL_TRUTH_A, "TRUE");
    set(row, COL_TRUTH_B, "TRUE");
    set(row, COL_TRUTH_C, "FALSE");
    set(row, COL_TRUTH_D, "TRUE");
    // leave correctAnswer blank to demonstrate auto-derivation from truth columns
    set(row, COL_CORRECT_ANSWER, "");
    set(row, COL_VALIDATION_MODE, "");
    set(row, COL_TOLERANCE, "");
    set(
        row,
        COL_EXPLANATION,
        "Đọc bảng biến thiên: $y'$ đổi dấu từ $+$ sang $-$ tại $x=-1$ ⇒ cực đại, $f(-1)=2$ (A đúng); "
            + "$y'$ đổi dấu từ $-$ sang $+$ tại $x=1$ ⇒ cực tiểu, $f(1)=-2$ (B đúng); "
            + "giới hạn ở $-\\infty$ là $-\\infty$, ở $+\\infty$ là $+\\infty$ (C sai); "
            + "trên $(-1;1)$ thì $y' < 0$ ⇒ nghịch biến (D đúng).",
        wrap);
    set(
        row,
        COL_SOLUTION_STEPS,
        "Bước 1: Đọc dấu của $y'$ trên từng khoảng.\n"
            + "Bước 2: Xác định cực trị tại các điểm $y'$ đổi dấu.\n"
            + "Bước 3: Đọc giới hạn ở hai đầu của hàng $y$ trong bảng.\n"
            + "Bước 4: Đối chiếu từng phát biểu A→D với bảng.\n"
            + "Bước 5: Các phát biểu đúng là A, B, D.",
        wrap);
  }

  private void writeExampleShortAnswer(Sheet sheet, int rowIdx, CellStyle wrap) {
    Row row = sheet.createRow(rowIdx);
    set(row, COL_QUESTION_TYPE, "SHORT_ANSWER");
    set(row, COL_COGNITIVE_LEVEL, "VAN_DUNG");
    set(row, COL_POINTS, "1.0");
    set(row, COL_TAGS, "đồ thị hàm số, cực trị, lớp 12");
    set(
        row,
        COL_QUESTION_TEXT,
        "Cho hàm số $y = x^{3} - 3x$ có đồ thị như hình vẽ. Tính giá trị cực đại của hàm số "
            + "(làm tròn đến 3 chữ số thập phân nếu cần).",
        wrap);
    set(
        row,
        COL_DIAGRAM_DATA,
        "\\begin{tikzpicture}\n"
            + "\\begin{axis}[axis lines = middle, xmin=-3, xmax=3, ymin=-10, ymax=10, samples=100]\n"
            + "\\addplot[blue, thick]{x^3 - 3*x};\n"
            + "\\addplot[only marks, red] coordinates {(-1, 2) (1, -2) (0, 0)};\n"
            + "\\end{axis}\n"
            + "\\end{tikzpicture}",
        wrap);
    // SHORT_ANSWER has no options / truth columns
    set(row, COL_OPTION_A, "");
    set(row, COL_OPTION_B, "");
    set(row, COL_OPTION_C, "");
    set(row, COL_OPTION_D, "");
    set(row, COL_CORRECT_ANSWER, "2");
    set(row, COL_VALIDATION_MODE, "NUMERIC");
    set(row, COL_TOLERANCE, "0.001");
    set(
        row,
        COL_EXPLANATION,
        "Đạo hàm $y' = 3x^{2} - 3 = 3(x-1)(x+1)$. Phương trình $y'=0$ cho $x = \\pm 1$. "
            + "Lập bảng biến thiên, $y$ đổi dấu từ $+$ sang $-$ tại $x = -1$ ⇒ đây là điểm cực đại; "
            + "giá trị cực đại là $y(-1) = (-1)^{3} - 3(-1) = -1 + 3 = 2$.",
        wrap);
    set(
        row,
        COL_SOLUTION_STEPS,
        "Bước 1: Tính đạo hàm $y' = 3x^{2} - 3$.\n"
            + "Bước 2: Giải $y' = 0 \\Leftrightarrow x = \\pm 1$.\n"
            + "Bước 3: Lập bảng biến thiên, xác định $x = -1$ là điểm cực đại.\n"
            + "Bước 4: Tính $y(-1) = -1 + 3 = 2$.\n"
            + "Bước 5: Đáp số: giá trị cực đại bằng $2$.",
        wrap);
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

    sheet.createRow(r++).createCell(0).setCellValue("HƯỚNG DẪN IMPORT CÂU HỎI");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("CÁC LOẠI CÂU HỎI HỖ TRỢ:");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("• MULTIPLE_CHOICE — trắc nghiệm 4 đáp án A/B/C/D, có thể kèm hình LaTeX");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "• TRUE_FALSE — câu hỏi nhiều phát biểu, mỗi phát biểu A/B/C/D có cờ đúng/sai");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("• SHORT_ANSWER — đáp án ngắn (số/biểu thức), kèm chế độ chấm");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("MULTIPLE_CHOICE:");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- Bắt buộc điền optionA, optionB, optionC, optionD");
    sheet.createRow(r++).createCell(0).setCellValue("- correctAnswer phải là một trong A, B, C, D");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- Geometry MCQ: dán nguyên TikZ vào cột diagramData");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("TRUE_FALSE:");
    sheet.createRow(r++).createCell(0).setCellValue("- optionA-D chứa nội dung phát biểu (LaTeX)");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "- truthA-D chấp nhận: TRUE/FALSE, T/F, 1/0, Đ/S - bắt buộc cho mỗi phát biểu được dùng");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- correctAnswer được tự suy ra (vd 'A,C') từ truthA-D, có thể bỏ trống");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- Có thể có 2-4 phát biểu; phát biểu để trống sẽ bị bỏ qua");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- Bảng biến thiên: dán LaTeX (\\tkzTabInit ...) vào diagramData");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("SHORT_ANSWER:");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- correctAnswer là giá trị / biểu thức kỳ vọng");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- answerValidationMode: EXACT (mặc định) | NUMERIC | REGEX");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- answerTolerance: sai số cho NUMERIC, mặc định 0.001");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- Đồ thị/hàm số: dán LaTeX (TikZ/PGFPlots) vào diagramData");
    r++;

    sheet.createRow(r++).createCell(0).setCellValue("LATEX:");
    sheet.createRow(r++).createCell(0).setCellValue("- Inline: $...$ ; Block: $$...$$");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue(
            "- Cột diagramData giữ nguyên xuống dòng — dán nguyên \\begin{tikzpicture} ... \\end{tikzpicture}");
    sheet
        .createRow(r++)
        .createCell(0)
        .setCellValue("- KHÔNG cần escape backslash; cell Excel lưu thô");

    sheet.setColumnWidth(0, 120 * 256);
  }

  // ---- Parsing ---------------------------------------------------------------

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
    short first = row.getFirstCellNum();
    short last = row.getLastCellNum();
    if (first < 0) return true;
    for (int c = first; c < last; c++) {
      String value = getCellValueAsString(row, c);
      if (value != null && !value.isBlank()) {
        return false;
      }
    }
    return true;
  }

  private CreateQuestionRequest parseRowToRequest(Row row) throws Exception {
    String questionTypeStr = getCellValueAsString(row, COL_QUESTION_TYPE);
    String cognitiveLevelStr = getCellValueAsString(row, COL_COGNITIVE_LEVEL);
    String pointsStr = getCellValueAsString(row, COL_POINTS);
    String tagsStr = getCellValueAsString(row, COL_TAGS);
    String questionText = getCellValueAsString(row, COL_QUESTION_TEXT);
    String diagramData = getCellValueAsString(row, COL_DIAGRAM_DATA);
    String correctAnswerRaw = getCellValueAsString(row, COL_CORRECT_ANSWER);
    String explanation = getCellValueAsString(row, COL_EXPLANATION);
    String solutionSteps = getCellValueAsString(row, COL_SOLUTION_STEPS);

    QuestionType questionType = parseEnum(QuestionType.class, questionTypeStr, "questionType");
    CognitiveLevel cognitiveLevel =
        parseEnum(CognitiveLevel.class, cognitiveLevelStr, "cognitiveLevel");

    BigDecimal points = BigDecimal.valueOf(1.0);
    if (pointsStr != null && !pointsStr.isBlank()) {
      try {
        points = new BigDecimal(pointsStr.trim());
      } catch (NumberFormatException e) {
        throw new Exception("Invalid points value: " + pointsStr);
      }
    }

    String[] tags = null;
    if (tagsStr != null && !tagsStr.isBlank()) {
      String[] parts = tagsStr.split(",");
      List<String> cleaned = new ArrayList<>();
      for (String p : parts) {
        String t = p.trim();
        if (!t.isEmpty()) cleaned.add(t);
      }
      tags = cleaned.toArray(new String[0]);
    }

    Map<String, Object> options = null;
    String correctAnswer = correctAnswerRaw == null ? null : correctAnswerRaw.trim();
    Map<String, Object> generationMetadata = null;

    if (questionType == QuestionType.MULTIPLE_CHOICE) {
      options = readOptionMap(row);
      if (correctAnswer != null && !correctAnswer.isBlank()) {
        correctAnswer = correctAnswer.trim().toUpperCase();
      }
    } else if (questionType == QuestionType.TRUE_FALSE) {
      options = readOptionMap(row);
      Map<String, Object> tfClauses = new LinkedHashMap<>();
      List<String> trueKeys = new ArrayList<>();
      String[] keys = {"A", "B", "C", "D"};
      int[] truthCols = {COL_TRUTH_A, COL_TRUTH_B, COL_TRUTH_C, COL_TRUTH_D};
      for (int i = 0; i < keys.length; i++) {
        String key = keys[i];
        if (!options.containsKey(key)) {
          continue; // statement absent ⇒ ignore truth flag too
        }
        String truthRaw = getCellValueAsString(row, truthCols[i]);
        Boolean truth = parseTruth(truthRaw);
        if (truth == null) {
          throw new Exception(
              "Missing/invalid truth value for statement " + key + " (got '" + truthRaw + "')");
        }
        if (truth) trueKeys.add(key);
        Map<String, Object> clauseMeta = new LinkedHashMap<>();
        clauseMeta.put("truthValue", truth);
        clauseMeta.put("cognitiveLevel", cognitiveLevel == null ? null : cognitiveLevel.name());
        tfClauses.put(key, clauseMeta);
      }
      if (correctAnswer == null || correctAnswer.isBlank()) {
        correctAnswer = String.join(",", trueKeys);
      }
      generationMetadata = new LinkedHashMap<>();
      generationMetadata.put("tfClauses", tfClauses);
    } else if (questionType == QuestionType.SHORT_ANSWER) {
      // No options. correctAnswer is the expected value/expression.
      String validationModeRaw = getCellValueAsString(row, COL_VALIDATION_MODE);
      String toleranceRaw = getCellValueAsString(row, COL_TOLERANCE);
      String validationMode = "EXACT";
      if (validationModeRaw != null && !validationModeRaw.isBlank()) {
        validationMode = validationModeRaw.trim().toUpperCase();
        if (!VALIDATION_MODES.contains(validationMode)) {
          throw new Exception(
              "Invalid answerValidationMode: "
                  + validationModeRaw
                  + " (allowed: EXACT|NUMERIC|REGEX)");
        }
      }
      Double tolerance = null;
      if (toleranceRaw != null && !toleranceRaw.isBlank()) {
        try {
          tolerance = Double.parseDouble(toleranceRaw.trim());
        } catch (NumberFormatException e) {
          throw new Exception("Invalid answerTolerance: " + toleranceRaw);
        }
      }
      generationMetadata = new LinkedHashMap<>();
      generationMetadata.put("answerValidationMode", validationMode);
      generationMetadata.put("answerTolerance", tolerance == null ? 0.001 : tolerance);
    }

    return CreateQuestionRequest.builder()
        .questionText(questionText)
        .questionType(questionType)
        .cognitiveLevel(cognitiveLevel)
        .points(points)
        .correctAnswer(correctAnswer)
        .explanation(explanation)
        .solutionSteps(solutionSteps)
        .diagramData(emptyToNull(diagramData))
        .tags(tags)
        .options(options)
        .generationMetadata(generationMetadata)
        .build();
  }

  private Map<String, Object> readOptionMap(Row row) {
    Map<String, Object> options = new LinkedHashMap<>();
    int[] cols = {COL_OPTION_A, COL_OPTION_B, COL_OPTION_C, COL_OPTION_D};
    String[] keys = {"A", "B", "C", "D"};
    for (int i = 0; i < cols.length; i++) {
      String value = getCellValueAsString(row, cols[i]);
      if (value != null && !value.isBlank()) {
        options.put(keys[i], value);
      }
    }
    return options;
  }

  private Boolean parseTruth(String raw) {
    if (raw == null) return null;
    String v = raw.trim().toUpperCase();
    if (v.isEmpty()) return null;
    if (TRUTH_TRUE.contains(v)) return true;
    if (TRUTH_FALSE.contains(v)) return false;
    return null;
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

  /**
   * Render a numeric cell without losing decimals. Whole numbers come out as
   * "42" (not "42.0") so existing test expectations on integer points still
   * hold; values like 0.25 round-trip correctly.
   */
  private String formatNumeric(double value) {
    if (value == Math.floor(value) && !Double.isInfinite(value)) {
      return String.valueOf((long) value);
    }
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  // ---- Validation ------------------------------------------------------------

  private List<String> validateRequest(CreateQuestionRequest request) {
    List<String> errors = new ArrayList<>();

    Set<ConstraintViolation<CreateQuestionRequest>> violations = validator.validate(request);
    for (ConstraintViolation<CreateQuestionRequest> violation : violations) {
      errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
    }

    QuestionType type = request.getQuestionType();
    if (type == null) {
      // standard validation already complained; nothing more to add
      return errors;
    }

    switch (type) {
      case MULTIPLE_CHOICE -> validateMultipleChoice(request, errors);
      case TRUE_FALSE -> validateTrueFalse(request, errors);
      case SHORT_ANSWER -> validateShortAnswer(request, errors);
      default ->
          errors.add(
              "Loại câu hỏi '"
                  + type
                  + "' chưa được hỗ trợ trong import. Hỗ trợ: MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER");
    }

    return errors;
  }

  private void validateMultipleChoice(CreateQuestionRequest request, List<String> errors) {
    Map<String, Object> options = request.getOptions();
    Set<String> expectedKeys = Set.of("A", "B", "C", "D");
    if (options == null || options.size() != 4 || !options.keySet().equals(expectedKeys)) {
      errors.add(
          "MULTIPLE_CHOICE: phải có đủ 4 đáp án optionA, optionB, optionC, optionD. Hiện tại: "
              + (options == null ? "(rỗng)" : options.keySet()));
    } else {
      for (Map.Entry<String, Object> e : options.entrySet()) {
        Object v = e.getValue();
        if (v == null || (v instanceof String s && s.isBlank())) {
          errors.add("MULTIPLE_CHOICE: nội dung đáp án " + e.getKey() + " đang trống");
        }
      }
    }
    String correct = request.getCorrectAnswer();
    if (correct == null || !correct.matches("^[A-D]$")) {
      errors.add("MULTIPLE_CHOICE: correctAnswer phải là A, B, C hoặc D. Hiện tại: " + correct);
    }
  }

  private void validateTrueFalse(CreateQuestionRequest request, List<String> errors) {
    Map<String, Object> options = request.getOptions();
    if (options == null || options.size() < 2) {
      errors.add("TRUE_FALSE: cần ít nhất 2 phát biểu (optionA, optionB, ...)");
      return;
    }
    Set<String> allowedKeys = Set.of("A", "B", "C", "D");
    for (String k : options.keySet()) {
      if (!allowedKeys.contains(k)) {
        errors.add("TRUE_FALSE: nhãn phát biểu phải nằm trong A, B, C, D. Bị thừa: " + k);
      }
    }
    Map<String, Object> meta = request.getGenerationMetadata();
    @SuppressWarnings("unchecked")
    Map<String, Object> tfClauses =
        meta == null ? null : (Map<String, Object>) meta.get("tfClauses");
    if (tfClauses == null || tfClauses.size() != options.size()) {
      errors.add("TRUE_FALSE: số cờ truthA-D phải khớp số phát biểu (" + options.size() + ")");
    }
    String correct = request.getCorrectAnswer();
    if (correct != null && !correct.isBlank()) {
      for (String key : correct.split(",")) {
        String k = key.trim().toUpperCase();
        if (!allowedKeys.contains(k)) {
          errors.add("TRUE_FALSE: correctAnswer chứa nhãn không hợp lệ: " + key);
        } else if (!options.containsKey(k)) {
          errors.add("TRUE_FALSE: correctAnswer tham chiếu phát biểu không tồn tại: " + key);
        }
      }
    }
  }

  private void validateShortAnswer(CreateQuestionRequest request, List<String> errors) {
    if (request.getOptions() != null && !request.getOptions().isEmpty()) {
      errors.add("SHORT_ANSWER: không được điền optionA-D");
    }
    if (request.getCorrectAnswer() == null || request.getCorrectAnswer().isBlank()) {
      errors.add("SHORT_ANSWER: cần điền correctAnswer (giá trị/biểu thức kỳ vọng)");
    }
    Map<String, Object> meta = request.getGenerationMetadata();
    Object mode = meta == null ? null : meta.get("answerValidationMode");
    if (mode != null && !VALIDATION_MODES.contains(mode.toString())) {
      errors.add(
          "SHORT_ANSWER: answerValidationMode phải là EXACT, NUMERIC hoặc REGEX. Hiện tại: "
              + mode);
    }
  }
}
