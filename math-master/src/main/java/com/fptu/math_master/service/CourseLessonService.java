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

  /**
   * Get video URL for admin review (bypasses enrollment check).
   * 
   * @param courseId the course ID
   * @param courseLessonId the lesson ID
   * @return presigned video URL
   */
  String getAdminVideoUrl(UUID courseId, UUID courseLessonId);

  CourseLessonResponse addMaterial(UUID courseId, UUID lessonId, MultipartFile file);

  CourseLessonResponse removeMaterial(UUID courseId, UUID lessonId, String materialId);

  /** Returns a presigned download URL for a material file. Accessible to enrolled students, teacher owner, and admin. */
  String getMaterialDownloadUrl(UUID courseId, UUID lessonId, String materialId);

  /** Fetches the raw bytes of a material file from MinIO for direct streaming to the client. */
  record MaterialDownloadResult(byte[] content, String contentType, String fileName) {}
  MaterialDownloadResult downloadMaterial(UUID courseId, UUID lessonId, String materialId);

  /**
   * Bundles every material attached to a lesson into a single zip stream. Same
   * access policy as single-material download: admin / course owner /
   * non-owner teacher (for published courses) / ACTIVE-enrolled student.
   *
   * @return the zip bytes plus a suggested filename
   */
  MaterialDownloadResult downloadAllMaterials(UUID courseId, UUID lessonId);
}
