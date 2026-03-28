package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.response.EnrollmentResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.EnrollmentService;
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
public class EnrollmentServiceImpl implements EnrollmentService {

  EnrollmentRepository enrollmentRepository;
  CourseRepository courseRepository;
  UserRepository userRepository;

  @Override
  public EnrollmentResponse enroll(UUID courseId) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    Course course =
        courseRepository
            .findByIdAndDeletedAtIsNull(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    if (!course.isPublished()) {
      throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
    }

    var existing =
        enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId);

    if (existing.isPresent()) {
      Enrollment e = existing.get();
      if (e.getStatus() == EnrollmentStatus.ACTIVE) {
        throw new AppException(ErrorCode.ALREADY_ENROLLED);
      }
      e.setStatus(EnrollmentStatus.ACTIVE);
      e.setEnrolledAt(Instant.now());
      e = enrollmentRepository.save(e);
      log.info("Student {} re-enrolled in course {}", studentId, courseId);
      return mapToResponse(e, course.getTitle(), studentId);
    }

    Enrollment enrollment =
        Enrollment.builder()
            .courseId(courseId)
            .studentId(studentId)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .build();

    enrollment = enrollmentRepository.save(enrollment);
    log.info("Student {} enrolled in course {}", studentId, courseId);
    return mapToResponse(enrollment, course.getTitle(), studentId);
  }

  @Override
  public EnrollmentResponse drop(UUID enrollmentId) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    Enrollment enrollment =
        enrollmentRepository
            .findByIdAndDeletedAtIsNull(enrollmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

    if (!enrollment.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
    }

    enrollment.setStatus(EnrollmentStatus.DROPPED);
    enrollment = enrollmentRepository.save(enrollment);

    String courseTitle =
        courseRepository
            .findByIdAndDeletedAtIsNull(enrollment.getCourseId())
            .map(Course::getTitle)
            .orElse(null);

    log.info("Student {} dropped enrollment {}", studentId, enrollmentId);
    return mapToResponse(enrollment, courseTitle, studentId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> getMyEnrollments() {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return enrollmentRepository
        .findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(studentId)
        .stream()
        .map(
            e -> {
              String courseTitle =
                  courseRepository
                      .findByIdAndDeletedAtIsNull(e.getCourseId())
                      .map(Course::getTitle)
                      .orElse(null);
              return mapToResponse(e, courseTitle, studentId);
            })
        .collect(Collectors.toList());
  }

  private EnrollmentResponse mapToResponse(Enrollment e, String courseTitle, UUID studentId) {
    String studentName = userRepository.findById(studentId).map(User::getFullName).orElse(null);
    return EnrollmentResponse.builder()
        .id(e.getId())
        .courseId(e.getCourseId())
        .courseTitle(courseTitle)
        .studentId(e.getStudentId())
        .studentName(studentName)
        .status(e.getStatus())
        .enrolledAt(e.getEnrolledAt())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }
}
