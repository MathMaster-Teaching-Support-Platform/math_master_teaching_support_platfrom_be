package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateCustomCourseSectionRequest;
import com.fptu.math_master.dto.request.UpdateCustomCourseSectionRequest;
import com.fptu.math_master.dto.response.CustomCourseSectionResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CustomCourseSection;
import com.fptu.math_master.enums.CourseProvider;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.CustomCourseSectionRepository;
import com.fptu.math_master.service.CustomCourseSectionService;
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
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class CustomCourseSectionServiceImpl implements CustomCourseSectionService {

  CustomCourseSectionRepository sectionRepository;
  CourseRepository courseRepository;

  @Override
  public CustomCourseSectionResponse createSection(
      UUID courseId, CreateCustomCourseSectionRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);
    verifyCustomProvider(course);

    CustomCourseSection section =
        CustomCourseSection.builder()
            .courseId(courseId)
            .title(request.getTitle())
            .description(request.getDescription())
            .orderIndex(request.getOrderIndex())
            .build();

    section = sectionRepository.save(section);
    log.info("CustomCourseSection created: {} for course: {}", section.getId(), courseId);
    return mapToResponse(section, 0);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CustomCourseSectionResponse> listSections(UUID courseId) {
    findCourseOrThrow(courseId); // Verify course exists
    return sectionRepository
        .findByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(courseId)
        .stream()
        .map(
            s -> {
              long lessonCount = sectionRepository.countActiveLessonsBySectionId(s.getId());
              return mapToResponse(s, (int) lessonCount);
            })
        .collect(Collectors.toList());
  }

  @Override
  public CustomCourseSectionResponse updateSection(
      UUID courseId, UUID sectionId, UpdateCustomCourseSectionRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CustomCourseSection section = findSectionOrThrow(sectionId);
    if (!section.getCourseId().equals(courseId)) {
      throw new AppException(ErrorCode.SECTION_NOT_IN_COURSE);
    }

    if (StringUtils.hasText(request.getTitle())) section.setTitle(request.getTitle());
    if (request.getDescription() != null) section.setDescription(request.getDescription());
    if (request.getOrderIndex() != null) section.setOrderIndex(request.getOrderIndex());

    section = sectionRepository.save(section);
    long lessonCount = sectionRepository.countActiveLessonsBySectionId(section.getId());
    return mapToResponse(section, (int) lessonCount);
  }

  @Override
  public void deleteSection(UUID courseId, UUID sectionId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course = findCourseOrThrow(courseId);
    verifyOwnership(course, currentUserId);

    CustomCourseSection section = findSectionOrThrow(sectionId);
    if (!section.getCourseId().equals(courseId)) {
      throw new AppException(ErrorCode.SECTION_NOT_IN_COURSE);
    }

    long activeLessons = sectionRepository.countActiveLessonsBySectionId(sectionId);
    if (activeLessons > 0) {
      throw new AppException(ErrorCode.CUSTOM_COURSE_SECTION_HAS_LESSONS);
    }

    section.setDeletedAt(Instant.now());
    section.setDeletedBy(currentUserId);
    sectionRepository.save(section);
    log.info("CustomCourseSection soft-deleted: {}", sectionId);
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

  private void verifyCustomProvider(Course course) {
    if (course.getProvider() != CourseProvider.CUSTOM) {
      throw new AppException(ErrorCode.OPERATION_NOT_SUPPORTED_FOR_PROVIDER);
    }
  }

  private CustomCourseSection findSectionOrThrow(UUID sectionId) {
    return sectionRepository
        .findByIdAndDeletedAtIsNull(sectionId)
        .orElseThrow(() -> new AppException(ErrorCode.CUSTOM_COURSE_SECTION_NOT_FOUND));
  }

  private CustomCourseSectionResponse mapToResponse(CustomCourseSection s, int lessonCount) {
    return CustomCourseSectionResponse.builder()
        .id(s.getId())
        .courseId(s.getCourseId())
        .title(s.getTitle())
        .description(s.getDescription())
        .orderIndex(s.getOrderIndex())
        .lessonCount(lessonCount)
        .createdAt(s.getCreatedAt())
        .updatedAt(s.getUpdatedAt())
        .build();
  }
}
