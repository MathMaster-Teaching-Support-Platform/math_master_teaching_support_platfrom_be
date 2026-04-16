package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.AddAssessmentToCourseRequest;
import com.fptu.math_master.dto.request.CreateCourseRequest;
import com.fptu.math_master.dto.request.UpdateCourseAssessmentRequest;
import com.fptu.math_master.dto.request.UpdateCourseRequest;
import com.fptu.math_master.dto.response.CourseAssessmentResponse;
import com.fptu.math_master.dto.response.CourseResponse;
import com.fptu.math_master.dto.response.StudentInCourseResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseAssessment;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.CourseAssessmentRepository;
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
  CourseAssessmentRepository courseAssessmentRepository;
  AssessmentRepository assessmentRepository;

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
    // Native query already has ORDER BY, ignore Pageable sort to avoid camelCase field names
    Pageable unsortedPageable = org.springframework.data.domain.PageRequest.of(
        pageable.getPageNumber(), 
        pageable.getPageSize()
    );
    return courseRepository
        .findPublishedCoursesWithFilter(schoolGradeId, subjectId, keyword, unsortedPageable)
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

  // ─── Course Assessment Management ─────────────────────────────────────────

  @Override
  public CourseAssessmentResponse addAssessmentToCourse(
      UUID courseId, AddAssessmentToCourseRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(request.getAssessmentId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    // Verify assessment belongs to the same teacher
    if (!assessment.getTeacherId().equals(currentUserId)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }

    // Check if assessment is already added to this course
    if (courseAssessmentRepository.existsByCourseIdAndAssessmentIdAndNotDeleted(
        courseId, request.getAssessmentId())) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_IN_COURSE);
    }

    // Validate assessment status - only PUBLISHED assessments can be added to courses
    if (assessment.getStatus() != com.fptu.math_master.enums.AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_PUBLISHED);
    }

    // Validate assessment has questions
    Long questionCount = assessmentRepository.countQuestionsByAssessmentId(assessment.getId());
    if (questionCount == null || questionCount == 0) {
      throw new AppException(ErrorCode.ASSESSMENT_NO_QUESTIONS);
    }

    CourseAssessment courseAssessment =
        CourseAssessment.builder()
            .courseId(courseId)
            .assessmentId(request.getAssessmentId())
            .orderIndex(request.getOrderIndex())
            .isRequired(request.getIsRequired() != null ? request.getIsRequired() : true)
            .build();

    courseAssessment = courseAssessmentRepository.save(courseAssessment);
    log.info(
        "Assessment {} added to course {} by teacher {}",
        request.getAssessmentId(),
        courseId,
        currentUserId);

    return mapToCourseAssessmentResponse(courseAssessment, assessment);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseAssessmentResponse> getCourseAssessments(UUID courseId, String status, String type, Boolean isRequired) {
    Course course = findCourseOrThrow(courseId);
    List<CourseAssessment> courseAssessments =
        courseAssessmentRepository.findByCourseIdAndNotDeleted(courseId);

    return courseAssessments.stream()
        .map(
            ca -> {
              Assessment assessment =
                  assessmentRepository
                      .findByIdAndNotDeleted(ca.getAssessmentId())
                      .orElse(null);
              return mapToCourseAssessmentResponse(ca, assessment);
            })
        .filter(response -> {
          // Apply filters
          if (status != null && response.getAssessmentStatus() != null) {
            try {
              com.fptu.math_master.enums.AssessmentStatus statusEnum = 
                  com.fptu.math_master.enums.AssessmentStatus.valueOf(status.toUpperCase());
              if (!response.getAssessmentStatus().equals(statusEnum)) {
                return false;
              }
            } catch (IllegalArgumentException e) {
              // Invalid status, skip this filter
              log.warn("Invalid assessment status filter: {}", status);
            }
          }
          
          if (type != null && response.getAssessmentType() != null) {
            try {
              com.fptu.math_master.enums.AssessmentType typeEnum = 
                  com.fptu.math_master.enums.AssessmentType.valueOf(type.toUpperCase());
              if (!response.getAssessmentType().equals(typeEnum)) {
                return false;
              }
            } catch (IllegalArgumentException e) {
              // Invalid type, skip this filter
              log.warn("Invalid assessment type filter: {}", type);
            }
          }
          
          if (isRequired != null && response.getIsRequired() != isRequired) {
            return false;
          }
          
          return true;
        })
        .collect(Collectors.toList());
  }

  @Override
  public CourseAssessmentResponse updateCourseAssessment(
      UUID courseId, UUID assessmentId, UpdateCourseAssessmentRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CourseAssessment courseAssessment =
        courseAssessmentRepository
            .findByCourseIdAndAssessmentIdAndNotDeleted(courseId, assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_ASSESSMENT_NOT_FOUND));

    if (request.getOrderIndex() != null) {
      courseAssessment.setOrderIndex(request.getOrderIndex());
    }
    if (request.getIsRequired() != null) {
      courseAssessment.setRequired(request.getIsRequired());
    }

    courseAssessment = courseAssessmentRepository.save(courseAssessment);
    log.info("Course assessment {} updated in course {}", assessmentId, courseId);

    Assessment assessment =
        assessmentRepository.findByIdAndNotDeleted(assessmentId).orElse(null);
    return mapToCourseAssessmentResponse(courseAssessment, assessment);
  }

  @Override
  public void removeAssessmentFromCourse(UUID courseId, UUID assessmentId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CourseAssessment courseAssessment =
        courseAssessmentRepository
            .findByCourseIdAndAssessmentIdAndNotDeleted(courseId, assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_ASSESSMENT_NOT_FOUND));

    // Check if assessment has submissions from students
    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(assessmentId);
    if (submissionCount != null && submissionCount > 0) {
      log.warn(
          "Attempting to remove assessment {} with {} submissions from course {}",
          assessmentId,
          submissionCount,
          courseId);
      throw new AppException(ErrorCode.ASSESSMENT_HAS_SUBMISSIONS);
    }

    courseAssessment.setDeletedAt(Instant.now());
    courseAssessment.setDeletedBy(currentUserId);
    courseAssessmentRepository.save(courseAssessment);

    log.info("Assessment {} removed from course {} by teacher {}", assessmentId, courseId, currentUserId);
  }

  private CourseAssessmentResponse mapToCourseAssessmentResponse(
      CourseAssessment ca, Assessment assessment) {
    CourseAssessmentResponse.CourseAssessmentResponseBuilder builder =
        CourseAssessmentResponse.builder()
            .id(ca.getId())
            .courseId(ca.getCourseId())
            .assessmentId(ca.getAssessmentId())
            .orderIndex(ca.getOrderIndex())
            .isRequired(ca.isRequired())
            .createdAt(ca.getCreatedAt())
            .updatedAt(ca.getUpdatedAt());

    if (assessment != null) {
      Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(assessment.getId());
      Double totalPointsDouble = assessmentRepository.calculateTotalPoints(assessment.getId());
      Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(assessment.getId());

      builder
          .assessmentTitle(assessment.getTitle())
          .assessmentDescription(assessment.getDescription())
          .assessmentType(assessment.getAssessmentType())
          .assessmentStatus(assessment.getStatus())
          .timeLimitMinutes(assessment.getTimeLimitMinutes())
          .passingScore(assessment.getPassingScore())
          .startDate(assessment.getStartDate())
          .endDate(assessment.getEndDate())
          .totalQuestions(totalQuestions)
          .totalPoints(
              totalPointsDouble != null
                  ? java.math.BigDecimal.valueOf(totalPointsDouble)
                  : java.math.BigDecimal.ZERO)
          .submissionCount(submissionCount);
    }

    return builder.build();
  }
}
