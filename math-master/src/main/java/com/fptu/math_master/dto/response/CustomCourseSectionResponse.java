package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomCourseSectionResponse {
  private UUID id;
  private UUID courseId;
  private String title;
  private String description;
  private Integer orderIndex;
  /** Number of non-deleted lessons in this section. */
  private int lessonCount;
  private Instant createdAt;
  private Instant updatedAt;
}
