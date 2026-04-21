package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankResponse {

  private UUID id;
  private UUID teacherId;
  private String teacherName;
  private String name;
  private String description;
  private Boolean isPublic;
  private UUID chapterId;
  private String chapterTitle;
  private Long questionCount;
  /** Counts per CognitiveLevel enum name, e.g. {"NHAN_BIET": 6, "THONG_HIEU": 4} */
  private Map<String, Long> cognitiveStats;
  private Instant createdAt;
  private Instant updatedAt;
}
