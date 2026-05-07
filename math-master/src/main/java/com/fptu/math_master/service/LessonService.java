package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateLessonRequest;
import com.fptu.math_master.dto.request.ReorderLessonsRequest;
import com.fptu.math_master.dto.request.UpdateLessonRequest;
import com.fptu.math_master.dto.response.LessonResponse;
import java.util.List;
import java.util.UUID;

public interface LessonService {

  LessonResponse createLesson(CreateLessonRequest request);

  LessonResponse getLessonById(UUID id);

  List<LessonResponse> getLessonsByChapterId(UUID chapterId);

  List<LessonResponse> searchLessonsByChapterId(UUID chapterId, String name);

  LessonResponse updateLesson(UUID id, UpdateLessonRequest request);

  void deleteLesson(UUID id);

  LessonResponse restoreLesson(UUID id);

  List<LessonResponse> getLessonsIncludingDeleted(UUID chapterId);

  List<LessonResponse> reorderLessons(UUID chapterId, ReorderLessonsRequest request);
}
