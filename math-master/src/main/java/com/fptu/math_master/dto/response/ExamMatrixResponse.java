package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.MatrixStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixResponse {

  private UUID id;
  private UUID assessmentId;
  private String assessmentTitle;
  private UUID lessonId;
  private String lessonTitle;
  private UUID teacherId;
  private String teacherName;
  private String name;
  private String description;
  private Integer totalQuestions;
  private BigDecimal totalPoints;
  private Integer timeLimitMinutes;
  private MatrixStatus status;
  private Integer cellCount;
  private Integer filledCells;
  private Integer selectedQuestions;
  private Instant createdAt;
  private Instant updatedAt;
}

