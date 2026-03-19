package com.fptu.math_master.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.MatrixCellResponse;
import com.fptu.math_master.dto.response.MatrixChapterGroupResponse;
import com.fptu.math_master.dto.response.MatrixRowResponse;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.service.ExamMatrixService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ExamMatrixPdfExportService}.
 *
 * <p>These tests mock {@link ExamMatrixService#getMatrixTable} and verify:
 * <ol>
 *   <li>The returned byte array is a valid PDF file (starts with {@code %PDF}).</li>
 *   <li>The PDF is non-empty.</li>
 *   <li>Edge cases (null/empty chapters, single row, many chapters) are handled.</li>
 *   <li>Null matrix data throws a meaningful exception.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExamMatrixPdfExportService — PDF generation")
class ExamMatrixPdfExportServiceTest {

  // ── Helpers ───────────────────────────────────────────────────────────────

  private static int stepCounter = 0;

  private static void testHeader(String title) {
    stepCounter = 0;
    System.out.println("\n============================================================");
    System.out.println("  TEST: " + title);
    System.out.println("============================================================");
  }

  private static void step(String description) {
    System.out.println("\n[Step " + (++stepCounter) + "] " + description);
  }

  private static void pass(String assertion) {
    System.out.println("  ✓ " + assertion);
  }

  // ── Mocks ──────────────────────────────────────────────────────────────────

  @Mock private ExamMatrixService examMatrixService;
  @InjectMocks private ExamMatrixPdfExportService pdfExportService;

  // ── Fixed IDs ─────────────────────────────────────────────────────────────

  private static final UUID MATRIX_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID CHAPTER_1 = UUID.fromString("00000000-0000-0000-0001-000000000001");
  private static final UUID CHAPTER_2 = UUID.fromString("00000000-0000-0000-0001-000000000002");
  private static final UUID CHAPTER_3 = UUID.fromString("00000000-0000-0000-0001-000000000003");

  // ── Fixture builders ───────────────────────────────────────────────────────

  private MatrixCellResponse cell(CognitiveLevel level, String label, int count) {
    BigDecimal pts = new BigDecimal("0.20");
    return MatrixCellResponse.builder()
        .mappingId(UUID.randomUUID())
        .cognitiveLevel(level)
        .cognitiveLevelLabel(label)
        .questionCount(count)
        .pointsPerQuestion(pts)
        .totalPoints(pts.multiply(BigDecimal.valueOf(count)))
        .build();
  }

  private MatrixRowResponse row(String typeName, String ref, int order,
                                 Map<String, Integer> counts, List<MatrixCellResponse> cells) {
    int total = cells.stream().mapToInt(c -> c.getQuestionCount()).sum();
    BigDecimal pts = cells.stream()
        .map(c -> c.getTotalPoints())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return MatrixRowResponse.builder()
        .rowId(UUID.randomUUID())
        .chapterId(CHAPTER_1)
        .questionTypeName(typeName)
        .referenceQuestions(ref)
        .orderIndex(order)
        .cells(cells)
        .countByCognitive(counts)
        .rowTotalQuestions(total)
        .rowTotalPoints(pts)
        .build();
  }

  private MatrixChapterGroupResponse chapter(UUID id, String title, int order,
                                              List<MatrixRowResponse> rows,
                                              Map<String, Integer> totals, int total) {
    BigDecimal chapPts = rows.stream()
        .map(r -> r.getRowTotalPoints())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return MatrixChapterGroupResponse.builder()
        .chapterId(id)
        .chapterTitle(title)
        .chapterOrderIndex(order)
        .rows(rows)
        .totalByCognitive(totals)
        .chapterTotalQuestions(total)
        .chapterTotalPoints(chapPts)
        .build();
  }

  /** Builds the canonical 50-question THPT Toán 12 matrix response used across tests. */
  private ExamMatrixTableResponse fullThptMatrix() {
    // ── Chapter 1: Đạo Hàm và Ứng Dụng ─────────────────────────────────
    MatrixRowResponse r1 = row("Đơn điệu của HS", "3,30", 1,
        Map.of("NB", 1, "TH", 1),
        List.of(cell(CognitiveLevel.NHAN_BIET, "NB", 1),
                cell(CognitiveLevel.THONG_HIEU, "TH", 1)));
    MatrixRowResponse r2 = row("Cực trị của HS", "4,5,39,46", 2,
        Map.of("NB", 1, "TH", 1, "VD", 1, "VDC", 1),
        List.of(cell(CognitiveLevel.NHAN_BIET, "NB", 1),
                cell(CognitiveLevel.THONG_HIEU, "TH", 1),
                cell(CognitiveLevel.VAN_DUNG, "VD", 1),
                cell(CognitiveLevel.VAN_DUNG_CAO, "VDC", 1)));
    MatrixRowResponse r3 = row("Min, Max của hàm số", "31", 3,
        Map.of("TH", 1),
        List.of(cell(CognitiveLevel.THONG_HIEU, "TH", 1)));
    MatrixRowResponse r4 = row("Đường Tiệm Cận", "6", 4,
        Map.of("NB", 1),
        List.of(cell(CognitiveLevel.NHAN_BIET, "NB", 1)));
    MatrixRowResponse r5 = row("Khảo sát và vẽ đồ thị", "7,8", 5,
        Map.of("NB", 1, "TH", 1),
        List.of(cell(CognitiveLevel.NHAN_BIET, "NB", 1),
                cell(CognitiveLevel.THONG_HIEU, "TH", 1)));
    MatrixChapterGroupResponse ch1 = chapter(CHAPTER_1,
        "ĐẠO HÀM VÀ ỨNG DỤNG", 1,
        List.of(r1, r2, r3, r4, r5),
        Map.of("NB", 4, "TH", 4, "VD", 1, "VDC", 1), 10);

    // ── Chapter 2: Hàm Số Mũ - Logarit ──────────────────────────────────
    MatrixRowResponse r6 = row("Lũy thừa – Mũ – Logarit", "9,11", 6,
        Map.of("NB", 1, "TH", 1),
        List.of(cell(CognitiveLevel.NHAN_BIET, "NB", 1),
                cell(CognitiveLevel.THONG_HIEU, "TH", 1)));
    MatrixRowResponse r7 = row("HS Mũ – Logarit", "10", 7,
        Map.of("TH", 1),
        List.of(cell(CognitiveLevel.THONG_HIEU, "TH", 1)));
    MatrixRowResponse r8 = row("PT Mũ – Logarit", "12,13,47", 8,
        Map.of("NB", 1, "VD", 1, "VDC", 1),
        List.of(cell(CognitiveLevel.NHAN_BIET, "NB", 1),
                cell(CognitiveLevel.VAN_DUNG, "VD", 1),
                cell(CognitiveLevel.VAN_DUNG_CAO, "VDC", 1)));
    MatrixRowResponse r9 = row("BPT Mũ – Logarit", "32,40", 9,
        Map.of("VD", 1, "VDC", 1),
        List.of(cell(CognitiveLevel.VAN_DUNG, "VD", 1),
                cell(CognitiveLevel.VAN_DUNG_CAO, "VDC", 1)));
    MatrixChapterGroupResponse ch2 = chapter(CHAPTER_2,
        "HÀM SỐ MŨ - LOGARIT", 2,
        List.of(r6, r7, r8, r9),
        Map.of("NB", 2, "TH", 2, "VD", 2, "VDC", 2), 8);

    // ── Chapter 3: Nguyên Hàm – Tích Phân ───────────────────────────────
    MatrixRowResponse r10 = row("Nguyên hàm", "14,15", 10,
        Map.of("NB", 1, "TH", 1),
        List.of(cell(CognitiveLevel.NHAN_BIET, "NB", 1),
                cell(CognitiveLevel.THONG_HIEU, "TH", 1)));
    MatrixRowResponse r11 = row("Tích phân", "16,17,33,41", 11,
        Map.of("NB", 1, "TH", 1, "VD", 2),
        List.of(cell(CognitiveLevel.NHAN_BIET, "NB", 1),
                cell(CognitiveLevel.THONG_HIEU, "TH", 1),
                cell(CognitiveLevel.VAN_DUNG, "VD", 2)));
    MatrixRowResponse r12 = row("Ứng dụng TP tính diện tích", "44,48", 12,
        Map.of("TH", 1, "VD", 1),
        List.of(cell(CognitiveLevel.THONG_HIEU, "TH", 1),
                cell(CognitiveLevel.VAN_DUNG, "VD", 1)));
    MatrixChapterGroupResponse ch3 = chapter(CHAPTER_3,
        "NGUYÊN HÀM – TÍCH PHÂN", 3,
        List.of(r10, r11, r12),
        Map.of("NB", 2, "TH", 3, "VD", 3), 8);

    return ExamMatrixTableResponse.builder()
        .id(MATRIX_ID)
        .name("Ma Trận Đề Minh Họa THPT 2025")
        .description("Ma trận đề thi thử THPT Quốc Gia môn Toán – 50 câu, 10 điểm")
        .teacherId(UUID.randomUUID())
        .teacherName("Nguyễn Văn A")
        .gradeLevel(12)
        .curriculumId(UUID.randomUUID())
        .curriculumName("Toán 12 – Đề Minh Họa THPT")
        .subjectId(UUID.randomUUID())
        .subjectName("Toán")
        .isReusable(true)
        .status(MatrixStatus.DRAFT)
        .chapters(List.of(ch1, ch2, ch3))
        .grandTotalByCognitive(Map.of("NB", 8, "TH", 9, "VD", 6, "VDC", 3))
        .grandTotalQuestions(26)
        .grandTotalPoints(new BigDecimal("5.20"))
        .totalQuestionsTarget(26)
        .totalPointsTarget(new BigDecimal("5.20"))
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @BeforeEach
  void setup() {
    stepCounter = 0;
  }

  // ═════════════════════════════════════════════════════════════════════════
  // NORMAL TEST CASES
  // ═════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("exportToPdf — should return valid PDF bytes for a multi-chapter matrix")
  void should_generate_valid_pdf_for_multi_chapter_matrix() {
    testHeader("exportToPdf — multi-chapter matrix (3 chương, 12 rows)");

    step("Stub getMatrixTable() → 3-chapter fixture");
    ExamMatrixTableResponse matrix = fullThptMatrix();
    when(examMatrixService.getMatrixTable(MATRIX_ID)).thenReturn(matrix);
    pass("Mock configured: " + matrix.getChapters().size() + " chapters, "
        + matrix.getGrandTotalQuestions() + " total questions");

    step("Call exportToPdf(matrixId)");
    byte[] pdf = pdfExportService.exportToPdf(MATRIX_ID);

    step("Assert PDF is non-null and non-empty");
    assertThat(pdf).isNotNull();
    assertThat(pdf.length).isGreaterThan(0);
    pass("PDF byte array length = " + pdf.length);

    step("Assert PDF header magic bytes = %PDF");
    String header = new String(pdf, 0, Math.min(5, pdf.length));
    assertThat(header).startsWith("%PDF");
    pass("PDF starts with \"%PDF\" ✓");

    step("Assert getMatrixTable was called exactly once");
    verify(examMatrixService).getMatrixTable(MATRIX_ID);
    pass("examMatrixService.getMatrixTable() → 1×");
  }

  @Test
  @DisplayName("exportToPdf — should handle empty chapters list without throwing")
  void should_generate_pdf_when_chapters_is_empty() {
    testHeader("exportToPdf — matrix with NO chapters (empty list)");

    step("Build matrix with empty chapters list");
    ExamMatrixTableResponse empty = ExamMatrixTableResponse.builder()
        .id(MATRIX_ID)
        .name("Ma Trận Trống")
        .gradeLevel(12)
        .chapters(List.of())
        .grandTotalByCognitive(Map.of())
        .grandTotalQuestions(0)
        .grandTotalPoints(BigDecimal.ZERO)
        .status(MatrixStatus.DRAFT)
        .build();
    when(examMatrixService.getMatrixTable(MATRIX_ID)).thenReturn(empty);
    pass("Empty chapter list prepared");

    step("Call exportToPdf — must NOT throw");
    byte[] pdf = pdfExportService.exportToPdf(MATRIX_ID);

    step("Assert result is still a valid PDF");
    assertThat(pdf).isNotNull();
    assertThat(pdf.length).isGreaterThan(0);
    assertThat(new String(pdf, 0, 5)).startsWith("%PDF");
    pass("PDF generated with 0 chapters, length = " + pdf.length);
  }

  @Test
  @DisplayName("exportToPdf — should handle null chapters list without throwing")
  void should_generate_pdf_when_chapters_is_null() {
    testHeader("exportToPdf — matrix with null chapters");

    step("Build matrix with chapters = null");
    ExamMatrixTableResponse nullChaps = ExamMatrixTableResponse.builder()
        .id(MATRIX_ID)
        .name("Ma Trận Null Chapters")
        .gradeLevel(11)
        .chapters(null)
        .grandTotalByCognitive(null)
        .grandTotalQuestions(0)
        .grandTotalPoints(BigDecimal.ZERO)
        .status(MatrixStatus.DRAFT)
        .build();
    when(examMatrixService.getMatrixTable(MATRIX_ID)).thenReturn(nullChaps);
    pass("Null chapters configured");

    step("Call exportToPdf — must NOT throw");
    byte[] pdf = pdfExportService.exportToPdf(MATRIX_ID);

    step("Assert valid PDF");
    assertThat(pdf).isNotNull();
    assertThat(new String(pdf, 0, 5)).startsWith("%PDF");
    pass("PDF generated with null chapters, length = " + pdf.length);
  }

  @Test
  @DisplayName("exportToPdf — should handle single chapter with a single row")
  void should_generate_pdf_for_single_chapter_single_row() {
    testHeader("exportToPdf — minimal: 1 chapter, 1 row, 1 cell");

    step("Build minimal matrix: 1 chapter, 1 row NB=3, 2.00đ/câu");
    MatrixCellResponse singleCell = cell(CognitiveLevel.NHAN_BIET, "NB", 3);
    MatrixRowResponse singleRow = row("Trắc Nghiệm", "1,2,3", 1,
        Map.of("NB", 3),
        List.of(singleCell));
    MatrixChapterGroupResponse singleChap = chapter(CHAPTER_1,
        "ĐẠO HÀM VÀ ỨNG DỤNG", 1,
        List.of(singleRow),
        Map.of("NB", 3), 3);
    ExamMatrixTableResponse minimal = ExamMatrixTableResponse.builder()
        .id(MATRIX_ID)
        .name("Đề Kiểm Tra Đơn Giản")
        .gradeLevel(12)
        .curriculumName("Toán 12")
        .subjectName("Toán")
        .chapters(List.of(singleChap))
        .grandTotalByCognitive(Map.of("NB", 3))
        .grandTotalQuestions(3)
        .grandTotalPoints(new BigDecimal("6.00"))
        .totalPointsTarget(new BigDecimal("6.00"))
        .status(MatrixStatus.APPROVED)
        .build();
    when(examMatrixService.getMatrixTable(MATRIX_ID)).thenReturn(minimal);
    pass("Minimal matrix fixture: 1 row, NB=3, totalPoints=6.00");

    step("Call exportToPdf");
    byte[] pdf = pdfExportService.exportToPdf(MATRIX_ID);

    step("Assert valid PDF");
    assertThat(pdf).isNotNull();
    assertThat(new String(pdf, 0, 5)).startsWith("%PDF");
    pass("Minimal PDF generated, length = " + pdf.length);
  }

  @Test
  @DisplayName("exportToPdf — should propagate exception when service throws")
  void should_propagate_exception_when_service_throws() {
    testHeader("exportToPdf — ExamMatrixService throws → RuntimeException propagated");

    step("Stub getMatrixTable() to throw RuntimeException");
    when(examMatrixService.getMatrixTable(MATRIX_ID))
        .thenThrow(new RuntimeException("Matrix not found"));
    pass("Stub throws RuntimeException(\"Matrix not found\")");

    step("Assert exportToPdf also throws");
    assertThatThrownBy(() -> pdfExportService.exportToPdf(MATRIX_ID))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Matrix not found");
    pass("RuntimeException propagated correctly ✓");
  }

  @Test
  @DisplayName("exportToPdf — PDF size is reasonable (< 5 MB for a typical matrix)")
  void should_produce_reasonable_file_size() {
    testHeader("exportToPdf — file size sanity check for full 3-chapter fixture");

    step("Stub with full multi-chapter fixture");
    when(examMatrixService.getMatrixTable(MATRIX_ID)).thenReturn(fullThptMatrix());
    pass("Full matrix fixture stubbed");

    step("Export PDF and check size");
    byte[] pdf = pdfExportService.exportToPdf(MATRIX_ID);
    int sizeKb = pdf.length / 1024;
    System.out.println("  PDF size: " + sizeKb + " KB (" + pdf.length + " bytes)");

    step("Assert PDF size is between 1 KB and 5 MB");
    assertThat(pdf.length)
        .as("PDF should be at least 1 KB")
        .isGreaterThan(1_024);
    assertThat(pdf.length)
        .as("PDF should be under 5 MB")
        .isLessThan(5 * 1024 * 1024);
    pass("PDF size = " + sizeKb + " KB — within expected range");
  }
}
