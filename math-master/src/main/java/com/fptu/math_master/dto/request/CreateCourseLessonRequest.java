package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseLessonRequest {

  /**
   * FK to Ministry Lesson. Required when adding a lesson to a MINISTRY course.
   * Leave null for CUSTOM courses.
   */
  private UUID lessonId;

  /**
   * FK to CustomCourseSection. Required when adding a lesson to a CUSTOM course.
   * Leave null for MINISTRY courses.
   */
  private UUID sectionId;

  /** Teacher-defined title. Required for CUSTOM course lessons. */
  private String customTitle;

  /** Optional description for CUSTOM course lessons. */
  private String customDescription;

  private String videoTitle;

  private Integer orderIndex;

  @Builder.Default
  private boolean isFreePreview = false;

  /** JSON string of List<MaterialItem>: [{type, title, url}] */
  private String materials;
}
