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
      LessonProgress progress = existing.get();
      // If already completed, return immediately (idempotent)
      if (progress.isCompleted()) {
        return mapToItem(progress);
      }
      // Progress record exists but is not yet complete — mark it complete now
      progress.setCompleted(true);
      progress.setCompletedAt(Instant.now());
      progress = lessonProgressRepository.save(progress);
      log.info("Lesson {} (existing IN_PROGRESS record) marked complete for enrollment {}", courseLessonId, enrollmentId);
      return mapToItem(progress);
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

    var cl = courseLessonRepository.findByIdAndDeletedAtIsNull(courseLessonId).orElse(null);

    if (request.getWatchedSeconds() != null) {
      // Only allow increasing watched seconds, protect against reverting backward on seek
      progress.setWatchedSeconds(Math.max(progress.getWatchedSeconds(), request.getWatchedSeconds()));
      progress.setLastWatchedAt(Instant.now());

      // Auto-complete at 90% if we have duration
      if (cl != null && cl.getDurationSeconds() != null && cl.getDurationSeconds() > 0) {
        double percent = (progress.getWatchedSeconds() * 100.0) / cl.getDurationSeconds();
        if (percent >= 90.0 && !progress.isCompleted()) {
          progress.setCompleted(true);
          progress.setCompletedAt(Instant.now());
          log.info("Lesson {} auto-marked complete (reached 90% progress) for enrollment {}", 
              courseLessonId, enrollmentId);
        }
      }
    }

    if (Boolean.TRUE.equals(request.getIsCompleted()) && !progress.isCompleted()) {
      progress.setCompleted(true);
      progress.setCompletedAt(Instant.now());
    }

    progress = lessonProgressRepository.save(progress);
    log.info("Progress updated for lesson {} on enrollment {}", courseLessonId, enrollmentId);
    return mapToItem(cl, progress);
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
                  return mapToItem(cl, lp);
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
    return mapToItem(cl, lp);
  }

  private LessonProgressItem mapToItem(com.fptu.math_master.entity.CourseLesson cl, LessonProgress lp) {
    int watched = lp != null ? lp.getWatchedSeconds() : 0;
    boolean completed = lp != null && lp.isCompleted();
    double percent = 0.0;

    if (completed) {
      percent = 100.0;
    } else if (cl != null && cl.getDurationSeconds() != null && cl.getDurationSeconds() > 0) {
      percent = Math.min(100.0, (watched * 100.0) / cl.getDurationSeconds());
    }

    return LessonProgressItem.builder()
        .courseLessonId(cl != null ? cl.getId() : (lp != null ? lp.getCourseLessonId() : null))
        .videoTitle(cl != null ? cl.getVideoTitle() : null)
        .orderIndex(cl != null ? cl.getOrderIndex() : null)
        .isCompleted(completed)
        .completedAt(lp != null ? lp.getCompletedAt() : null)
        .watchedSeconds(watched)
        .progressPercent(percent)
        .build();
  }
}
