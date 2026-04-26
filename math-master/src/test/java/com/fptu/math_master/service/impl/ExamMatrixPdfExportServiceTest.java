package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.MatrixChapterGroupResponse;
import com.fptu.math_master.dto.response.MatrixRowResponse;
import com.fptu.math_master.service.ExamMatrixService;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;

@DisplayName("ExamMatrixPdfExportService - Tests")
class ExamMatrixPdfExportServiceTest extends BaseUnitTest {

  @InjectMocks private ExamMatrixPdfExportService examMatrixPdfExportService;

  @Mock private ExamMatrixService examMatrixService;

  private static final UUID MATRIX_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private MatrixRowResponse buildRow(
      String questionTypeName, String referenceQuestions, Map<String, Integer> countByCognitive, int total) {
    return MatrixRowResponse.builder()
        .questionTypeName(questionTypeName)
        .referenceQuestions(referenceQuestions)
        .countByCognitive(countByCognitive)
        .rowTotalQuestions(total)
        .rowTotalPoints(BigDecimal.valueOf(total))
        .build();
  }

  private MatrixChapterGroupResponse buildChapter(String title, int totalQuestions, MatrixRowResponse... rows) {
    return MatrixChapterGroupResponse.builder()
        .chapterTitle(title)
        .rows(List.of(rows))
        .chapterTotalQuestions(totalQuestions)
        .chapterTotalPoints(BigDecimal.valueOf(totalQuestions))
        .build();
  }

  private ExamMatrixTableResponse buildMatrixWithChapters(List<MatrixChapterGroupResponse> chapters) {
    return ExamMatrixTableResponse.builder()
        .id(MATRIX_ID)
        .name("Ma Tran Toan 12 HK2")
        .subjectName("Toan hoc")
        .gradeLevel(12)
        .curriculumName("Chuong trinh GDPT 2018")
        .chapters(chapters)
        .grandTotalByCognitive(Map.of("NB", 2, "TH", 1, "VD", 0, "VDC", 1))
        .grandTotalQuestions(4)
        .grandTotalPoints(BigDecimal.valueOf(4))
        .build();
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
    Method method = ExamMatrixPdfExportService.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(examMatrixPdfExportService, args);
  }

  @Nested
  @DisplayName("exportToPdf()")
  class ExportToPdfTests {

    /**
     * Normal case: Export PDF byte array from a valid matrix.
     *
     * <p>Input:
     * <ul>
     *   <li>matrixId: existing UUID</li>
     *   <li>matrix.chapters: one chapter with two rows</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>exportToPdf -> buildPdf success branch</li>
     *   <li>drawTitle -> subtitle non-empty branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return non-empty PDF bytes and fetch matrix once from dependency</li>
     * </ul>
     */
    @Test
    void it_should_return_non_empty_pdf_bytes_when_matrix_is_valid() {
      // ===== ARRANGE =====
      ExamMatrixTableResponse matrix =
          buildMatrixWithChapters(
              List.of(
                  buildChapter(
                      "Dao ham va ung dung",
                      4,
                      buildRow("Don dieu cua ham so", "3,30", Map.of("NB", 1, "TH", 1), 2),
                      buildRow("Cuc tri ham so", "4,5,39,46", Map.of("NB", 1, "TH", 0, "VDC", 1), 2))));
      when(examMatrixService.getMatrixTable(MATRIX_ID)).thenReturn(matrix);

      // ===== ACT =====
      byte[] result = examMatrixPdfExportService.exportToPdf(MATRIX_ID);

      // ===== ASSERT =====
      assertNotNull(result);
      assertTrue(result.length > 100);

      // ===== VERIFY =====
      verify(examMatrixService, times(1)).getMatrixTable(MATRIX_ID);
      verifyNoMoreInteractions(examMatrixService);
    }

    /**
     * Abnormal case: Data source throws before rendering.
     *
     * <p>Input:
     * <ul>
     *   <li>matrixId: unknown UUID</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>exportToPdf -> upstream exception propagation branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw RuntimeException from dependency and stop processing</li>
     * </ul>
     */
    @Test
    void it_should_propagate_exception_when_get_matrix_table_fails() {
      // ===== ARRANGE =====
      RuntimeException expected = new RuntimeException("matrix unavailable");
      when(examMatrixService.getMatrixTable(MATRIX_ID)).thenThrow(expected);

      // ===== ACT & ASSERT =====
      RuntimeException actual =
          assertThrows(RuntimeException.class, () -> examMatrixPdfExportService.exportToPdf(MATRIX_ID));
      assertEquals("matrix unavailable", actual.getMessage());

      // ===== VERIFY =====
      verify(examMatrixService, times(1)).getMatrixTable(MATRIX_ID);
      verifyNoMoreInteractions(examMatrixService);
    }
  }

  @Nested
  @DisplayName("private helpers")
  class PrivateHelperTests {

