package com.fptu.math_master.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Course info embedded in a roadmap topic response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapTopicCourseResponse {

  private UUID id;

  /** Course title */
  private String title;

  /** Course thumbnail URL (may be null if not set) */
  private String thumbnail;

  /** Total number of active lessons in this course */
  private Integer totalLessons;

  /**
   * Whether the current student is enrolled.
   * Always {@code false} from the backend; the FE overlays real-time enrollment state.
   */
  private Boolean isEnrolled;

  /** Number of completed lessons */
  private Integer completedLessons;

  /** Progress percentage (0.0 to 100.0) */
  private Double progress;
}
