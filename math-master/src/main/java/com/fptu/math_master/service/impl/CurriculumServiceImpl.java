package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateCurriculumRequest;
import com.fptu.math_master.dto.request.UpdateCurriculumRequest;
import com.fptu.math_master.dto.response.CurriculumResponse;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.enums.CurriculumCategory;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.service.CurriculumService;
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
public class CurriculumServiceImpl implements CurriculumService {

  CurriculumRepository curriculumRepository;

  @Override
  public CurriculumResponse createCurriculum(CreateCurriculumRequest request) {
    log.info(
        "Creating new curriculum: {} (grade={}, category={})",
        request.getName(),
        request.getGrade(),
        request.getCategory());

    // Check if curriculum already exists
    if (curriculumRepository
        .findByNameAndGradeAndCategoryAndNotDeleted(
            request.getName(), request.getGrade(), request.getCategory())
        .isPresent()) {
      throw new AppException(ErrorCode.CURRICULUM_ALREADY_EXISTS);
    }

    Curriculum curriculum =
        Curriculum.builder()
            .name(request.getName())
            .grade(request.getGrade())
            .category(request.getCategory())
            .description(request.getDescription())
            .build();

    curriculum = curriculumRepository.save(curriculum);
    log.info("Curriculum created successfully with id: {}", curriculum.getId());

    return mapToResponse(curriculum);
  }

  @Override
  public CurriculumResponse updateCurriculum(UUID curriculumId, UpdateCurriculumRequest request) {
    log.info("Updating curriculum id: {}", curriculumId);

    Curriculum curriculum =
        curriculumRepository
            .findByIdAndNotDeleted(curriculumId)
            .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

    // Check if new combination conflicts with existing curriculum
    if (request.getName() != null || request.getGrade() != null || request.getCategory() != null) {
      String name = request.getName() != null ? request.getName() : curriculum.getName();
      Integer grade = request.getGrade() != null ? request.getGrade() : curriculum.getGrade();
      CurriculumCategory category =
          request.getCategory() != null ? request.getCategory() : curriculum.getCategory();

      if (!name.equals(curriculum.getName())
          || !grade.equals(curriculum.getGrade())
          || !category.equals(curriculum.getCategory())) {
        if (curriculumRepository
            .findByNameAndGradeAndCategoryAndNotDeleted(name, grade, category)
            .isPresent()) {
          throw new AppException(ErrorCode.CURRICULUM_ALREADY_EXISTS);
        }
      }
    }

    if (request.getName() != null) {
      curriculum.setName(request.getName());
    }
    if (request.getGrade() != null) {
      curriculum.setGrade(request.getGrade());
    }
    if (request.getCategory() != null) {
      curriculum.setCategory(request.getCategory());
    }
    if (request.getDescription() != null) {
      curriculum.setDescription(request.getDescription());
    }

    curriculum = curriculumRepository.save(curriculum);
    log.info("Curriculum updated successfully");

    return mapToResponse(curriculum);
  }

  @Override
  @Transactional(readOnly = true)
  public CurriculumResponse getCurriculumById(UUID curriculumId) {
    Curriculum curriculum =
        curriculumRepository
            .findByIdAndNotDeleted(curriculumId)
            .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

    return mapToResponse(curriculum);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CurriculumResponse> getAllCurricula(Pageable pageable) {
    return curriculumRepository.findAll(pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CurriculumResponse> getAllCurricula() {
    return curriculumRepository.findAllNotDeleted().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<CurriculumResponse> getCurriculaByGrade(Integer grade) {
    return curriculumRepository.findByGradeAndNotDeleted(grade).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<CurriculumResponse> getCurriculaByCategory(CurriculumCategory category) {
    return curriculumRepository.findByCategoryAndNotDeleted(category).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<CurriculumResponse> getCurriculaByGradeAndCategory(
      Integer grade, CurriculumCategory category) {
    return curriculumRepository.findByGradeAndCategoryAndNotDeleted(grade, category).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteCurriculum(UUID curriculumId) {
    log.info("Deleting curriculum id: {}", curriculumId);

    Curriculum curriculum =
        curriculumRepository
            .findByIdAndNotDeleted(curriculumId)
            .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

    curriculum.setDeletedAt(Instant.now());
    curriculumRepository.save(curriculum);

    log.info("Curriculum deleted successfully");
  }

  @Override
  @Transactional(readOnly = true)
  public List<CurriculumResponse> searchCurriculaByName(String name) {
    if (name == null || name.isBlank()) {
      return getAllCurricula();
    }

    String searchTerm = name.toLowerCase().trim();
    return getAllCurricula().stream()
        .filter(c -> c.getName().toLowerCase().contains(searchTerm))
        .collect(Collectors.toList());
  }

  private CurriculumResponse mapToResponse(Curriculum curriculum) {
    return CurriculumResponse.builder()
        .id(curriculum.getId())
        .name(curriculum.getName())
        .grade(curriculum.getGrade())
        .category(curriculum.getCategory())
        .description(curriculum.getDescription())
        .createdAt(curriculum.getCreatedAt())
        .updatedAt(curriculum.getUpdatedAt())
        .build();
  }
}
