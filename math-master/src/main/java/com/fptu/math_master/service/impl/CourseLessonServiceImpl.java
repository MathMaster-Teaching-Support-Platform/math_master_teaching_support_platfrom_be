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
      videoUrl = uploadService.uploadFile(videoFile, "course-videos");
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
      
      return mapToResponse(courseLesson, lesson.getTitle());

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
      
      return mapToResponse(courseLesson, request.getCustomTitle());
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
      courseLesson.setVideoUrl(uploadService.uploadFile(videoFile, "course-videos"));
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

    return mapToResponse(courseLesson, lessonTitle);
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

    courseLesson.setDeletedAt(Instant.now());
    courseLesson.setDeletedBy(currentUserId);
    courseLessonRepository.save(courseLesson);
    log.info("CourseLesson soft-deleted: {}", courseLessonId);
    
    courseService.syncCourseMetrics(courseId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseLessonResponse> getLessons(UUID courseId) {
    findCourseOrThrow(courseId);
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
              return mapToResponse(cl, lessonTitle);
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

  private CourseLessonResponse mapToResponse(CourseLesson cl, String lessonTitle) {
    String materialsJson = cl.getMaterials();
    if (materialsJson != null && !materialsJson.isBlank()) {
      List<MaterialItem> items = getMaterialList(materialsJson);
      for (MaterialItem item : items) {
        try {
          item.setUrl(uploadService.getPresignedUrl(item.getKey(), minioProperties.getCourseMaterialsBucket()));
        } catch (Exception e) {
          log.error("Error generating presigned URL for material {}", item.getId(), e);
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
        .videoUrl(cl.getVideoUrl())
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

    String objectKey = uploadService.uploadFile(file, minioProperties.getCourseMaterialsBucket());

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
    
    return mapToResponse(cl, getTitleForLesson(cl));
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
      // Note: We might want to actually delete the file from MinIO here, but for safety
      // let's just remove reference. In a production app, we'd have a cleanup job or delete immediately.
      // uploadService.deleteFile(toRemove.getKey()); // Need bucket info too
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

    return mapToResponse(cl, getTitleForLesson(cl));
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
