package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.StudentWishRequest;
import com.fptu.math_master.dto.response.StudentWishResponse;
import com.fptu.math_master.entity.StudentWish;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.StudentWishRepository;
import com.fptu.math_master.service.StudentWishService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of StudentWishService
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentWishServiceImpl implements StudentWishService {

  StudentWishRepository studentWishRepository;

  @Override
  @Transactional
  public StudentWishResponse upsertWish(UUID studentId, StudentWishRequest request) {
    log.info("Creating/updating wish for student={}, subject={}", studentId, request.getSubject());

    // Check if wish already exists
    Optional<StudentWish> existing = studentWishRepository.findActiveWishByStudentAndSubject(
        studentId, request.getSubject());

    StudentWish wish;
    if (existing.isPresent()) {
      wish = existing.get();
      // Update existing wish
      wish.setLearningGoals(request.getLearningGoals());
      wish.setPreferredTopics(request.getPreferredTopics());
      wish.setWeakAreasToImprove(request.getWeakAreasToImprove());
      wish.setDailyStudyMinutes(request.getDailyStudyMinutes());
      wish.setTargetAccuracyPercentage(request.getTargetAccuracyPercentage());
      wish.setLearningStylePreference(request.getLearningStylePreference());
      wish.setPreferDifficultChallenges(request.getPreferDifficultChallenges());
      wish.setIsActive(true);
      log.info("Updated existing wish: {}", wish.getId());
    } else {
      // Create new wish
      wish = StudentWish.builder()
          .studentId(studentId)
          .subject(request.getSubject())
          .gradeLevel(request.getGradeLevel())
          .learningGoals(request.getLearningGoals())
          .preferredTopics(request.getPreferredTopics())
          .weakAreasToImprove(request.getWeakAreasToImprove())
          .dailyStudyMinutes(request.getDailyStudyMinutes())
          .targetAccuracyPercentage(request.getTargetAccuracyPercentage())
          .learningStylePreference(request.getLearningStylePreference())
          .preferDifficultChallenges(request.getPreferDifficultChallenges())
          .isActive(true)
          .build();
      log.info("Created new wish for student={}, subject={}", studentId, request.getSubject());
    }

    wish = studentWishRepository.save(wish);
    return mapToResponse(wish);
  }

  @Override
  @Transactional(readOnly = true)
  public StudentWishResponse getActiveWish(UUID studentId, String subject) {
    log.debug("Getting active wish for student={}, subject={}", studentId, subject);

    StudentWish wish = studentWishRepository.findActiveWishByStudentAndSubject(studentId, subject)
        .orElseThrow(() -> {
          log.warn("No active wish found for student={}, subject={}", studentId, subject);
          return new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
        });

    return mapToResponse(wish);
  }

  @Override
  @Transactional(readOnly = true)
  public List<StudentWishResponse> getActiveWishes(UUID studentId) {
    log.debug("Getting all active wishes for student={}", studentId);

    return studentWishRepository.findAllActiveWishesByStudent(studentId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<StudentWishResponse> getAllWishes(UUID studentId) {
    log.debug("Getting all wishes for student={}", studentId);

    return studentWishRepository.findAllWishesByStudent(studentId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void deactivateWish(UUID wishId) {
    log.info("Deactivating wish: {}", wishId);

    StudentWish wish = studentWishRepository.findById(wishId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    wish.setIsActive(false);
    studentWishRepository.save(wish);
  }

  @Override
  @Transactional
  public void deleteWish(UUID wishId) {
    log.info("Soft deleting wish: {}", wishId);

    StudentWish wish = studentWishRepository.findById(wishId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    wish.setDeletedAt(Instant.now());
    wish.setIsActive(false);
    studentWishRepository.save(wish);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean wishExists(UUID studentId, String subject) {
    return studentWishRepository.existsByStudentAndSubject(studentId, subject);
  }

  /**
   * Map StudentWish entity to response DTO
   */
  private StudentWishResponse mapToResponse(StudentWish wish) {
    return StudentWishResponse.builder()
        .id(wish.getId())
        .studentId(wish.getStudentId())
        .subject(wish.getSubject())
        .gradeLevel(wish.getGradeLevel())
        .learningGoals(wish.getLearningGoals())
        .preferredTopics(wish.getPreferredTopics())
        .weakAreasToImprove(wish.getWeakAreasToImprove())
        .dailyStudyMinutes(wish.getDailyStudyMinutes())
        .targetAccuracyPercentage(wish.getTargetAccuracyPercentage())
        .learningStylePreference(wish.getLearningStylePreference())
        .preferDifficultChallenges(wish.getPreferDifficultChallenges())
        .isActive(wish.getIsActive())
        .createdAt(wish.getCreatedAt())
        .updatedAt(wish.getUpdatedAt())
        .build();
  }
}
