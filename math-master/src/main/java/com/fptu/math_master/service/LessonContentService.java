package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.GenerateLessonContentRequest;
import com.fptu.math_master.dto.response.ChapterResponse;
import com.fptu.math_master.dto.response.GenerateLessonContentResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import java.util.List;
import java.util.UUID;

/**
 * Service for AI-powered generation and retrieval of Math Lesson & Chapter content.
 *
 * <p>Flow: caller provides gradeLevel + subject → Gemini generates a structured curriculum →
 * service persists Lessons & Chapters to DB → returns summary.
 */
public interface LessonContentService {

  /**
   * Use Gemini to auto-generate a full lesson curriculum (lessons + chapters) for the given math
   * subject and grade level, then persist it to the database.
   *
   * @param request generation parameters
   * @return summary of what was created
   */
  GenerateLessonContentResponse generateAndSaveContent(GenerateLessonContentRequest request);

  /** List all lessons for a given gradeLevel + subject. */
  List<LessonResponse> getLessonsByGradeAndSubject(String gradeLevel, String subject);

  /** Get a single lesson with its chapters. */
  LessonResponse getLessonById(UUID lessonId);

  /** List chapters of a lesson. */
  List<ChapterResponse> getChaptersByLessonId(UUID lessonId);

  /** Delete a lesson and all its chapters (soft-delete). */
  void deleteLesson(UUID lessonId);

  /** Delete a single chapter (soft-delete). */
  void deleteChapter(UUID chapterId);
}
