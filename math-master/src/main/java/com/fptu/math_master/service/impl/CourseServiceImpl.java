package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateCourseRequest;
import com.fptu.math_master.dto.request.UpdateCourseRequest;
import com.fptu.math_master.dto.response.CourseResponse;
import com.fptu.math_master.dto.response.StudentInCourseResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.CourseService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class CourseServiceImpl implements CourseService {

  CourseRepository courseRepository;
  SubjectRepository subjectRepository;
  SchoolGradeRepository schoolGradeRepository;
  EnrollmentRepository enrollmentRepository;
  CourseLessonRepository courseLessonRepository;
  LessonProgressRepository lessonProgressRepository;
  UserRepository userRepository;

  @Override
  public CourseResponse createCourse(CreateCourseRequest request) {
    UUID teacherId = SecurityUtils.getCurrentUserId();

    Subject subject =
        subjectRepository
            .findById(request.getSubjectId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

    SchoolGrade schoolGrade =
        schoolGradeRepository
            .findById(request.getSchoolGradeId())
            .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));

    Course course =
        Course.builder()
            .teacherId(teacherId)
            .subjectId(request.getSubjectId())
            .schoolGradeId(request.getSchoolGradeId())
            .title(request.getTitle())
            .description(request.getDescription())
            .thumbnailUrl(request.getThumbnailUrl())
            .build();

    course = courseRepository.save(course);
    log.info("Course created: {} by teacher: {}", course.getId(), teacherId);
    return mapToResponse(course, subject, schoolGrade);
  }

  @Override
  public CourseResponse updateCourse(UUID courseId, UpdateCourseRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    if (request.getTitle() != null) course.setTitle(request.getTitle());
    if (request.getDescription() != null) course.setDescription(request.getDescription());
    if (request.getThumbnailUrl() != null) course.setThumbnailUrl(request.getThumbnailUrl());

    course = courseRepository.save(course);
    return mapToResponse(course);
  }

  @Override
  public void deleteCourse(UUID courseId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    if (enrollmentRepository.countActiveEnrollmentsByCourseId(courseId) > 0) {
      course.setPublished(false);
    }

    course.setDeletedAt(Instant.now());
    course.setDeletedBy(currentUserId);
    courseRepository.save(course);
    log.info("Course soft-deleted: {}", courseId);
  }

  @Override
  public CourseResponse publishCourse(UUID courseId, boolean publish) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    course.setPublished(publish);
    course = courseRepository.save(course);
    log.info("Course {} {}", courseId, publish ? "published" : "unpublished");
    return mapToResponse(course);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseResponse> getMyCourses() {
    UUID teacherId = SecurityUtils.getCurrentUserId();
    return courseRepository
        .findByTeacherIdAndDeletedAtIsNullOrderByCreatedAtDesc(teacherId)
        .stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public CourseResponse getCourseById(UUID courseId) {
    return mapToResponse(findCourseOrThrow(courseId));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CourseResponse> getPublicCourses(
      UUID schoolGradeId, UUID subjectId, String keyword, Pageable pageable) {
    return courseRepository
        .findPublishedCoursesWithFilter(schoolGradeId, subjectId, keyword, pageable)
        .map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<StudentInCourseResponse> getStudentsInCourse(UUID courseId, Pageable pageable) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    int totalLessons = (int) courseLessonRepository.countByCourseIdAndNotDeleted(courseId);

    return enrollmentRepository
        .findByCourseIdAndStatusAndDeletedAtIsNull(courseId, EnrollmentStatus.ACTIVE, pageable)
        .map(e -> buildStudentInCourseResponse(e, totalLessons));
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

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

  private StudentInCourseResponse buildStudentInCourseResponse(Enrollment e, int totalLessons) {
    User student = userRepository.findById(e.getStudentId()).orElse(null);
    int completedLessons = (int) lessonProgressRepository.countCompletedByEnrollmentId(e.getId());
    return StudentInCourseResponse.builder()
        .studentId(e.getStudentId())
        .studentName(student != null ? student.getFullName() : null)
        .email(student != null ? student.getEmail() : null)
        .enrolledAt(e.getEnrolledAt())
        .completedLessons(completedLessons)
        .totalLessons(totalLessons)
        .build();
  }

  /** mapToResponse khi đã có subject/schoolGrade object sẵn (tránh query lại) */
  private CourseResponse mapToResponse(Course course, Subject subject, SchoolGrade schoolGrade) {
    String teacherName =
        userRepository.findById(course.getTeacherId()).map(User::getFullName).orElse(null);
    int studentsCount = (int) enrollmentRepository.countActiveEnrollmentsByCourseId(course.getId());
    int lessonsCount = (int) courseLessonRepository.countByCourseIdAndNotDeleted(course.getId());

    return CourseResponse.builder()
        .id(course.getId())
        .teacherId(course.getTeacherId())
        .teacherName(teacherName)
        .subjectId(course.getSubjectId())
        .subjectName(subject != null ? subject.getName() : null)
        .schoolGradeId(course.getSchoolGradeId())
        .gradeLevel(schoolGrade != null ? schoolGrade.getGradeLevel() : null)
        .title(course.getTitle())
        .description(course.getDescription())
        .thumbnailUrl(course.getThumbnailUrl())
        .isPublished(course.isPublished())
        .rating(course.getRating())
        .studentsCount(studentsCount)
        .lessonsCount(lessonsCount)
        .createdAt(course.getCreatedAt())
        .updatedAt(course.getUpdatedAt())
        .build();
  }

  /** mapToResponse khi chỉ có course — tự query subject/schoolGrade */
  private CourseResponse mapToResponse(Course course) {
    Subject subject = subjectRepository.findById(course.getSubjectId()).orElse(null);
    SchoolGrade schoolGrade = schoolGradeRepository.findById(course.getSchoolGradeId()).orElse(null);
    return mapToResponse(course, subject, schoolGrade);
  }
}
