package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.UpdateProgressRequest;
import com.fptu.math_master.dto.response.LessonProgressItem;
import com.fptu.math_master.dto.response.StudentProgressResponse;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.LessonProgress;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.service.ProgressService;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class ProgressServiceImpl implements ProgressService {

  EnrollmentRepository enrollmentRepository;
  CourseLessonRepository courseLessonRepository;
  LessonProgressRepository lessonProgressRepository;
  CourseRepository courseRepository;

  @Override
  public LessonProgressItem markComplete(UUID enrollmentId, UUID courseLessonId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    Enrollment enrollment = findActiveEnrollmentOrThrow(enrollmentId, studentId);

    if (!courseLessonRepository.existsByIdAndCourseIdAndDeletedAtIsNull(
        courseLessonId, enrollment.getCourseId())) {
      throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
    }

    var existing =
        lessonProgressRepository.findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
            enrollmentId, courseLessonId);

    if (existing.isPresent()) {
      return mapToItem(existing.get());
    }

    LessonProgress progress =
        LessonProgress.builder()
            .enrollmentId(enrollmentId)
            .courseLessonId(courseLessonId)
            .isCompleted(true)
            .completedAt(Instant.now())
            .build();

    progress = lessonProgressRepository.save(progress);
    log.info("Lesson {} marked complete for enrollment {}", courseLessonId, enrollmentId);
    return mapToItem(progress);
  }

  @Override
  public LessonProgressItem updateProgress(
      UUID enrollmentId, UUID courseLessonId, UpdateProgressRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    Enrollment enrollment = findActiveEnrollmentOrThrow(enrollmentId, studentId);

    if (!courseLessonRepository.existsByIdAndCourseIdAndDeletedAtIsNull(
        courseLessonId, enrollment.getCourseId())) {
      throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
    }

    LessonProgress progress = lessonProgressRepository
        .findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(enrollmentId, courseLessonId)
        .orElse(LessonProgress.builder()
            .enrollmentId(enrollmentId)
            .courseLessonId(courseLessonId)
            .isCompleted(false)
            .watchedSeconds(0)
            .build());

    if (request.getWatchedSeconds() != null) {
      // Only allow increasing watched seconds, protect against reverting backward on seek
      progress.setWatchedSeconds(Math.max(progress.getWatchedSeconds(), request.getWatchedSeconds()));
      progress.setLastWatchedAt(Instant.now());
    }

    if (Boolean.TRUE.equals(request.getIsCompleted()) && !progress.isCompleted()) {
      progress.setCompleted(true);
      progress.setCompletedAt(Instant.now());
    }

    progress = lessonProgressRepository.save(progress);
    log.info("Progress updated for lesson {} on enrollment {}", courseLessonId, enrollmentId);
    return mapToItem(progress);
  }

  @Override
  @Transactional(readOnly = true)
  public StudentProgressResponse getProgress(UUID enrollmentId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    Enrollment enrollment = findEnrollmentOrThrow(enrollmentId, studentId);

    int totalLessons =
        (int) courseLessonRepository.countByCourseIdAndNotDeleted(enrollment.getCourseId());

    List<LessonProgress> progressList =
        lessonProgressRepository.findByEnrollmentIdOrderByCourseLessonOrderIndex(enrollmentId);

    int completedLessons = (int) progressList.stream().filter(LessonProgress::isCompleted).count();

    double completionRate =
        totalLessons == 0 ? 0.0 : Math.min(100.0, (completedLessons * 100.0) / totalLessons);

    String courseTitle =
        courseRepository
            .findByIdAndDeletedAtIsNull(enrollment.getCourseId())
            .map(c -> c.getTitle())
            .orElse(null);

    List<LessonProgressItem> lessonItems =
        courseLessonRepository.findByCourseIdAndNotDeleted(enrollment.getCourseId()).stream()
            .map(
                cl -> {
                  var lp =
                      lessonProgressRepository
                          .findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
                              enrollmentId, cl.getId())
                          .orElse(null);
                  return LessonProgressItem.builder()
                      .courseLessonId(cl.getId())
                      .videoTitle(cl.getVideoTitle())
                      .orderIndex(cl.getOrderIndex())
                      .isCompleted(lp != null && lp.isCompleted())
                      .completedAt(lp != null ? lp.getCompletedAt() : null)
                      .watchedSeconds(lp != null ? lp.getWatchedSeconds() : 0)
                      .build();
                })
            .collect(Collectors.toList());

    return StudentProgressResponse.builder()
        .enrollmentId(enrollmentId)
        .courseId(enrollment.getCourseId())
        .courseTitle(courseTitle)
        .totalLessons(totalLessons)
        .completedLessons(completedLessons)
        .completionRate(completionRate)
        .lessons(lessonItems)
        .build();
  }

  private Enrollment findActiveEnrollmentOrThrow(UUID enrollmentId, UUID studentId) {
    Enrollment enrollment =
        enrollmentRepository
            .findByIdAndDeletedAtIsNull(enrollmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

    if (!enrollment.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
    }
    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
      throw new AppException(ErrorCode.ENROLLMENT_NOT_FOUND);
    }
    return enrollment;
  }

  private Enrollment findEnrollmentOrThrow(UUID enrollmentId, UUID studentId) {
    Enrollment enrollment =
        enrollmentRepository
            .findByIdAndDeletedAtIsNull(enrollmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

    if (!enrollment.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
    }
    return enrollment;
  }

  private LessonProgressItem mapToItem(LessonProgress lp) {
    var cl = courseLessonRepository.findByIdAndDeletedAtIsNull(lp.getCourseLessonId()).orElse(null);
    return LessonProgressItem.builder()
        .courseLessonId(lp.getCourseLessonId())
        .videoTitle(cl != null ? cl.getVideoTitle() : null)
        .orderIndex(cl != null ? cl.getOrderIndex() : null)
        .isCompleted(lp.isCompleted())
        .completedAt(lp.getCompletedAt())
        .watchedSeconds(lp.getWatchedSeconds())
        .build();
  }
}
