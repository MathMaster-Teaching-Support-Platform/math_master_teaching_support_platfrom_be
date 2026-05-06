package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hierarchical view of a question bank for the "happy-case" creation flow:
 *
 * <pre>
 * Lớp 9
 *   ├── Chương I
 *   │     ├── NB → [questions...]
 *   │     ├── TH → [questions...]
 *   │     ├── VD → [questions...]
 *   │     └── VDC → [questions...]
 *   ├── Chương II
 *   ...
 * </pre>
 *
 * Buckets are keyed by {@link CognitiveLevel} name (NHAN_BIET / THONG_HIEU /
 * VAN_DUNG / VAN_DUNG_CAO). Every chapter is always returned with all four
 * buckets — empty buckets simply have {@code count = 0} and an empty list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankTreeResponse {

  private UUID bankId;
  private String bankName;

  private UUID schoolGradeId;
  private Integer gradeLevel;
  private String schoolGradeName;

  private UUID subjectId;
  private String subjectName;

  private List<ChapterNode> chapters;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChapterNode {
    private UUID chapterId;
    private String title;
    private Integer orderIndex;
    private Long totalQuestions;
    /** Always contains all 4 Vietnamese cognitive levels. */
    private Map<String, CognitiveBucket> buckets;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CognitiveBucket {
    private CognitiveLevel level;
    private long count;
    private List<QuestionSummary> questions;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QuestionSummary {
    private UUID id;
    private String questionText;
    private String questionType;
    private String questionStatus;
  }
}
