package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateCourseLessonRequest;
import com.fptu.math_master.dto.request.UpdateCourseLessonRequest;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.service.CourseLessonService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
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
  UploadService uploadService;

  @Override
  public CourseLessonResponse addLesson(
      UUID courseId, CreateCourseLessonRequest request, MultipartFile videoFile) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    var lesson =
        lessonRepository
            .findByIdAndNotDeleted(request.getLessonId())
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    String videoUrl = null;
    if (videoFile != null && !videoFile.isEmpty()) {
      videoUrl = uploadService.uploadFile(videoFile, "course-videos");
    }

    CourseLesson courseLesson =
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
    return mapToResponse(courseLesson, lesson.getTitle());
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

    String lessonTitle =
        lessonRepository
            .findByIdAndNotDeleted(courseLesson.getLessonId())
            .map(l -> l.getTitle())
            .orElse(null);

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
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseLessonResponse> getLessons(UUID courseId) {
    findCourseOrThrow(courseId);
    return courseLessonRepository.findByCourseIdAndNotDeleted(courseId).stream()
        .map(
            cl -> {
              String lessonTitle =
                  lessonRepository
                      .findByIdAndNotDeleted(cl.getLessonId())
                      .map(l -> l.getTitle())
                      .orElse(null);
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
    return CourseLessonResponse.builder()
        .id(cl.getId())
        .courseId(cl.getCourseId())
        .lessonId(cl.getLessonId())
        .lessonTitle(lessonTitle)
        .videoUrl(cl.getVideoUrl())
        .videoTitle(cl.getVideoTitle())
        .durationSeconds(cl.getDurationSeconds())
        .orderIndex(cl.getOrderIndex())
        .isFreePreview(cl.isFreePreview())
        .materials(cl.getMaterials())
        .createdAt(cl.getCreatedAt())
        .updatedAt(cl.getUpdatedAt())
        .build();
  }
}
