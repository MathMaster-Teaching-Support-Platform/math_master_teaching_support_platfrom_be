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
public class CourseLessonResponse {
  private UUID id;
  private UUID courseId;
  /** FK to Ministry Lesson. Null for CUSTOM-course lessons. */
  private UUID lessonId;
  /** FK to CustomCourseSection. Null for MINISTRY-course lessons. */
  private UUID sectionId;
  /**
   * Lesson display title.
   * Populated from {@code Lesson.title} for MINISTRY courses
   * or from {@code CourseLesson.customTitle} for CUSTOM courses.
   */
  private String lessonTitle;
  /** Teacher-defined description (CUSTOM courses only). */
  private String customDescription;
  private String videoUrl;
  private String videoTitle;
  private Integer durationSeconds;
  private Integer orderIndex;
  private boolean isFreePreview;
  /** Raw JSON string of List<MaterialItem> */
  private String materials;
  /** MINISTRY courses: chapter info for grouping */
  private UUID chapterId;
  private String chapterTitle;
  private Instant createdAt;
  private Instant updatedAt;
}
