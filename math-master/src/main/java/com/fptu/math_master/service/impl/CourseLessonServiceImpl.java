package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.request.CreateCourseLessonRequest;
import com.fptu.math_master.dto.request.UpdateCourseLessonRequest;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.dto.response.MaterialItem;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.entity.CustomCourseSection;
import com.fptu.math_master.enums.CourseProvider;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CustomCourseSectionRepository;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.service.CourseLessonService;
import com.fptu.math_master.service.CourseService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class CourseLessonServiceImpl implements CourseLessonService {

  CourseLessonRepository courseLessonRepository;
  CourseRepository courseRepository;
  LessonRepository lessonRepository;
  CustomCourseSectionRepository customCourseSectionRepository;
  com.fptu.math_master.repository.EnrollmentRepository enrollmentRepository;
  UploadService uploadService;
  ObjectMapper objectMapper;
  MinioProperties minioProperties;
  CourseService courseService;

  @Override
  public CourseLessonResponse addLesson(
      UUID courseId, CreateCourseLessonRequest request, MultipartFile videoFile) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    String videoUrl = null;
    if (videoFile != null && !videoFile.isEmpty()) {
      videoUrl = uploadService.uploadFile(videoFile, "course-videos", minioProperties.getCourseVideosBucket());
    }

    CourseLesson courseLesson;

    if (course.getProvider() == CourseProvider.MINISTRY) {
      // ── MINISTRY: must reference a Ministry Lesson ──────────────────────────
      if (request.getLessonId() == null) {
        throw new AppException(ErrorCode.LESSON_ID_REQUIRED_FOR_MINISTRY_COURSE);
      }
      var lesson =
          lessonRepository
              .findByIdAndNotDeleted(request.getLessonId())
              .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

      courseLesson =
          CourseLesson.builder()
              .courseId(courseId)
              .lessonId(request.getLessonId())
              .videoUrl(videoUrl)
              .videoTitle(request.getVideoTitle())
              .orderIndex(request.getOrderIndex())
              .isFreePreview(request.isFreePreview())
              .materials(request.getMaterials())
              .build();

      courseLesson = courseLessonRepository.save(courseLesson);
      log.info("CourseLesson added: {} to course: {}", courseLesson.getId(), courseId);
      
      courseService.syncCourseMetrics(courseId);
      
      return mapToResponse(courseLesson, lesson.getTitle(), true);

    } else {
      // ── CUSTOM: teacher-defined lesson ──────────────────────────────────────
      if (request.getSectionId() == null) {
        throw new AppException(ErrorCode.SECTION_REQUIRED_FOR_CUSTOM_COURSE);
      }
      if (!org.springframework.util.StringUtils.hasText(request.getCustomTitle())) {
        throw new AppException(ErrorCode.CUSTOM_TITLE_REQUIRED);
      }

      CustomCourseSection section =
          customCourseSectionRepository
              .findByIdAndDeletedAtIsNull(request.getSectionId())
              .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_COURSE_SECTION_NOT_FOUND));

      if (!section.getCourseId().equals(courseId)) {
        throw new AppException(ErrorCode.SECTION_NOT_IN_COURSE);
      }

      courseLesson =
          CourseLesson.builder()
              .courseId(courseId)
              .sectionId(request.getSectionId())
              .customTitle(request.getCustomTitle())
              .customDescription(request.getCustomDescription())
              .videoUrl(videoUrl)
              .videoTitle(request.getVideoTitle())
              .orderIndex(request.getOrderIndex())
              .isFreePreview(request.isFreePreview())
              .materials(request.getMaterials())
              .build();

      courseLesson = courseLessonRepository.save(courseLesson);
      log.info("[CUSTOM] CourseLesson added: {} to course: {}", courseLesson.getId(), courseId);
      
      courseService.syncCourseMetrics(courseId);
      
      return mapToResponse(courseLesson, request.getCustomTitle(), true);
    }
  }

  @Override
  public CourseLessonResponse updateLesson(
      UUID courseId,
      UUID courseLessonId,
      UpdateCourseLessonRequest request,
      MultipartFile videoFile) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CourseLesson courseLesson =
        courseLessonRepository
            .findByIdAndDeletedAtIsNull(courseLessonId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    if (!courseLesson.getCourseId().equals(courseId)) {
      throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
    }

    if (videoFile != null && !videoFile.isEmpty()) {
      if (org.springframework.util.StringUtils.hasText(courseLesson.getVideoUrl())) {
        uploadService.deleteFile(courseLesson.getVideoUrl(), minioProperties.getCourseVideosBucket());
      }
      courseLesson.setVideoUrl(uploadService.uploadFile(videoFile, "course-videos", minioProperties.getCourseVideosBucket()));
    }
    if (request.getVideoTitle() != null) courseLesson.setVideoTitle(request.getVideoTitle());
    if (request.getOrderIndex() != null) courseLesson.setOrderIndex(request.getOrderIndex());
    if (request.getIsFreePreview() != null) courseLesson.setFreePreview(request.getIsFreePreview());
    if (request.getMaterials() != null) courseLesson.setMaterials(request.getMaterials());

    courseLesson = courseLessonRepository.save(courseLesson);

    courseService.syncCourseMetrics(courseId);

    String lessonTitle = courseLesson.getCustomTitle();
    if (courseLesson.getLessonId() != null) {
      lessonTitle =
          lessonRepository
              .findByIdAndNotDeleted(courseLesson.getLessonId())
              .map(l -> l.getTitle())
              .orElse(lessonTitle);
    }

    return mapToResponse(courseLesson, lessonTitle, true);
  }

  @Override
  public void deleteLesson(UUID courseId, UUID courseLessonId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CourseLesson courseLesson =
        courseLessonRepository
            .findByIdAndDeletedAtIsNull(courseLessonId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    if (!courseLesson.getCourseId().equals(courseId)) {
      throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
    }

    // Store the orderIndex of the deleted lesson
    int deletedOrderIndex = courseLesson.getOrderIndex();

    // MinIO Cleanup: Delete video file
    if (org.springframework.util.StringUtils.hasText(courseLesson.getVideoUrl())) {
      uploadService.deleteFile(courseLesson.getVideoUrl(), minioProperties.getCourseVideosBucket());
    }

    // MinIO Cleanup: Delete all material files
    List<MaterialItem> items = getMaterialList(courseLesson.getMaterials());
    for (MaterialItem item : items) {
      if (org.springframework.util.StringUtils.hasText(item.getKey())) {
        uploadService.deleteFile(item.getKey(), minioProperties.getCourseMaterialsBucket());
      }
    }

    courseLesson.setDeletedAt(Instant.now());
    courseLesson.setDeletedBy(currentUserId);
    courseLessonRepository.save(courseLesson);
    log.info("CourseLesson soft-deleted and resources cleaned up: {}", courseLessonId);
    
    // Reorder remaining lessons to fill the gap
    List<CourseLesson> remainingLessons = courseLessonRepository.findByCourseIdAndNotDeleted(courseId);
    remainingLessons.stream()
        .filter(lesson -> {
            if (course.getProvider() == com.fptu.math_master.enums.CourseProvider.CUSTOM) {
                return java.util.Objects.equals(lesson.getSectionId(), courseLesson.getSectionId()) 
                    && lesson.getOrderIndex() > deletedOrderIndex;
            }
            return lesson.getOrderIndex() > deletedOrderIndex;
        })
        .forEach(lesson -> {
          lesson.setOrderIndex(lesson.getOrderIndex() - 1);
          courseLessonRepository.save(lesson);
        });
    
    log.info("Reordered {} lessons after deletion of lesson {}", 
        remainingLessons.stream().filter(l -> l.getOrderIndex() >= deletedOrderIndex).count(), 
        courseLessonId);
    
    courseService.syncCourseMetrics(courseId);
  }

  @Override
  public void reorderLessons(UUID courseId, com.fptu.math_master.dto.request.ReorderLessonsRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    // Validate: Check for duplicate orderIndex values
    java.util.Set<Integer> orderIndexSet = new java.util.HashSet<>();
    for (var order : request.getOrders()) {
      if (!orderIndexSet.add(order.getOrderIndex())) {
        throw new AppException(ErrorCode.DUPLICATE_ORDER_INDEX);
      }
    }

    // Validate: All lessons belong to this course
    for (var order : request.getOrders()) {
      CourseLesson lesson = courseLessonRepository.findByIdAndDeletedAtIsNull(order.getLessonId())
          .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

      if (!lesson.getCourseId().equals(courseId)) {
        throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
      }

      lesson.setOrderIndex(order.getOrderIndex());
      courseLessonRepository.save(lesson);
    }
    log.info("Batch reordered {} lessons for course: {}", request.getOrders().size(), courseId);
  }

  @Override
  @Transactional(readOnly = true)
  public String getAdminVideoUrl(UUID courseId, UUID courseLessonId) {
    UUID currentAdminId = SecurityUtils.getCurrentUserId();
    log.info("Admin {} requesting video URL for lesson {} in course {}", 
        currentAdminId, courseLessonId, courseId);
    
    Course course = findCourseOrThrow(courseId);
    
    CourseLesson courseLesson = courseLessonRepository
        .findByIdAndDeletedAtIsNull(courseLessonId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    if (!courseLesson.getCourseId().equals(courseId)) {
      throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
    }

    if (!org.springframework.util.StringUtils.hasText(courseLesson.getVideoUrl())) {
      throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
    }

    // Generate presigned URL for admin access
    String presignedUrl = uploadService.getPresignedUrl(
        courseLesson.getVideoUrl(),
        minioProperties.getCourseVideosBucket()
    );
    
    log.info("✅ Admin {} granted video access to lesson {}", currentAdminId, courseLessonId);
    return presignedUrl;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseLessonResponse> getLessons(UUID courseId) {
    Course course = findCourseOrThrow(courseId);
    UUID currentUserId = SecurityUtils.getOptionalCurrentUserId();

    boolean isAuthorized = false;
    final String[] authReasonHolder = {"not authenticated"}; // Use array to make it effectively final
    
    if (currentUserId != null) {
      // IMPORTANT: Check admin role FIRST before other checks
      boolean isAdmin = SecurityUtils.hasRole("ADMIN");
      boolean isOwner = course.getTeacherId().equals(currentUserId);
      
      log.info("Course {} lesson access check: userId={}, isAdmin={}, isOwner={}, courseStatus={}", 
          courseId, currentUserId, isAdmin, isOwner, course.getStatus());
      
      if (isAdmin) {
        isAuthorized = true;
        authReasonHolder[0] = "admin role";
        log.info("✅ Admin user {} granted access to course {} lessons", currentUserId, courseId);
      } else if (isOwner) {
        isAuthorized = true;
        authReasonHolder[0] = "course owner";
      } else {
        // Check student enrollment
        isAuthorized =
            enrollmentRepository
                .findByStudentIdAndCourseIdAndDeletedAtIsNull(currentUserId, courseId)
                .map(e -> {
                  boolean active = "ACTIVE".equals(e.getStatus().name());
                  if (active) {
                    authReasonHolder[0] = "enrolled student";
                  }
                  return active;
                })
                .orElse(false);
      }
    } else {
      log.warn("❌ Unauthenticated access attempt to course {} lessons", courseId);
    }
    
    log.info("Course {} lesson access result: userId={}, authorized={}, reason={}", 
        courseId, currentUserId, isAuthorized, authReasonHolder[0]);

    // Udemy-style access: only enrolled/owner/admin can access all lessons.
    // Non-enrolled users can access only lessons explicitly marked as free preview.
    final boolean canAccessAll = isAuthorized;

    return courseLessonRepository.findByCourseIdAndNotDeleted(courseId).stream()
        .map(
            cl -> {
              String lessonTitle = cl.getCustomTitle();
              if (cl.getLessonId() != null) {
                lessonTitle =
                    lessonRepository
                        .findByIdAndNotDeleted(cl.getLessonId())
                        .map(l -> l.getTitle())
                        .orElse(lessonTitle);
              }
              return mapToResponse(cl, lessonTitle, canAccessAll);
            })
        .collect(Collectors.toList());
  }

  private Course findCourseOrThrow(UUID courseId) {
    return courseRepository
        .findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
  }

  private void verifyOwnership(Course course, UUID userId) {
    if (!course.getTeacherId().equals(userId)) {
      throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
    }
  }

  private CourseLessonResponse mapToResponse(
      CourseLesson cl, String lessonTitle, boolean isAuthorized) {
    String materialsJson = cl.getMaterials();
    boolean canAccess = isAuthorized || cl.isFreePreview();

    if (materialsJson != null && !materialsJson.isBlank()) {
      List<MaterialItem> items = getMaterialList(materialsJson);
      if (canAccess) {
        for (MaterialItem item : items) {
          try {
            item.setUrl(
                uploadService.getPresignedUrl(
                    item.getKey(), minioProperties.getCourseMaterialsBucket()));
          } catch (Exception e) {
            // Legacy: file was uploaded to template bucket before course-materials bucket existed
            try {
              item.setUrl(
                  uploadService.getPresignedUrl(
                      item.getKey(), minioProperties.getTemplateBucket()));
            } catch (Exception ex) {
              log.error("Error generating presigned URL for material {}", item.getId(), ex);
            }
          }
          // Never expose the raw object key to clients — they must use the presigned url field.
          item.setKey(null);
        }
      } else {
        // Lock materials for unauthorized/non-preview access
        for (MaterialItem item : items) {
          item.setKey(null);
          item.setUrl(null);
        }
      }
      try {
        materialsJson = objectMapper.writeValueAsString(items);
      } catch (Exception e) {
        log.error("Error re-serializing materials with URLs", e);
      }
    }

    UUID chapterId = null;
    String chapterTitle = null;

    if (cl.getLessonId() != null) {
      var lessonOpt = lessonRepository.findByIdAndNotDeleted(cl.getLessonId());
      if (lessonOpt.isPresent()) {
        chapterId = lessonOpt.get().getChapterId();
        var chapter = lessonOpt.get().getChapter();
        if (chapter != null) {
          chapterTitle = chapter.getTitle();
        }
      }
    }

    return CourseLessonResponse.builder()
        .id(cl.getId())
        .courseId(cl.getCourseId())
        .lessonId(cl.getLessonId())
        .sectionId(cl.getSectionId())
        .chapterId(chapterId)
        .chapterTitle(chapterTitle)
        .lessonTitle(lessonTitle)
        .customDescription(cl.getCustomDescription())
        .videoUrl(canAccess ? cl.getVideoUrl() : null)
        .videoTitle(cl.getVideoTitle())
        .durationSeconds(cl.getDurationSeconds())
        .orderIndex(cl.getOrderIndex())
        .isFreePreview(cl.isFreePreview())
        .materials(materialsJson)
        .createdAt(cl.getCreatedAt())
        .updatedAt(cl.getUpdatedAt())
        .build();
  }

  @Override
  public CourseLessonResponse addMaterial(UUID courseId, UUID lessonId, MultipartFile file) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CourseLesson cl =
        courseLessonRepository
            .findByIdAndDeletedAtIsNull(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    String objectKey = uploadService.uploadFile(
        file, "course-materials", minioProperties.getCourseMaterialsBucket());

    MaterialItem newItem =
        MaterialItem.builder()
            .id(UUID.randomUUID().toString())
            .name(file.getOriginalFilename())
            .key(objectKey)
            .contentType(file.getContentType())
            .size(file.getSize())
            .uploadedAt(Instant.now())
            .build();

    List<MaterialItem> items = getMaterialList(cl.getMaterials());
    items.add(newItem);

    try {
      cl.setMaterials(objectMapper.writeValueAsString(items));
    } catch (Exception e) {
      log.error("Error serializing materials", e);
      throw new RuntimeException("Error saving materials metadata");
    }

    courseLessonRepository.save(cl);
    
    courseService.syncCourseMetrics(courseId);
    
    return mapToResponse(cl, getTitleForLesson(cl), true);
  }

  @Override
  public CourseLessonResponse removeMaterial(UUID courseId, UUID lessonId, String materialId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CourseLesson cl =
        courseLessonRepository
            .findByIdAndDeletedAtIsNull(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    List<MaterialItem> items = getMaterialList(cl.getMaterials());
    MaterialItem toRemove =
        items.stream().filter(i -> i.getId().equals(materialId)).findFirst().orElse(null);

    if (toRemove != null) {
      if (toRemove.getKey() != null && !toRemove.getKey().isBlank()) {
        uploadService.deleteFile(toRemove.getKey(), minioProperties.getCourseMaterialsBucket());
      }
      items.remove(toRemove);
      try {
        cl.setMaterials(objectMapper.writeValueAsString(items));
      } catch (Exception e) {
        log.error("Error serializing materials", e);
        throw new RuntimeException("Error updating materials metadata");
      }
      courseLessonRepository.save(cl);
      
      courseService.syncCourseMetrics(courseId);
    }

    return mapToResponse(cl, getTitleForLesson(cl), true);
  }

  @Override
  public String getMaterialDownloadUrl(UUID courseId, UUID lessonId, String materialId) {
    Course course = findCourseOrThrow(courseId);
    UUID currentUserId = SecurityUtils.getOptionalCurrentUserId();

    // Only the course owner (teacher) or students who have an ACTIVE enrollment may download.
    boolean isOwner = currentUserId != null && course.getTeacherId().equals(currentUserId);
    if (!isOwner) {
      boolean enrolled = currentUserId != null && enrollmentRepository
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(currentUserId, courseId)
          .map(e -> "ACTIVE".equals(e.getStatus().name()))
          .orElse(false);
      if (!enrolled) {
        throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
      }
    }

    CourseLesson cl = courseLessonRepository
        .findByIdAndDeletedAtIsNull(lessonId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    MaterialItem item = getMaterialList(cl.getMaterials()).stream()
        .filter(m -> m.getId().equals(materialId))
        .findFirst()
        .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

    try {
      return uploadService.getPresignedDownloadUrl(
          item.getKey(), minioProperties.getCourseMaterialsBucket(), item.getName());
    } catch (Exception e) {
      // Legacy fallback: file stored in template bucket before course-materials bucket existed
      log.warn("Material not in course-materials bucket, falling back to template bucket for key: {}", item.getKey());
      return uploadService.getPresignedDownloadUrl(
          item.getKey(), minioProperties.getTemplateBucket(), item.getName());
    }
  }

  @Override
  public CourseLessonService.MaterialDownloadResult downloadMaterial(
      UUID courseId, UUID lessonId, String materialId) {
    Course course = findCourseOrThrow(courseId);
    UUID currentUserId = SecurityUtils.getOptionalCurrentUserId();

    boolean isOwner = currentUserId != null && course.getTeacherId().equals(currentUserId);
    if (!isOwner) {
      boolean enrolled = currentUserId != null && enrollmentRepository
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(currentUserId, courseId)
          .map(e -> "ACTIVE".equals(e.getStatus().name()))
          .orElse(false);
      if (!enrolled) {
        throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
      }
    }

    CourseLesson cl = courseLessonRepository
        .findByIdAndDeletedAtIsNull(lessonId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    MaterialItem item = getMaterialList(cl.getMaterials()).stream()
        .filter(m -> m.getId().equals(materialId))
        .findFirst()
        .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));

    byte[] content;
    try {
      content = uploadService.downloadFile(
          item.getKey(), minioProperties.getCourseMaterialsBucket());
    } catch (Exception e) {
      // Legacy fallback: file stored in template bucket before course-materials bucket existed
      log.warn("Material not in course-materials bucket, falling back to template bucket for key: {}", item.getKey());
      content = uploadService.downloadFile(
          item.getKey(), minioProperties.getTemplateBucket());
    }
    String ct = item.getContentType() != null ? item.getContentType() : "application/octet-stream";
    return new CourseLessonService.MaterialDownloadResult(content, ct, item.getName());
  }

  private List<MaterialItem> getMaterialList(String json) {
    if (json == null || json.isBlank()) return new ArrayList<>();
    try {
      return objectMapper.readValue(json, new TypeReference<List<MaterialItem>>() {});
    } catch (Exception e) {
      log.warn("Legacy or invalid materials JSON detected (likely a plain string link): {}. Returning empty list.", json);
      return new ArrayList<>();
    }
  }

  private String getTitleForLesson(CourseLesson cl) {
    String lessonTitle = cl.getCustomTitle();
    if (cl.getLessonId() != null) {
      lessonTitle =
          lessonRepository
              .findByIdAndNotDeleted(cl.getLessonId())
              .map(l -> l.getTitle())
              .orElse(lessonTitle);
    }
    return lessonTitle;
  }
}
