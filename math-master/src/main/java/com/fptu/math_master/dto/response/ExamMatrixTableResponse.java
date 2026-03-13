package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.MatrixStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.*;

/**
 * Full hierarchical view of an exam matrix (ma trận đề thi) — mirrors the
 * tabular format teachers see in practice:
 * <pre>
 * Lớp | Môn | Chương | Dạng bài | Trích dẫn | NB | TH | VD | VDC | Tổng
 * ────────────────────────────────────────────────────────────────────────
 * 12  | … | Đạo hàm | Đơn điệu |  3,30     |  1 |  1 |    |     |  2
 *        │           | Cực trị  |  4,5,…    |  1 |  1 |  1 |  1  |  4
 *        │ Chương total                     |  … |  … |  … |  …  |  10
 *        ┆
 * Tổng                                      | 20 | 15 | 10 |  5  |  50
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixTableResponse {

  private UUID id;
  private String name;
  private String description;

  private UUID teacherId;
  private String teacherName;

  /** Primary grade level (lớp) this matrix is designed for. */
  private Integer gradeLevel;

  /** Curriculum / chương trình the matrix is based on. */
  private UUID curriculumId;
  private String curriculumName;

  /** Subject (môn) — resolved from the curriculum. */
  private UUID subjectId;
  private String subjectName;

  private Boolean isReusable;
  private MatrixStatus status;

  /** Chapters, each containing their dạng-bài rows. */
  private List<MatrixChapterGroupResponse> chapters;

  /**
   * Grand-total question count per cognitive-level label (NB/TH/VD/VDC).
   * Matches the bottom "Tổng" row in the matrix image.
   */
  private Map<String, Integer> grandTotalByCognitive;

  private int grandTotalQuestions;
  private BigDecimal grandTotalPoints;

  private Integer totalQuestionsTarget;
  private BigDecimal totalPointsTarget;

  private Instant createdAt;
  private Instant updatedAt;
}
