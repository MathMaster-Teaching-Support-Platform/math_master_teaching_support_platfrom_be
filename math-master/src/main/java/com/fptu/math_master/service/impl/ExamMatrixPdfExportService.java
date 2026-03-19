package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.MatrixChapterGroupResponse;
import com.fptu.math_master.dto.response.MatrixRowResponse;
import com.fptu.math_master.service.ExamMatrixService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

/**
 * Generates a PDF of an exam matrix (ma trận đề thi) in the standard Vietnamese format:
 *
 * <pre>
 * ┌─────┬──────────┬──────────────┬──────────┬───────────────────┬────────┬────────┐
 * │ Lớp │ Chương   │ Dạng bài     │ Trích dẫn│     MỨC ĐỘ        │ Tổng   │ Tổng   │
 * │     │          │              │          ├────┬────┬────┬─────┤ Dạng   │ Chương │
 * │     │          │              │          │ NB │ TH │ VD │ VDC │ bài    │        │
 * ├─────┼──────────┼──────────────┼──────────┼────┼────┼────┼─────┼────────┼────────┤
 * │ 12  │ ĐẠO HÀM  │ Đơn điệu HS │ 3,30     │  1 │  1 │    │     │   2    │        │
 * │     │          │ Cực trị HS   │ 4,5,39,46│  1 │  1 │  1 │   1 │   4    │   10   │
 * ├─────┼──────────┴──────────────┴──────────┴────┴────┴────┴─────┴────────┴────────┤
 * │     │ Tổng     │              │          │ 20 │ 15 │ 10 │   5 │   50   │        │
 * └─────┴──────────┴──────────────┴──────────┴────┴────┴────┴─────┴────────┴────────┘
 * </pre>
 *
 * <p>Font note: Vietnamese characters require a Unicode-capable TrueType font.
 * On Alpine Docker, install with: {@code apk add --no-cache ttf-freefont}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExamMatrixPdfExportService {

  private final ExamMatrixService examMatrixService;

  // ── Page: A4 Landscape ──────────────────────────────────────────────────

  private static final float PW = 841.89f; // A4 landscape width  (pts)
  private static final float PH = 595.28f; // A4 landscape height (pts)
  private static final float ML = 28f; // left / right margin
  private static final float MT = 25f; // top margin
  private static final float MB = 18f; // bottom margin

  // ── Heights ──────────────────────────────────────────────────────────────

  private static final float TITLE_H = 30f; // title block (name + subtitle)
  private static final float HDR1_H = 22f; // "Lớp / Chương / MỨC ĐỘ / …" row
  private static final float HDR2_H = 14f; // "NB / TH / VD / VDC" sub-row
  private static final float ROW_H = 16f; // data row
  private static final float TOTL_H = 18f; // grand-totals row

  // ── Columns ───────────────────────────────────────────────────────────────
  // 0=Lớp  1=Chương  2=Dạng bài  3=Trích dẫn  4=NB  5=TH  6=VD  7=VDC  8=TổngDB  9=TổngChương

  private static final float[] CW;
  private static final float[] CX; // column X start positions
  private static final float TW; // total table width

  static {
    CW = new float[] {28, 122, 195, 118, 41, 41, 41, 41, 65, 65};
    float tw = 0;
    for (float w : CW) tw += w;
    TW = tw; // ~757 pts; with ML=28 on each side fits in 841.89
    CX = new float[CW.length];
    CX[0] = ML;
    for (int i = 1; i < CW.length; i++) CX[i] = CX[i - 1] + CW[i - 1];
  }

  // ── Colors (RGB, 0–1) ─────────────────────────────────────────────────────

  // Header / chapter / totals row background — steel blue
  private static final float[] C_HEADER = {0.18f, 0.44f, 0.71f};
  // Chapter merged cell — slightly lighter blue
  private static final float[] C_CHAP = {0.26f, 0.56f, 0.84f};
  // White (even data rows)
  private static final float[] C_WHITE = {1f, 1f, 1f};
  // Alternating light blue (odd rows)
  private static final float[] C_ALT = {0.93f, 0.96f, 1.0f};
  // Cell border colour
  private static final float[] C_BORDER = {0.65f, 0.65f, 0.65f};

  // ── Font sizes ────────────────────────────────────────────────────────────

  private static final float FS_TITLE = 13f;
  private static final float FS_SUBTITLE = 8.5f;
  private static final float FS_HDR = 8f;
  private static final float FS_DATA = 7.5f;
  private static final float FS_TOTL = 8.5f;

  // ─────────────────────────────────────────────────────────────────────────
  // Public API
  // ─────────────────────────────────────────────────────────────────────────

  /** Fetch matrix data and return the rendered PDF as raw bytes. */
  public byte[] exportToPdf(UUID matrixId) {
    ExamMatrixTableResponse matrix = examMatrixService.getMatrixTable(matrixId);
    try {
      return buildPdf(matrix);
    } catch (IOException e) {
      throw new RuntimeException("PDF export failed: " + e.getMessage(), e);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // PDF building
  // ─────────────────────────────────────────────────────────────────────────

  private byte[] buildPdf(ExamMatrixTableResponse m) throws IOException {
    try (PDDocument doc = new PDDocument()) {
      PDFont regular = loadFont(doc, false);
      PDFont bold = loadFont(doc, true);

      List<MatrixChapterGroupResponse> allChaps =
          m.getChapters() != null ? m.getChapters() : List.of();

      // Usable vertical space for data rows (first page has title on top)
      float usable = PH - MT - MB - TITLE_H - HDR1_H - HDR2_H - TOTL_H;
      List<List<MatrixChapterGroupResponse>> pages = paginate(allChaps, usable);

      for (int pi = 0; pi < pages.size(); pi++) {
        renderPage(
            doc, m, pages.get(pi), regular, bold, pi == 0, pi == pages.size() - 1);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.save(out);
      return out.toByteArray();
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Page rendering
  // ─────────────────────────────────────────────────────────────────────────

  private void renderPage(
      PDDocument doc,
      ExamMatrixTableResponse m,
      List<MatrixChapterGroupResponse> pageChaps,
      PDFont regular,
      PDFont bold,
      boolean isFirst,
      boolean isLast)
      throws IOException {

    PDPage page = new PDPage(new PDRectangle(PW, PH));
    doc.addPage(page);

    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
      float y = PH - MT;

      // Title block (first page only)
      if (isFirst) {
        y = drawTitle(cs, m, bold, regular, y);
      }

      // Two-row table header
      y = drawHeaders(cs, bold, y);

      // ── Data block ──────────────────────────────────────────────────────
      int totalRows = pageChaps.stream().mapToInt(c -> c.getRows().size()).sum();
      float blockH = totalRows * ROW_H;
      float blockTop = y;
      float blockBot = y - blockH;

      // Lớp (grade) — single merged cell spanning all rows on this page
      fillBox(cs, CX[0], blockBot, CW[0], blockH, C_WHITE);
      strokeBox(cs, CX[0], blockBot, CW[0], blockH);
      drawInCell(
          cs,
          bold,
          FS_DATA + 1f,
          CX[0],
          blockBot,
          CW[0],
          blockH,
          List.of(m.getGradeLevel() != null ? String.valueOf(m.getGradeLevel()) : ""),
          true,
          C_WHITE);

      // ── Chapter groups ───────────────────────────────────────────────────
      float chapY = blockTop;
      for (MatrixChapterGroupResponse chap : pageChaps) {
        int nRows = chap.getRows().size();
        float chapH = nRows * ROW_H;
        float chapBot = chapY - chapH;

        // Draw data cells for each row (columns 2–8)
        for (int ri = 0; ri < nRows; ri++) {
          float[] bg = (ri % 2 == 0) ? C_WHITE : C_ALT;
          drawDataRow(cs, regular, chap.getRows().get(ri), chapY - ri * ROW_H, bg);
        }

        // Chương — merged cell spanning all rows of this chapter (col 1)
        fillBox(cs, CX[1], chapBot, CW[1], chapH, C_CHAP);
        strokeBox(cs, CX[1], chapBot, CW[1], chapH);
        drawInCell(
            cs,
            bold,
            FS_DATA,
            CX[1],
            chapBot,
            CW[1],
            chapH,
            wrapText(bold, FS_DATA, safe(chap.getChapterTitle()), CW[1] - 6),
            true,
            C_CHAP);

        // Tổng chương — merged cell (col 9)
        fillBox(cs, CX[9], chapBot, CW[9], chapH, C_WHITE);
        strokeBox(cs, CX[9], chapBot, CW[9], chapH);
        drawInCell(
            cs,
            bold,
            FS_DATA,
            CX[9],
            chapBot,
            CW[9],
            chapH,
            List.of(String.valueOf(chap.getChapterTotalQuestions())),
            true,
            C_WHITE);

        chapY -= chapH;
      }

      // Grand-totals row (last page only)
      if (isLast) {
        drawTotalsRow(cs, m, bold, chapY);
      }
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Section helpers
  // ─────────────────────────────────────────────────────────────────────────

  /** Title + subtitle above the table. Returns new Y position below the block. */
  private float drawTitle(
      PDPageContentStream cs, ExamMatrixTableResponse m, PDFont bold, PDFont plain, float y)
      throws IOException {
    String title = m.getName() != null ? m.getName() : "MA TRẬN ĐỀ THI";
    String subtitle = buildSubtitle(m);

    y -= FS_TITLE + 2f;
    drawCenteredLine(cs, bold, FS_TITLE, y, title);

    if (!subtitle.isEmpty()) {
      y -= FS_SUBTITLE + 3f;
      drawCenteredLine(cs, plain, FS_SUBTITLE, y, subtitle);
    }
    return y - 6f;
  }

  private String buildSubtitle(ExamMatrixTableResponse m) {
    StringBuilder sb = new StringBuilder();
    if (m.getSubjectName() != null) sb.append("Môn: ").append(m.getSubjectName());
    if (m.getGradeLevel() != null) {
      if (sb.length() > 0) sb.append("  |  ");
      sb.append("Lớp: ").append(m.getGradeLevel());
    }
    if (m.getCurriculumName() != null) {
      if (sb.length() > 0) sb.append("  |  ");
      sb.append(m.getCurriculumName());
    }
    return sb.toString();
  }

  /** Two-row table header. Returns Y below the header. */
  private float drawHeaders(PDPageContentStream cs, PDFont bold, float y) throws IOException {
    float totalHeaderH = HDR1_H + HDR2_H;
    float h2bot = y - totalHeaderH;
    float h1bot = y - HDR1_H; // = top of sub-row

    // Fill entire header area
    fillBox(cs, CX[0], h2bot, TW, totalHeaderH, C_HEADER);

    // ── Tall cells (span both rows): cols 0, 1, 2, 3, 8, 9 ─────────────
    int[] tallCols = {0, 1, 2, 3, 8, 9};
    String[] tallLabels = {
      "Lớp", "Chương", "Dạng bài", "Trích dẫn đề\nMinh Họa", "Tổng\nDạng bài", "Tổng\nChương"
    };
    for (int i = 0; i < tallCols.length; i++) {
      int c = tallCols[i];
      strokeBox(cs, CX[c], h2bot, CW[c], totalHeaderH);
      drawInCell(
          cs,
          bold,
          FS_HDR,
          CX[c],
          h2bot,
          CW[c],
          totalHeaderH,
          Arrays.asList(tallLabels[i].split("\n")),
          true,
          C_HEADER);
    }

    // ── "MỨC ĐỘ" spans cols 4–7 in the top header row ─────────────────
    float mucDoW = CW[4] + CW[5] + CW[6] + CW[7];
    strokeBox(cs, CX[4], h1bot, mucDoW, HDR1_H);
    drawInCell(cs, bold, FS_HDR, CX[4], h1bot, mucDoW, HDR1_H, List.of("MỨC ĐỘ"), true, C_HEADER);

    // ── NB / TH / VD / VDC in the bottom header row ─────────────────────
    String[] subLabels = {"NB", "TH", "VD", "VDC"};
    for (int li = 0; li < 4; li++) {
      int c = 4 + li;
      strokeBox(cs, CX[c], h2bot, CW[c], HDR2_H);
      drawInCell(cs, bold, FS_HDR, CX[c], h2bot, CW[c], HDR2_H, List.of(subLabels[li]), true, C_HEADER);
    }

    return h2bot;
  }

  /** One data row — columns 2 through 8. */
  private void drawDataRow(
      PDPageContentStream cs, PDFont regular, MatrixRowResponse row, float rowTop, float[] bg)
      throws IOException {
    float rowBot = rowTop - ROW_H;

    // Col 2 — Dạng bài (left-aligned, may wrap)
    fillBox(cs, CX[2], rowBot, CW[2], ROW_H, bg);
    strokeBox(cs, CX[2], rowBot, CW[2], ROW_H);
    drawInCell(
        cs,
        regular,
        FS_DATA,
        CX[2],
        rowBot,
        CW[2],
        ROW_H,
        wrapText(regular, FS_DATA, safe(row.getQuestionTypeName()), CW[2] - 5),
        false,
        bg);

    // Col 3 — Trích dẫn (centred)
    fillBox(cs, CX[3], rowBot, CW[3], ROW_H, bg);
    strokeBox(cs, CX[3], rowBot, CW[3], ROW_H);
    drawInCell(
        cs,
        regular,
        FS_DATA,
        CX[3],
        rowBot,
        CW[3],
        ROW_H,
        List.of(safe(row.getReferenceQuestions())),
        true,
        bg);

    // Cols 4–7 — NB / TH / VD / VDC (centred)
    Map<String, Integer> cnts =
        row.getCountByCognitive() != null ? row.getCountByCognitive() : Map.of();
    String[] levels = {"NB", "TH", "VD", "VDC"};
    for (int li = 0; li < 4; li++) {
      int c = 4 + li;
      Integer count = cnts.get(levels[li]);
      String val = (count != null && count > 0) ? String.valueOf(count) : "";
      fillBox(cs, CX[c], rowBot, CW[c], ROW_H, bg);
      strokeBox(cs, CX[c], rowBot, CW[c], ROW_H);
      drawInCell(cs, regular, FS_DATA, CX[c], rowBot, CW[c], ROW_H, List.of(val), true, bg);
    }

    // Col 8 — Tổng dạng bài (centred)
    fillBox(cs, CX[8], rowBot, CW[8], ROW_H, bg);
    strokeBox(cs, CX[8], rowBot, CW[8], ROW_H);
    drawInCell(
        cs,
        regular,
        FS_DATA,
        CX[8],
        rowBot,
        CW[8],
        ROW_H,
        List.of(String.valueOf(row.getRowTotalQuestions())),
        true,
        bg);
  }

  /** Grand-totals row at the very bottom. */
  private void drawTotalsRow(
      PDPageContentStream cs, ExamMatrixTableResponse m, PDFont bold, float y) throws IOException {
    float rowBot = y - TOTL_H;

    // "Tổng" spanning cols 0–3
    float spanW = CW[0] + CW[1] + CW[2] + CW[3];
    fillBox(cs, CX[0], rowBot, spanW, TOTL_H, C_HEADER);
    strokeBox(cs, CX[0], rowBot, spanW, TOTL_H);
    drawInCell(cs, bold, FS_TOTL, CX[0], rowBot, spanW, TOTL_H, List.of("Tổng"), true, C_HEADER);

    // NB / TH / VD / VDC totals
    Map<String, Integer> totals =
        m.getGrandTotalByCognitive() != null ? m.getGrandTotalByCognitive() : Map.of();
    String[] levels = {"NB", "TH", "VD", "VDC"};
    for (int li = 0; li < 4; li++) {
      int c = 4 + li;
      Integer cnt = totals.get(levels[li]);
      String val = cnt != null ? String.valueOf(cnt) : "";
      fillBox(cs, CX[c], rowBot, CW[c], TOTL_H, C_HEADER);
      strokeBox(cs, CX[c], rowBot, CW[c], TOTL_H);
      drawInCell(cs, bold, FS_TOTL, CX[c], rowBot, CW[c], TOTL_H, List.of(val), true, C_HEADER);
    }

    // Grand total (col 8)
    fillBox(cs, CX[8], rowBot, CW[8], TOTL_H, C_HEADER);
    strokeBox(cs, CX[8], rowBot, CW[8], TOTL_H);
    drawInCell(
        cs,
        bold,
        FS_TOTL,
        CX[8],
        rowBot,
        CW[8],
        TOTL_H,
        List.of(String.valueOf(m.getGrandTotalQuestions())),
        true,
        C_HEADER);

    // Col 9 — fill with header colour (blank)
    fillBox(cs, CX[9], rowBot, CW[9], TOTL_H, C_HEADER);
    strokeBox(cs, CX[9], rowBot, CW[9], TOTL_H);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Drawing primitives
  // ─────────────────────────────────────────────────────────────────────────

  private void fillBox(PDPageContentStream cs, float x, float y, float w, float h, float[] rgb)
      throws IOException {
    cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
    cs.addRect(x, y, w, h);
    cs.fill();
  }

  private void strokeBox(PDPageContentStream cs, float x, float y, float w, float h)
      throws IOException {
    cs.setStrokingColor(C_BORDER[0], C_BORDER[1], C_BORDER[2]);
    cs.setLineWidth(0.4f);
    cs.addRect(x, y, w, h);
    cs.stroke();
  }

  /** Centre a single text line horizontally across a full-width span. */
  private void drawCenteredLine(PDPageContentStream cs, PDFont font, float size, float y, String text)
      throws IOException {
    if (text == null || text.isEmpty()) return;
    cs.setFont(font, size);
    cs.setNonStrokingColor(0f, 0f, 0f);
    float tw = textWidth(font, size, text);
    float x = ML + Math.max(0, (TW - tw) / 2f);
    cs.beginText();
    cs.newLineAtOffset(x, y);
    cs.showText(text);
    cs.endText();
  }

  /**
   * Render a list of text lines inside a cell, centred vertically. Each line is
   * either horizontally centred (centre=true) or left-padded by 3 pt.
   *
   * <p>Text colour is chosen automatically: white on dark backgrounds, dark on light.
   */
  private void drawInCell(
      PDPageContentStream cs,
      PDFont font,
      float size,
      float cellX,
      float cellBot,
      float cellW,
      float cellH,
      List<String> lines,
      boolean centre,
      float[] bgColor)
      throws IOException {
    if (lines == null || lines.isEmpty()) return;

    // Pick text colour based on perceived luminance of background
    boolean darkBg = (bgColor[0] * 0.299f + bgColor[1] * 0.587f + bgColor[2] * 0.114f) < 0.50f;
    float[] tc = darkBg ? new float[] {1f, 1f, 1f} : new float[] {0.08f, 0.08f, 0.08f};

    float lineH = size * 1.35f;
    float totalTextH = lines.size() * lineH;
    // Vertical centre: baseline of the first (top) line
    float startY = cellBot + (cellH + totalTextH) / 2f - size;

    cs.setFont(font, size);
    cs.setNonStrokingColor(tc[0], tc[1], tc[2]);

    for (int i = 0; i < lines.size(); i++) {
      String line = safe(lines.get(i));
      if (line.isEmpty()) continue;

      float lineY = startY - i * lineH;
      // Skip lines outside cell bounds
      if (lineY + size < cellBot || lineY > cellBot + cellH) continue;

      float x;
      if (centre) {
        float tw = textWidth(font, size, line);
        x = cellX + Math.max(2f, (cellW - tw) / 2f);
      } else {
        x = cellX + 3f;
      }

      cs.beginText();
      cs.newLineAtOffset(x, lineY);
      cs.showText(line);
      cs.endText();
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Text utilities
  // ─────────────────────────────────────────────────────────────────────────

  private float textWidth(PDFont font, float size, String text) {
    try {
      return font.getStringWidth(text) / 1000f * size;
    } catch (Exception e) {
      return size * text.length() * 0.5f; // rough fallback
    }
  }

  /** Simple greedy word-wrap. Returns at least one element. */
  private List<String> wrapText(PDFont font, float size, String text, float maxW) {
    List<String> result = new ArrayList<>();
    if (text == null || text.isEmpty()) {
      result.add("");
      return result;
    }
    String[] words = text.split(" ");
    StringBuilder cur = new StringBuilder();
    for (String word : words) {
      String candidate = cur.isEmpty() ? word : cur + " " + word;
      if (textWidth(font, size, candidate) <= maxW) {
        cur = new StringBuilder(candidate);
      } else {
        if (cur.length() > 0) result.add(cur.toString());
        cur = new StringBuilder(word);
      }
    }
    if (cur.length() > 0) result.add(cur.toString());
    if (result.isEmpty()) result.add(text);
    return result;
  }

  private String safe(String s) {
    return s != null ? s : "";
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Pagination — no chapter is split across pages
  // ─────────────────────────────────────────────────────────────────────────

  private List<List<MatrixChapterGroupResponse>> paginate(
      List<MatrixChapterGroupResponse> chaps, float usableH) {
    List<List<MatrixChapterGroupResponse>> pages = new ArrayList<>();
    List<MatrixChapterGroupResponse> current = new ArrayList<>();
    float used = 0f;

    for (MatrixChapterGroupResponse chap : chaps) {
      float need = chap.getRows().size() * ROW_H;
      if (!current.isEmpty() && used + need > usableH) {
        pages.add(current);
        current = new ArrayList<>();
        used = 0f;
      }
      current.add(chap);
      used += need;
    }
    if (!current.isEmpty()) pages.add(current);
    if (pages.isEmpty()) pages.add(new ArrayList<>());
    return pages;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Font loading — tries several system paths, falls back to Helvetica
  // ─────────────────────────────────────────────────────────────────────────

  private PDFont loadFont(PDDocument doc, boolean bold) throws IOException {
    String[][] candidatePaths =
        bold
            ? new String[][] {
              // Windows
              {"C:/Windows/Fonts/arialbd.ttf"},
              {"C:/Windows/Fonts/calibrib.ttf"},
              // Ubuntu / Debian
              {"/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"},
              {"/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"},
              // Alpine (ttf-freefont package)
              {"/usr/share/fonts/TTF/FreeSansBold.ttf"},
              {"/usr/share/fonts/freefont/FreeSansBold.ttf"},
              // Noto
              {"/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"},
            }
            : new String[][] {
              // Windows
              {"C:/Windows/Fonts/arial.ttf"},
              {"C:/Windows/Fonts/calibri.ttf"},
              // Ubuntu / Debian
              {"/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"},
              {"/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"},
              // Alpine (ttf-freefont package)
              {"/usr/share/fonts/TTF/FreeSans.ttf"},
              {"/usr/share/fonts/freefont/FreeSans.ttf"},
              // Noto
              {"/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"},
            };

    for (String[] entry : candidatePaths) {
      File f = new File(entry[0]);
      if (f.exists() && f.canRead()) {
        try (FileInputStream fis = new FileInputStream(f)) {
          return PDType0Font.load(doc, fis, true);
        } catch (IOException e) {
          log.warn("Could not load font from {}: {}", entry[0], e.getMessage());
        }
      }
    }

    log.warn(
        "No system font found that supports Vietnamese. "
            + "On Alpine Docker run: apk add --no-cache ttf-freefont. "
            + "Falling back to built-in Helvetica (Vietnamese characters will not render).");
    Standard14Fonts.FontName fn =
        bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA;
    return new PDType1Font(fn);
  }
}
