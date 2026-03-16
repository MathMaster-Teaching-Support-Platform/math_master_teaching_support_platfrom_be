package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fptu.math_master.enums.RoadmapStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Summary response for a learning roadmap (used in list views)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoadmapSummaryResponse {

  @Schema(description = "Roadmap ID")
  private UUID id;

  @Schema(description = "Student ID")
  private UUID studentId;

  @Schema(description = "Student full name")
  private String studentName;

  @Schema(description = "Subject name")
  private String subject;

  @Schema(description = "Grade level")
  private String gradeLevel;

  @Schema(description = "Current status")
  private RoadmapStatus status;

  @Schema(description = "Overall progress percentage")
  private BigDecimal progressPercentage;

  @Schema(description = "Number of completed topics")
  private Integer completedTopicsCount;

  @Schema(description = "Total topics in roadmap")
  private Integer totalTopicsCount;

  @Schema(description = "When roadmap was created")
  private Instant createdAt;

  @Schema(description = "When roadmap was last updated")
  private Instant updatedAt;
}