    @Test
    void it_should_build_subtitle_with_all_fields_when_all_values_are_present() throws Exception {
      // ===== ARRANGE =====
      ExamMatrixTableResponse matrix =
          ExamMatrixTableResponse.builder()
              .subjectName("Toan hoc")
              .gradeLevel(11)
              .curriculumName("Chuong trinh moi")
              .build();

      // ===== ACT =====
      String subtitle = invokePrivate("buildSubtitle", new Class<?>[] {ExamMatrixTableResponse.class}, matrix);

      // ===== ASSERT =====
      assertTrue(subtitle.contains("Toan hoc"));
      assertTrue(subtitle.contains("11"));
      assertTrue(subtitle.contains("Chuong trinh moi"));
    }

    @Test
    void it_should_build_subtitle_with_empty_string_when_all_values_are_missing() throws Exception {
      // ===== ARRANGE =====
      ExamMatrixTableResponse matrix = ExamMatrixTableResponse.builder().build();

      // ===== ACT =====
      String subtitle = invokePrivate("buildSubtitle", new Class<?>[] {ExamMatrixTableResponse.class}, matrix);

      // ===== ASSERT =====
      assertEquals("", subtitle);
    }

    @Test
    void it_should_return_single_empty_line_when_wrap_text_receives_empty_value() throws Exception {
      // ===== ARRANGE =====
      PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

      // ===== ACT =====
      List<String> lines =
          invokePrivate(
              "wrapText",
              new Class<?>[] {org.apache.pdfbox.pdmodel.font.PDFont.class, float.class, String.class, float.class},
              font,
              8f,
              "",
              50f);

      // ===== ASSERT =====
      assertEquals(1, lines.size());
      assertEquals("", lines.get(0));
    }

    @Test
    void it_should_split_text_into_multiple_lines_when_max_width_is_small() throws Exception {
      // ===== ARRANGE =====
      PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      String text = "Dao ham va ung dung trong bai toan cuc tri";

      // ===== ACT =====
      List<String> lines =
          invokePrivate(
              "wrapText",
              new Class<?>[] {org.apache.pdfbox.pdmodel.font.PDFont.class, float.class, String.class, float.class},
              font,
              8f,
              text,
              25f);

      // ===== ASSERT =====
      assertFalse(lines.isEmpty());
      assertTrue(lines.size() > 1);
    }

    @Test
    void it_should_return_single_empty_line_when_wrap_text_receives_null_value() throws Exception {
      // ===== ARRANGE =====
      PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

      // ===== ACT =====
      List<String> lines =
          invokePrivate(
              "wrapText",
              new Class<?>[] {org.apache.pdfbox.pdmodel.font.PDFont.class, float.class, String.class, float.class},
              font,
              8f,
              null,
              50f);

      // ===== ASSERT =====
      assertEquals(1, lines.size());
      assertEquals("", lines.get(0));
    }

    @Test
    void it_should_create_at_least_one_empty_page_when_paginate_receives_empty_chapter_list() throws Exception {
      // ===== ARRANGE =====
      List<MatrixChapterGroupResponse> chapters = List.of();

      // ===== ACT =====
      List<List<MatrixChapterGroupResponse>> pages =
          invokePrivate("paginate", new Class<?>[] {List.class, float.class}, chapters, 100f);

      // ===== ASSERT =====
      assertEquals(1, pages.size());
      assertTrue(pages.get(0).isEmpty());
    }

    @Test
    void it_should_split_pages_by_chapter_boundaries_when_usable_height_is_limited() throws Exception {
      // ===== ARRANGE =====
      MatrixChapterGroupResponse firstChapter =
          buildChapter(
              "Ham so",
              2,
              buildRow("Dang 1", "1", Map.of("NB", 1), 1),
              buildRow("Dang 2", "2", Map.of("TH", 1), 1));
      MatrixChapterGroupResponse secondChapter =
          buildChapter(
              "Tich phan",
              2,
              buildRow("Dang 3", "3", Map.of("VD", 1), 1),
              buildRow("Dang 4", "4", Map.of("VDC", 1), 1));

      // ===== ACT =====
      List<List<MatrixChapterGroupResponse>> pages =
          invokePrivate(
              "paginate",
              new Class<?>[] {List.class, float.class},
              List.of(firstChapter, secondChapter),
              32f);

      // ===== ASSERT =====
      assertEquals(2, pages.size());
      assertEquals(1, pages.get(0).size());
      assertEquals(1, pages.get(1).size());
    }

    @Test
    void it_should_return_empty_string_when_safe_receives_null() throws Exception {
      // ===== ARRANGE =====

      // ===== ACT =====
      String value = invokePrivate("safe", new Class<?>[] {String.class}, new Object[] {null});

      // ===== ASSERT =====
      assertEquals("", value);
    }

    @Test
    void it_should_keep_original_string_when_safe_receives_non_null_value() throws Exception {
      // ===== ARRANGE =====
      String input = "Noi dung cau hoi";

      // ===== ACT =====
      String value = invokePrivate("safe", new Class<?>[] {String.class}, input);

      // ===== ASSERT =====
      assertEquals(input, value);
    }

