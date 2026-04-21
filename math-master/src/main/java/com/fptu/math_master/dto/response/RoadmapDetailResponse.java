package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response containing detailed learning roadmap information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoadmapDetailResponse {

  @Schema(description = "Roadmap ID")
  private UUID id;

  @Schema(description = "Roadmap display name (e.g., 'Toán học lớp 6 cho người mới bắt đầu')")
  private String name;

  @Schema(description = "Student ID")
  private UUID studentId;

  @Schema(description = "Teacher ID (if assigned by teacher)")
  private UUID teacherId;

  @Schema(description = "Subject ID")
  private UUID subjectId;

  @Schema(description = "Subject name")
  private String subject;

  @Schema(description = "Grade level")
  private String gradeLevel;

  @Schema(description = "Type of generation")
  private RoadmapGenerationType generationType;

  @Schema(description = "Current status")
  private RoadmapStatus status;

  @Schema(description = "Overall progress percentage (0-100)")
  private BigDecimal progressPercentage;

  @Schema(description = "Number of completed topics")
  private Integer completedTopicsCount;

  @Schema(description = "Total number of topics")
  private Integer totalTopicsCount;

  @Schema(description = "Estimated days to complete")
  private Integer estimatedCompletionDays;

  @Schema(description = "Description of the roadmap")
  private String description;

  @Schema(description = "When roadmap was started")
  private Instant startedAt;

  @Schema(description = "When roadmap was completed")
  private Instant completedAt;

  @Schema(description = "When roadmap was created")
  private Instant createdAt;

  @Schema(description = "When roadmap was last updated")
  private Instant updatedAt;

  @Schema(description = "Topics in this roadmap")
  private List<RoadmapTopicResponse> topics;

  @Schema(description = "Quick stats")
  private RoadmapStatsResponse stats;

  @Schema(description = "Entry test information if available")
  private RoadmapEntryTestInfo entryTest;

  @Schema(description = "Student best score across all submitted tests (10-point scale)")
  private Integer studentBestScore;
}
