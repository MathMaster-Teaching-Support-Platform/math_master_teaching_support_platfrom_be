package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateLessonPlanRequest;
import com.fptu.math_master.dto.request.UpdateLessonPlanRequest;
import com.fptu.math_master.dto.response.LessonPlanResponse;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.LessonPlan;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.LessonPlanRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.LessonPlanService;
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
public class LessonPlanServiceImpl implements LessonPlanService {

  LessonPlanRepository lessonPlanRepository;
  LessonRepository lessonRepository;
  UserRepository userRepository;

  @Override
  public LessonPlanResponse createLessonPlan(CreateLessonPlanRequest request) {
    UUID teacherId = SecurityUtils.getCurrentUserId();

    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(request.getLessonId())
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    if (lessonPlanRepository.existsByLessonIdAndTeacherIdAndNotDeleted(
        request.getLessonId(), teacherId)) {
      throw new AppException(ErrorCode.LESSON_PLAN_ALREADY_EXISTS);
    }

    LessonPlan lessonPlan =
        LessonPlan.builder()
            .lessonId(request.getLessonId())
            .teacherId(teacherId)
            .objectives(request.getObjectives())
            .materialsNeeded(request.getMaterialsNeeded())
            .teachingStrategy(request.getTeachingStrategy())
            .assessmentMethods(request.getAssessmentMethods())
            .notes(request.getNotes())
            .build();

    lessonPlan = lessonPlanRepository.save(lessonPlan);
    log.info("LessonPlan created: {} for lesson: {}", lessonPlan.getId(), lesson.getId());

    return mapToResponse(lessonPlan, lesson.getTitle(), resolveTeacherName(teacherId));
  }

  @Override
  public LessonPlanResponse updateLessonPlan(UUID lessonPlanId, UpdateLessonPlanRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    LessonPlan lessonPlan =
        lessonPlanRepository
            .findByIdAndNotDeleted(lessonPlanId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_PLAN_NOT_FOUND));

    if (!lessonPlan.getTeacherId().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.LESSON_PLAN_ACCESS_DENIED);
    }

    if (request.getObjectives() != null) lessonPlan.setObjectives(request.getObjectives());
    if (request.getMaterialsNeeded() != null)
      lessonPlan.setMaterialsNeeded(request.getMaterialsNeeded());
    if (request.getTeachingStrategy() != null)
      lessonPlan.setTeachingStrategy(request.getTeachingStrategy());
    if (request.getAssessmentMethods() != null)
      lessonPlan.setAssessmentMethods(request.getAssessmentMethods());
    if (request.getNotes() != null) lessonPlan.setNotes(request.getNotes());

    lessonPlan = lessonPlanRepository.save(lessonPlan);

    String lessonTitle =
        lessonRepository
            .findByIdAndNotDeleted(lessonPlan.getLessonId())
            .map(Lesson::getTitle)
            .orElse(null);

    return mapToResponse(lessonPlan, lessonTitle, resolveTeacherName(lessonPlan.getTeacherId()));
  }

  @Override
  @Transactional(readOnly = true)
  public LessonPlanResponse getLessonPlanById(UUID lessonPlanId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    LessonPlan lessonPlan =
        lessonPlanRepository
            .findByIdAndNotDeleted(lessonPlanId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_PLAN_NOT_FOUND));

    if (!lessonPlan.getTeacherId().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.LESSON_PLAN_ACCESS_DENIED);
    }

    String lessonTitle =
        lessonRepository
            .findByIdAndNotDeleted(lessonPlan.getLessonId())
            .map(Lesson::getTitle)
            .orElse(null);

    return mapToResponse(lessonPlan, lessonTitle, resolveTeacherName(lessonPlan.getTeacherId()));
  }

  @Override
  @Transactional(readOnly = true)
  public LessonPlanResponse getMyLessonPlanByLesson(UUID lessonId) {
    UUID teacherId = SecurityUtils.getCurrentUserId();

    LessonPlan lessonPlan =
        lessonPlanRepository
            .findByTeacherIdAndLessonIdAndNotDeleted(teacherId, lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_PLAN_NOT_FOUND));

    String lessonTitle =
        lessonRepository.findByIdAndNotDeleted(lessonId).map(Lesson::getTitle).orElse(null);

    return mapToResponse(lessonPlan, lessonTitle, resolveTeacherName(teacherId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<LessonPlanResponse> getMyLessonPlans() {
    UUID teacherId = SecurityUtils.getCurrentUserId();
    String teacherName = resolveTeacherName(teacherId);

    return lessonPlanRepository.findByTeacherIdAndNotDeleted(teacherId).stream()
        .map(
            lp -> {
              String lessonTitle =
                  lessonRepository
                      .findByIdAndNotDeleted(lp.getLessonId())
                      .map(Lesson::getTitle)
                      .orElse(null);
              return mapToResponse(lp, lessonTitle, teacherName);
            })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<LessonPlanResponse> getLessonPlansByLesson(UUID lessonId) {
    lessonRepository
        .findByIdAndNotDeleted(lessonId)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    String lessonTitle =
        lessonRepository.findByIdAndNotDeleted(lessonId).map(Lesson::getTitle).orElse(null);

    return lessonPlanRepository.findByLessonIdAndNotDeleted(lessonId).stream()
        .map(lp -> mapToResponse(lp, lessonTitle, resolveTeacherName(lp.getTeacherId())))
        .collect(Collectors.toList());
  }

  @Override
  public void deleteLessonPlan(UUID lessonPlanId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    LessonPlan lessonPlan =
        lessonPlanRepository
            .findByIdAndNotDeleted(lessonPlanId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_PLAN_NOT_FOUND));

    if (!lessonPlan.getTeacherId().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.LESSON_PLAN_ACCESS_DENIED);
    }

    lessonPlan.setDeletedAt(Instant.now());
    lessonPlan.setDeletedBy(currentUserId);
    lessonPlanRepository.save(lessonPlan);
    log.info("LessonPlan soft-deleted: {}", lessonPlanId);
  }

  private String resolveTeacherName(UUID teacherId) {
    return userRepository.findById(teacherId).map(User::getFullName).orElse(null);
  }

  private LessonPlanResponse mapToResponse(LessonPlan lp, String lessonTitle, String teacherName) {
    return LessonPlanResponse.builder()
        .id(lp.getId())
        .lessonId(lp.getLessonId())
        .teacherId(lp.getTeacherId())
        .lessonTitle(lessonTitle)
        .teacherName(teacherName)
        .objectives(lp.getObjectives())
        .materialsNeeded(lp.getMaterialsNeeded())
        .teachingStrategy(lp.getTeachingStrategy())
        .assessmentMethods(lp.getAssessmentMethods())
        .notes(lp.getNotes())
        .createdAt(lp.getCreatedAt())
        .updatedAt(lp.getUpdatedAt())
        .build();
  }
}