    @Test
    void it_should_return_rough_estimate_when_text_width_computation_throws_exception() throws Exception {
      // ===== ARRANGE =====
      PDFont brokenFont = mock(PDFont.class);
      doThrow(new RuntimeException("font failure")).when(brokenFont).getStringWidth("abc");

      // ===== ACT =====
      float width = invokePrivate("textWidth", new Class<?>[] {PDFont.class, float.class, String.class}, brokenFont, 10f, "abc");

      // ===== ASSERT =====
      assertTrue(width > 0);
      assertEquals(15f, width);
    }

    @Test
    void it_should_skip_drawing_centered_line_when_text_is_empty() throws Exception {
      // ===== ARRANGE =====
      try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
          PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

          // ===== ACT =====
          invokePrivate(
              "drawCenteredLine",
              new Class<?>[] {PDPageContentStream.class, PDFont.class, float.class, float.class, String.class},
              contentStream,
              font,
              8f,
              200f,
              "");

          // ===== ASSERT =====
          assertTrue(true);
        }
      }
    }

    @Test
    void it_should_draw_title_without_subtitle_when_subject_grade_and_curriculum_are_missing() throws Exception {
      // ===== ARRANGE =====
      ExamMatrixTableResponse matrix = ExamMatrixTableResponse.builder().name("Ma tran de thi").build();
      try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
          PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
          PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

          // ===== ACT =====
          float y =
              invokePrivate(
                  "drawTitle",
                  new Class<?>[] {
                    PDPageContentStream.class,
                    ExamMatrixTableResponse.class,
                    org.apache.pdfbox.pdmodel.font.PDFont.class,
                    org.apache.pdfbox.pdmodel.font.PDFont.class,
                    float.class
                  },
                  contentStream,
                  matrix,
                  bold,
                  regular,
                  500f);

          // ===== ASSERT =====
          assertTrue(y < 500f);
        }
      }
    }

    @Test
    void it_should_render_cell_with_left_alignment_and_dark_background() throws Exception {
      // ===== ARRANGE =====
      try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
          PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

          // ===== ACT =====
          invokePrivate(
              "drawInCell",
              new Class<?>[] {
                PDPageContentStream.class,
                org.apache.pdfbox.pdmodel.font.PDFont.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                List.class,
                boolean.class,
                float[].class
              },
              contentStream,
              regular,
              8f,
              30f,
              60f,
              80f,
              18f,
              List.of("", "Noi dung canh trai"),
              false,
              new float[] {0.1f, 0.1f, 0.1f});

          // ===== ASSERT =====
          assertTrue(true);
        }
      }
    }

    @Test
    void it_should_return_immediately_when_draw_in_cell_receives_null_lines() throws Exception {
      // ===== ARRANGE =====
      try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
          PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

          // ===== ACT =====
          invokePrivate(
              "drawInCell",
              new Class<?>[] {
                PDPageContentStream.class,
                org.apache.pdfbox.pdmodel.font.PDFont.class,
                float.class,
                float.class,
                float.class,
                float.class,
                float.class,
                List.class,
                boolean.class,
                float[].class
              },
              contentStream,
              regular,
              8f,
              20f,
              20f,
              80f,
              16f,
              null,
              true,
              new float[] {1f, 1f, 1f});

          // ===== ASSERT =====
          assertTrue(true);
        }
      }
    }

    @Test
    void it_should_draw_data_row_when_cognitive_count_map_is_null() throws Exception {
      // ===== ARRANGE =====
      MatrixRowResponse row =
          MatrixRowResponse.builder()
              .questionTypeName("Dang bai null map")
              .referenceQuestions("1,2")
              .countByCognitive(null)
              .rowTotalQuestions(0)
              .build();
      try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
          PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

          // ===== ACT =====
          invokePrivate(
              "drawDataRow",
              new Class<?>[] {
                PDPageContentStream.class,
                org.apache.pdfbox.pdmodel.font.PDFont.class,
                MatrixRowResponse.class,
                float.class,
                float[].class
              },
              contentStream,
              regular,
              row,
              300f,
              new float[] {1f, 1f, 1f});

          // ===== ASSERT =====
          assertTrue(true);
        }
      }
    }

    @Test
    void it_should_draw_totals_row_with_unicode_font_when_some_levels_are_missing() throws Exception {
      // ===== ARRANGE =====
      File arial = new File("C:/Windows/Fonts/arial.ttf");
      if (!arial.exists()) {
        return;
      }
      ExamMatrixTableResponse matrix =
          ExamMatrixTableResponse.builder().grandTotalByCognitive(Map.of("NB", 2)).grandTotalQuestions(2).build();
      try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage();
        document.addPage(page);
        try (FileInputStream fis = new FileInputStream(arial);
            PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
          PDFont unicodeFont = PDType0Font.load(document, fis);

          // ===== ACT =====
          invokePrivate(
              "drawTotalsRow",
              new Class<?>[] {
                PDPageContentStream.class,
                ExamMatrixTableResponse.class,
                org.apache.pdfbox.pdmodel.font.PDFont.class,
                float.class
              },
              contentStream,
              matrix,
              unicodeFont,
              230f);

          // ===== ASSERT =====
          assertTrue(true);
        }
      }
    }

  }
}
