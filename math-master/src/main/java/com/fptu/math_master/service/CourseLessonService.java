package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateCourseLessonRequest;
import com.fptu.math_master.dto.request.UpdateCourseLessonRequest;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface CourseLessonService {

  CourseLessonResponse addLesson(UUID courseId, CreateCourseLessonRequest request, MultipartFile videoFile);

  CourseLessonResponse updateLesson(UUID courseId, UUID lessonId, UpdateCourseLessonRequest request, MultipartFile videoFile);

  void deleteLesson(UUID courseId, UUID lessonId);

  List<CourseLessonResponse> getLessons(UUID courseId);

  void reorderLessons(UUID courseId, com.fptu.math_master.dto.request.ReorderLessonsRequest request);

  CourseLessonResponse addMaterial(UUID courseId, UUID lessonId, MultipartFile file);

  CourseLessonResponse removeMaterial(UUID courseId, UUID lessonId, String materialId);

  /** Returns a presigned download URL for a material file. Accessible to enrolled students, teacher owner, and admin. */
  String getMaterialDownloadUrl(UUID courseId, UUID lessonId, String materialId);
}
