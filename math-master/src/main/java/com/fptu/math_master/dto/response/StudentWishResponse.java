package com.fptu.math_master.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response containing student learning wishes and preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentWishResponse {

  @Schema(description = "Unique identifier for the wish")
  private UUID id;

  @Schema(description = "Student ID")
  private UUID studentId;

  @Schema(description = "Subject")
  private String subject;

  @Schema(description = "Grade level")
  private String gradeLevel;

  @Schema(description = "Learning goals")
  private String learningGoals;

  @Schema(description = "Preferred topics")
  private String preferredTopics;

  @Schema(description = "Weak areas to improve")
  private String weakAreasToImprove;

  @Schema(description = "Daily study minutes")
  private Integer dailyStudyMinutes;

  @Schema(description = "Target accuracy percentage")
  private Integer targetAccuracyPercentage;

  @Schema(description = "Learning style preference")
  private String learningStylePreference;

  @Schema(description = "Whether student prefers difficult challenges")
  private Boolean preferDifficultChallenges;

  @Schema(description = "Whether this wish is active")
  private Boolean isActive;

  @Schema(description = "Created timestamp")
  private Instant createdAt;

  @Schema(description = "Last updated timestamp")
  private Instant updatedAt;
}
