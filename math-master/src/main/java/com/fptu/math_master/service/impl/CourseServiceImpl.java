package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.AddAssessmentToCourseRequest;
import com.fptu.math_master.dto.request.CreateCourseRequest;
import com.fptu.math_master.dto.request.UpdateCourseAssessmentRequest;
import com.fptu.math_master.dto.request.UpdateCourseRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.response.AvailableCourseAssessmentResponse;
import com.fptu.math_master.dto.response.CourseAssessmentResponse;
import com.fptu.math_master.dto.response.CoursePreviewResponse;
import com.fptu.math_master.dto.response.CourseLessonPreviewResponse;
import com.fptu.math_master.dto.response.CourseResponse;
import com.fptu.math_master.dto.response.StudentInCourseResponse;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentLesson;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseAssessment;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentLessonRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.CourseAssessmentRepository;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.CourseReviewRepository;
import com.fptu.math_master.repository.CustomCourseSectionRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.CourseService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.util.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
  ObjectMapper objectMapper;
  CourseAssessmentRepository courseAssessmentRepository;
  AssessmentRepository assessmentRepository;
  AssessmentLessonRepository assessmentLessonRepository;
  LessonRepository lessonRepository;
  UploadService uploadService;
  MinioProperties minioProperties;
  CourseReviewRepository courseReviewRepository;
  TeacherProfileRepository teacherProfileRepository;
  CustomCourseSectionRepository customCourseSectionRepository;

  @Override
  public CourseResponse createCourse(CreateCourseRequest request, MultipartFile thumbnailFile) {
    UUID teacherId = SecurityUtils.getCurrentUserId();

    Subject subject = null;
    SchoolGrade schoolGrade = null;

    if (request.getProvider() == com.fptu.math_master.enums.CourseProvider.MINISTRY) {
      // Ministry courses require both subjectId and schoolGradeId
      if (request.getSubjectId() == null) {
        throw new AppException(ErrorCode.SUBJECT_REQUIRED_FOR_MINISTRY_COURSE);
      }
      if (request.getSchoolGradeId() == null) {
        throw new AppException(ErrorCode.GRADE_REQUIRED_FOR_MINISTRY_COURSE);
      }
      subject = subjectRepository
          .findById(request.getSubjectId())
          .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
      schoolGrade = schoolGradeRepository
          .findById(request.getSchoolGradeId())
          .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));
    } else {
      // CUSTOM: subject/grade are optional
      if (request.getSubjectId() != null) {
        subject = subjectRepository.findById(request.getSubjectId()).orElse(null);
      }
      if (request.getSchoolGradeId() != null) {
        schoolGrade = schoolGradeRepository.findById(request.getSchoolGradeId()).orElse(null);
      }
    }

    String thumbnailUrl = resolveThumbnailForWrite(thumbnailFile);

    if (request.getOriginalPrice() != null && request.getDiscountedPrice() != null) {
      if (request.getDiscountedPrice().compareTo(request.getOriginalPrice()) > 0) {
        throw new AppException(ErrorCode.INVALID_AMOUNT);
      }
    }

    Course course = Course.builder()
        .teacherId(teacherId)
        .provider(request.getProvider())
        .subjectId(request.getSubjectId())
        .schoolGradeId(request.getSchoolGradeId())
        .title(request.getTitle())
        .description(request.getDescription())
        .thumbnailUrl(thumbnailUrl)
        .whatYouWillLearn(request.getWhatYouWillLearn())
        .requirements(request.getRequirements())
        .targetAudience(request.getTargetAudience())
        .subtitle(request.getSubtitle())
        .language(StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : "Tiếng Việt")
        .originalPrice(request.getOriginalPrice())
        .discountedPrice(request.getDiscountedPrice())
        .discountExpiryDate(request.getDiscountExpiryDate())
        .build();

    course = courseRepository.save(course);
    log.info("Course created: {} by teacher: {} [provider={}]", course.getId(), teacherId, course.getProvider());
    return mapToResponse(course, subject, schoolGrade);
  }

  @Override
  public CourseResponse updateCourse(
      UUID courseId, UpdateCourseRequest request, MultipartFile thumbnailFile) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    if (request.getTitle() != null)
      course.setTitle(request.getTitle());
    if (request.getDescription() != null)
      course.setDescription(request.getDescription());

    // Validate prices before updating
    java.math.BigDecimal activeOriginal = request.getOriginalPrice() != null ? request.getOriginalPrice() : course.getOriginalPrice();
    java.math.BigDecimal activeDiscounted = request.getDiscountedPrice() != null ? request.getDiscountedPrice() : course.getDiscountedPrice();
    
    if (activeOriginal != null && activeDiscounted != null) {
      if (activeDiscounted.compareTo(activeOriginal) > 0) {
        throw new AppException(ErrorCode.INVALID_AMOUNT);
      }
    }
    if (request.getWhatYouWillLearn() != null)
      course.setWhatYouWillLearn(request.getWhatYouWillLearn());
    if (request.getRequirements() != null)
      course.setRequirements(request.getRequirements());
    if (request.getTargetAudience() != null)
      course.setTargetAudience(request.getTargetAudience());
    if (request.getSubtitle() != null)
      course.setSubtitle(request.getSubtitle());
    if (request.getLanguage() != null)
      course.setLanguage(StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : "Tiếng Việt");
    if (request.getOriginalPrice() != null)
      course.setOriginalPrice(request.getOriginalPrice());
    if (request.getDiscountedPrice() != null)
      course.setDiscountedPrice(request.getDiscountedPrice());
    if (request.getDiscountExpiryDate() != null)
      course.setDiscountExpiryDate(request.getDiscountExpiryDate());

    if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
      course.setThumbnailUrl(resolveThumbnailForWrite(thumbnailFile));
    }

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
  public CoursePreviewResponse getCoursePreview(UUID courseId) {
    Course course = findCourseOrThrow(courseId);
    CourseResponse courseResponse = mapToResponse(course);
    
    List<CourseLessonPreviewResponse> lessons = courseLessonRepository.findByCourseIdAndNotDeleted(courseId)
        .stream()
        .map(cl -> {
          String lessonTitle = cl.getCustomTitle();
          if (cl.getLessonId() != null) {
            lessonTitle = lessonRepository.findByIdAndNotDeleted(cl.getLessonId())
                .map(Lesson::getTitle)
                .orElse(lessonTitle);
          }
          return CourseLessonPreviewResponse.builder()
              .id(cl.getId())
              .courseId(cl.getCourseId())
              .sectionId(cl.getSectionId())
              .lessonTitle(lessonTitle)
              .customDescription(cl.getCustomDescription())
              .videoTitle(cl.getVideoTitle())
              .durationSeconds(cl.getDurationSeconds())
              .orderIndex(cl.getOrderIndex())
              .isFreePreview(cl.isFreePreview())
              .createdAt(cl.getCreatedAt())
              .updatedAt(cl.getUpdatedAt())
              .build();
        })
        .collect(Collectors.toList());

    return CoursePreviewResponse.builder()
        .course(courseResponse)
        .lessons(lessons)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CourseResponse> getPublicCourses(
      UUID schoolGradeId, UUID subjectId, String keyword, Pageable pageable) {
    // Native query already has ORDER BY, ignore Pageable sort to avoid camelCase
    // field names
    Pageable unsortedPageable = org.springframework.data.domain.PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize());
    return courseRepository
        .findPublishedCoursesWithFilter(schoolGradeId, subjectId, keyword, unsortedPageable)
        .map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CourseResponse> searchCoursesForAdmin(String keyword, Pageable pageable) {
    String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    Pageable unsortedPageable = org.springframework.data.domain.PageRequest.of(
        pageable.getPageNumber(), pageable.getPageSize());
    return courseRepository
        .searchAllCoursesForAdmin(kw, unsortedPageable)
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
    User teacher = userRepository.findById(course.getTeacherId()).orElse(null);
    TeacherProfile profile = teacherProfileRepository.findByUserId(course.getTeacherId()).orElse(null);
    int studentsCount = (int) enrollmentRepository.countActiveEnrollmentsByCourseId(course.getId());
    int lessonsCount = (int) courseLessonRepository.countByCourseIdAndNotDeleted(course.getId());
    int sectionsCount = (int) customCourseSectionRepository.countByCourseIdAndDeletedAtIsNull(course.getId());
    int ratingCount = (int) courseReviewRepository.countByCourseIdAndDeletedAtIsNull(course.getId());

    UUID studentId = null;
    try {
      studentId = SecurityUtils.getCurrentUserId();
    } catch (Exception e) {
      // Ignored
    }

    Boolean isEnrolled = false;
    Integer completedLessons = 0;
    Double progress = 0.0;

    if (studentId != null) {
      Enrollment enrollment = enrollmentRepository
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, course.getId())
          .orElse(null);
      if (enrollment != null) {
        isEnrolled = true;
        completedLessons = (int) lessonProgressRepository.countCompletedByEnrollmentId(enrollment.getId());
        progress = lessonsCount == 0 ? 0.0 : (completedLessons * 100.0) / lessonsCount;
      }
    }

    return CourseResponse.builder()
        .id(course.getId())
        .teacherId(course.getTeacherId())
        .teacherName(teacher != null ? teacher.getFullName() : "Anonymous")
        .teacherAvatar(teacher != null ? teacher.getAvatar() : null)
        .teacherPosition(profile != null ? profile.getPosition() : null)
        .provider(course.getProvider())
        .subjectId(course.getSubjectId())
        .subjectName(subject != null ? subject.getName() : null)
        .schoolGradeId(course.getSchoolGradeId())
        .gradeLevel(schoolGrade != null ? schoolGrade.getGradeLevel() : null)
        .title(course.getTitle())
        .description(course.getDescription())
        .thumbnailUrl(resolveThumbnailForRead(course.getThumbnailUrl()))
        .isPublished(course.isPublished())
        .rating(course.getRating())
        .ratingCount(ratingCount)
        .studentsCount(studentsCount)
        .lessonsCount(lessonsCount)
        .sectionsCount(sectionsCount)
        .createdAt(course.getCreatedAt())
        .updatedAt(course.getUpdatedAt())
        .whatYouWillLearn(course.getWhatYouWillLearn())
        .requirements(course.getRequirements())
        .targetAudience(course.getTargetAudience())
        .subtitle(course.getSubtitle())
        .language(course.getLanguage())
        .totalVideoHours(course.getTotalVideoHours())
        .articlesCount(course.getArticlesCount())
        .resourcesCount(course.getResourcesCount())
        .originalPrice(course.getOriginalPrice())
        .discountedPrice(course.getDiscountedPrice())
        .discountExpiryDate(course.getDiscountExpiryDate())
        .isEnrolled(isEnrolled)
        .completedLessons(completedLessons)
        .progress(progress)
        .build();
  }

  /** mapToResponse khi chỉ có course — tự query subject/schoolGrade */
  private CourseResponse mapToResponse(Course course) {
    Subject subject = course.getSubjectId() != null
        ? subjectRepository.findById(course.getSubjectId()).orElse(null)
        : null;
    SchoolGrade schoolGrade = course.getSchoolGradeId() != null
        ? schoolGradeRepository.findById(course.getSchoolGradeId()).orElse(null)
        : null;
    return mapToResponse(course, subject, schoolGrade);
  }

  private String resolveThumbnailForWrite(MultipartFile thumbnailFile) {
    if (thumbnailFile == null || thumbnailFile.isEmpty()) {
      return null;
    }
    return uploadService.uploadFile(thumbnailFile, "course-thumbnails");
  }

  private String resolveThumbnailForRead(String thumbnailUrl) {
    if (!StringUtils.hasText(thumbnailUrl)) {
      return null;
    }

    if (thumbnailUrl.startsWith("http://") || thumbnailUrl.startsWith("https://")) {
      return thumbnailUrl;
    }

    try {
      return uploadService.getPresignedUrl(thumbnailUrl, minioProperties.getTemplateBucket());
    } catch (Exception ex) {
      log.warn("Failed to build presigned thumbnail URL for key={}", thumbnailUrl, ex);
      return thumbnailUrl;
    }
  }

  // ─── Course Assessment Management ─────────────────────────────────────────

  @Override
  public CourseAssessmentResponse addAssessmentToCourse(
      UUID courseId, AddAssessmentToCourseRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    Assessment assessment = assessmentRepository
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

    // Validate assessment status - only PUBLISHED assessments can be added to
    // courses
    if (assessment.getStatus() != com.fptu.math_master.enums.AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_PUBLISHED);
    }

    // Validate assessment has questions
    Long questionCount = assessmentRepository.countQuestionsByAssessmentId(assessment.getId());
    if (questionCount == null || questionCount == 0) {
      throw new AppException(ErrorCode.ASSESSMENT_NO_QUESTIONS);
    }

    // For MINISTRY courses: validate lesson-matching requirement
    // For CUSTOM courses: skip lesson-matching — teacher defines lessons freely
    Set<UUID> courseLessonIds = getCourseLessonIds(courseId);
    List<UUID> matchedLessonIds;
    boolean allowOutOfCourseLessons = Boolean.TRUE.equals(request.getAllowOutOfCourseLessons());

    if (course.getProvider() == com.fptu.math_master.enums.CourseProvider.MINISTRY) {
      matchedLessonIds = getMatchedLessonIds(request.getAssessmentId(), courseLessonIds);
      if (!allowOutOfCourseLessons && matchedLessonIds.isEmpty()) {
        throw new AppException(ErrorCode.ASSESSMENT_NOT_MATCH_COURSE_LESSONS);
      }
    } else {
      // CUSTOM: no Ministry-lesson matching required
      matchedLessonIds = List.of();
    }

    CourseAssessment courseAssessment = CourseAssessment.builder()
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

    Map<UUID, String> lessonTitleById = getLessonTitleById(courseLessonIds);
    List<String> matchedLessonTitles = matchedLessonIds.stream()
        .map(id -> lessonTitleById.getOrDefault(id, "Unknown lesson"))
        .toList();
    return mapToCourseAssessmentResponse(courseAssessment, assessment, matchedLessonTitles);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseAssessmentResponse> getCourseAssessments(UUID courseId, String status, String type,
      Boolean isRequired) {
    Course course = findCourseOrThrow(courseId);
    List<CourseAssessment> courseAssessments = courseAssessmentRepository.findByCourseIdAndNotDeleted(courseId);

    Set<UUID> courseLessonIds = getCourseLessonIds(courseId);
    Map<UUID, String> lessonTitleById = getLessonTitleById(courseLessonIds);
    Set<UUID> assessmentIds = courseAssessments.stream().map(CourseAssessment::getAssessmentId)
        .collect(Collectors.toSet());
    Map<UUID, List<String>> matchedLessonTitlesByAssessmentId = buildMatchedLessonTitlesByAssessmentId(assessmentIds,
        courseLessonIds, lessonTitleById);

    return courseAssessments.stream()
        .map(
            ca -> {
              Assessment assessment = assessmentRepository
                  .findByIdAndNotDeleted(ca.getAssessmentId())
                  .orElse(null);
              List<String> matchedTitles = matchedLessonTitlesByAssessmentId.getOrDefault(ca.getAssessmentId(),
                  List.of());
              return mapToCourseAssessmentResponse(ca, assessment, matchedTitles);
            })
        .filter(response -> {
          // Apply filters
          if (status != null && response.getAssessmentStatus() != null) {
            try {
              com.fptu.math_master.enums.AssessmentStatus statusEnum = com.fptu.math_master.enums.AssessmentStatus
                  .valueOf(status.toUpperCase());
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
              com.fptu.math_master.enums.AssessmentType typeEnum = com.fptu.math_master.enums.AssessmentType
                  .valueOf(type.toUpperCase());
              if (!response.getAssessmentType().equals(typeEnum)) {
                return false;
              }
            } catch (IllegalArgumentException e) {
              // Invalid type, skip this filter
              log.warn("Invalid assessment type filter: {}", type);
            }
          }

          if (isRequired != null && response.isRequired() != isRequired) {
            return false;
          }

          return true;
        })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<AvailableCourseAssessmentResponse> getAvailableAssessmentsForCourse(
      UUID courseId, boolean includeOutOfCourseLessons) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    Set<UUID> courseLessonIds = getCourseLessonIds(courseId);
    if (courseLessonIds.isEmpty() && !includeOutOfCourseLessons) {
      return List.of();
    }

    List<Assessment> assessments;
    if (includeOutOfCourseLessons) {
      assessments = assessmentRepository
          .findByTeacherIdAndStatusAndNotDeleted(
              currentUserId, com.fptu.math_master.enums.AssessmentStatus.PUBLISHED)
          .stream()
          .sorted(Comparator.comparing(Assessment::getCreatedAt).reversed())
          .toList();
    } else {
      List<UUID> candidateAssessmentIds = assessmentLessonRepository.findAssessmentIdsByLessonIds(courseLessonIds);
      if (candidateAssessmentIds.isEmpty()) {
        return List.of();
      }

      assessments = assessmentRepository.findByIdInAndNotDeleted(candidateAssessmentIds).stream()
          .filter(a -> a.getTeacherId().equals(currentUserId))
          .filter(a -> a.getStatus() == com.fptu.math_master.enums.AssessmentStatus.PUBLISHED)
          .sorted(Comparator.comparing(Assessment::getCreatedAt).reversed())
          .toList();
    }

    if (assessments.isEmpty()) {
      return List.of();
    }

    Set<UUID> assessmentIds = assessments.stream().map(Assessment::getId).collect(Collectors.toSet());
    Map<UUID, String> lessonTitleById = getLessonTitleById(courseLessonIds);
    Map<UUID, List<UUID>> matchedLessonIdsByAssessmentId = buildMatchedLessonIdsByAssessmentId(assessmentIds,
        courseLessonIds);

    Map<UUID, long[]> summaryMap = new HashMap<>();
    for (Object[] row : assessmentRepository.findBulkSummaryByIds(assessmentIds)) {
      UUID aid = (UUID) row[0];
      long questionCount = row[1] == null ? 0L : ((Number) row[1]).longValue();
      long pointsCents = row[2] == null
          ? 0L
          : new BigDecimal(row[2].toString()).multiply(BigDecimal.valueOf(100)).longValue();
      summaryMap.put(aid, new long[] { questionCount, pointsCents });
    }

    List<AvailableCourseAssessmentResponse> result = new ArrayList<>();
    for (Assessment assessment : assessments) {
      List<UUID> matchedLessonIds = matchedLessonIdsByAssessmentId.getOrDefault(assessment.getId(), List.of());
      List<String> matchedLessonTitles = matchedLessonIds.stream()
          .map(id -> lessonTitleById.getOrDefault(id, "Unknown lesson"))
          .toList();
      long[] summary = summaryMap.getOrDefault(assessment.getId(), new long[] { 0L, 0L });

      result.add(
          AvailableCourseAssessmentResponse.builder()
              .assessmentId(assessment.getId())
              .title(assessment.getTitle())
              .description(assessment.getDescription())
              .assessmentType(assessment.getAssessmentType())
              .status(assessment.getStatus())
              .timeLimitMinutes(assessment.getTimeLimitMinutes())
              .totalQuestions(summary[0])
              .totalPoints(
                  BigDecimal.valueOf(summary[1])
                      .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
              .matchedLessonCount(matchedLessonIds.size())
              .matchedLessonIds(matchedLessonIds)
              .matchedLessonTitles(matchedLessonTitles)
              .build());
    }

    return result;
  }

  @Override
  public CourseAssessmentResponse updateCourseAssessment(
      UUID courseId, UUID assessmentId, UpdateCourseAssessmentRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CourseAssessment courseAssessment = courseAssessmentRepository
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

    Assessment assessment = assessmentRepository.findByIdAndNotDeleted(assessmentId).orElse(null);
    Set<UUID> courseLessonIds = getCourseLessonIds(courseId);
    Map<UUID, String> lessonTitleById = getLessonTitleById(courseLessonIds);
    List<String> matchedLessonTitles = getMatchedLessonIds(assessmentId, courseLessonIds).stream()
        .map(id -> lessonTitleById.getOrDefault(id, "Unknown lesson"))
        .toList();
    return mapToCourseAssessmentResponse(courseAssessment, assessment, matchedLessonTitles);
  }

  @Override
  public void removeAssessmentFromCourse(UUID courseId, UUID assessmentId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CourseAssessment courseAssessment = courseAssessmentRepository
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
      CourseAssessment ca, Assessment assessment, List<String> matchedLessonTitles) {
    CourseAssessmentResponse.CourseAssessmentResponseBuilder builder = CourseAssessmentResponse.builder()
        .id(ca.getId())
        .courseId(ca.getCourseId())
        .assessmentId(ca.getAssessmentId())
        .orderIndex(ca.getOrderIndex())
        .isRequired(ca.isRequired())
        .matchedLessonCount(matchedLessonTitles.size())
        .matchedLessonTitles(matchedLessonTitles)
        .lessonMatched(!matchedLessonTitles.isEmpty())
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

  private Set<UUID> getCourseLessonIds(UUID courseId) {
    return courseLessonRepository.findByCourseIdAndNotDeleted(courseId).stream()
        .map(cl -> cl.getLessonId())
        .collect(Collectors.toCollection(HashSet::new));
  }

  private Map<UUID, String> getLessonTitleById(Set<UUID> lessonIds) {
    if (lessonIds.isEmpty()) {
      return Map.of();
    }
    return lessonRepository.findByIdInAndNotDeleted(lessonIds).stream()
        .collect(Collectors.toMap(Lesson::getId, Lesson::getTitle));
  }

  private List<UUID> getMatchedLessonIds(UUID assessmentId, Set<UUID> courseLessonIds) {
    if (courseLessonIds.isEmpty()) {
      return List.of();
    }
    return assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId).stream()
        .filter(courseLessonIds::contains)
        .distinct()
        .toList();
  }

  private Map<UUID, List<UUID>> buildMatchedLessonIdsByAssessmentId(
      Collection<UUID> assessmentIds, Set<UUID> courseLessonIds) {
    if (assessmentIds.isEmpty() || courseLessonIds.isEmpty()) {
      return Map.of();
    }

    Map<UUID, List<UUID>> map = new HashMap<>();
    List<AssessmentLesson> links = assessmentLessonRepository.findByAssessmentIdIn(assessmentIds);
    for (AssessmentLesson link : links) {
      if (!courseLessonIds.contains(link.getLessonId())) {
        continue;
      }
      map.computeIfAbsent(link.getAssessmentId(), ignored -> new ArrayList<>()).add(link.getLessonId());
    }
    return map;
  }

  private Map<UUID, List<String>> buildMatchedLessonTitlesByAssessmentId(
      Collection<UUID> assessmentIds,
      Set<UUID> courseLessonIds,
      Map<UUID, String> lessonTitleById) {
    Map<UUID, List<UUID>> lessonIdsByAssessmentId = buildMatchedLessonIdsByAssessmentId(assessmentIds, courseLessonIds);

    Map<UUID, List<String>> result = new HashMap<>();
    for (Map.Entry<UUID, List<UUID>> entry : lessonIdsByAssessmentId.entrySet()) {
      List<String> titles = entry.getValue().stream()
          .distinct()
          .map(id -> lessonTitleById.getOrDefault(id, "Unknown lesson"))
          .toList();
      result.put(entry.getKey(), titles);
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CourseResponse> getRelatedCourses(UUID courseId, Pageable pageable) {
    Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    return courseRepository.findRelatedCourses(
        course.getSubjectId(),
        course.getSchoolGradeId(),
        courseId,
        pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseResponse> getTeacherCourses(UUID teacherId) {
    return courseRepository.findByTeacherIdAndIsPublishedTrueAndDeletedAtIsNull(teacherId)
        .stream().map(this::mapToResponse).collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public com.fptu.math_master.dto.response.TeacherProfileResponse getTeacherProfile(UUID teacherId) {
    User teacher = userRepository.findById(teacherId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    TeacherProfile profile = teacherProfileRepository.findByUserId(teacherId).orElse(null);

    long totalCourses = courseRepository.findByTeacherIdAndIsPublishedTrueAndDeletedAtIsNull(teacherId).size();
    int totalStudents = enrollmentRepository.countStudentsByTeacherId(teacherId);
    long totalRatings = courseReviewRepository.countByTeacherId(teacherId);
    Double avg = courseReviewRepository.calculateTeacherAverageRating(teacherId);
    BigDecimal averageRating = avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    return com.fptu.math_master.dto.response.TeacherProfileResponse.builder()
        .userId(teacherId)
        .fullName(teacher.getFullName())
        .avatar(teacher.getAvatar())
        .description(profile != null ? profile.getDescription() : null)
        .position(profile != null ? profile.getPosition() : null)
        .websiteUrl(profile != null ? profile.getWebsiteUrl() : null)
        .linkedinUrl(profile != null ? profile.getLinkedinUrl() : null)
        .youtubeUrl(profile != null ? profile.getYoutubeUrl() : null)
        .facebookUrl(profile != null ? profile.getFacebookUrl() : null)
        .totalCourses(totalCourses)
        .totalStudents(totalStudents)
        .totalRatings((int) totalRatings)
        .averageRating(averageRating)
        .build();
  }

  @Override
  @Transactional
  public void syncCourseMetrics(UUID courseId) {
    Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    var lessons = courseLessonRepository.findByCourseIdAndNotDeleted(courseId);

    BigDecimal totalVideoSeconds = BigDecimal.ZERO;
    int articlesCount = 0;
    int resourcesCount = 0;

    for (var cl : lessons) {
      // 1. Video Hours
      if (cl.getDurationSeconds() != null) {
        totalVideoSeconds = totalVideoSeconds.add(BigDecimal.valueOf(cl.getDurationSeconds()));
      }

      // 2. Articles Count (Lesson with no video)
      if (!StringUtils.hasText(cl.getVideoUrl())) {
        articlesCount++;
      }

      // 3. Resources Count (Materials JSON)
      String materialsJson = cl.getMaterials();
      if (StringUtils.hasText(materialsJson)) {
        try {
          List<?> materials = objectMapper.readValue(materialsJson, new TypeReference<List<Object>>() {});
          resourcesCount += materials.size();
        } catch (Exception e) {
          log.warn("Failed to parse materials JSON for lesson {}: {}", cl.getId(), e.getMessage());
        }
      }
    }

    BigDecimal totalVideoHours = totalVideoSeconds.divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

    course.setTotalVideoHours(totalVideoHours);
    course.setArticlesCount(articlesCount);
    course.setResourcesCount(resourcesCount);

    courseRepository.save(course);
    log.info("Synced metrics for course {}: {}h, {} articles, {} resources", 
        courseId, totalVideoHours, articlesCount, resourcesCount);
  }
}
