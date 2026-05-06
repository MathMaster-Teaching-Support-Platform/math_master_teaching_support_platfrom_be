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
  private Long questionCount;
  /** Counts per CognitiveLevel enum name, e.g. {"NHAN_BIET": 6, "THONG_HIEU": 4} */
  private Map<String, Long> cognitiveStats;

  /** School grade (lớp) this bank serves; null for legacy banks. */
  private UUID schoolGradeId;

  private Integer gradeLevel;
  private String schoolGradeName;

  /** Optional subject scope; null = covers all subjects for the grade. */
  private UUID subjectId;
  private String subjectName;

  private Instant createdAt;
  private Instant updatedAt;
}
