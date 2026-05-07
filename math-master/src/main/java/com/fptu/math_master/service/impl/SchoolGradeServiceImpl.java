package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateSchoolGradeRequest;
import com.fptu.math_master.dto.request.UpdateSchoolGradeRequest;
import com.fptu.math_master.dto.response.SchoolGradeResponse;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.service.SchoolGradeService;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SchoolGradeServiceImpl implements SchoolGradeService {

  SchoolGradeRepository schoolGradeRepository;
  SubjectRepository subjectRepository;

  @Override
  @Transactional
  public SchoolGradeResponse create(CreateSchoolGradeRequest request) {
    if (schoolGradeRepository.existsByGradeLevel(request.getGradeLevel())) {
      throw new AppException(ErrorCode.SCHOOL_GRADE_ALREADY_EXISTS);
    }

    SchoolGrade schoolGrade =
        SchoolGrade.builder()
            .gradeLevel(request.getGradeLevel())
            .name(request.getName())
            .description(request.getDescription())
            .isActive(true)
            .build();

    return toResponse(schoolGradeRepository.save(schoolGrade));
  }

  @Override
  @Transactional
  public SchoolGradeResponse update(UUID id, UpdateSchoolGradeRequest request) {
    SchoolGrade schoolGrade =
        schoolGradeRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));

    if (request.getGradeLevel() != null
        && !request.getGradeLevel().equals(schoolGrade.getGradeLevel())
        && schoolGradeRepository.existsByGradeLevel(request.getGradeLevel())) {
      throw new AppException(ErrorCode.SCHOOL_GRADE_ALREADY_EXISTS);
    }

    if (request.getGradeLevel() != null) {
      schoolGrade.setGradeLevel(request.getGradeLevel());
    }
    if (request.getName() != null && !request.getName().isBlank()) {
      schoolGrade.setName(request.getName());
    }
    if (request.getDescription() != null) {
      schoolGrade.setDescription(request.getDescription());
    }
    if (request.getActive() != null) {
      schoolGrade.setIsActive(request.getActive());
    }

    return toResponse(schoolGradeRepository.save(schoolGrade));
  }

  @Override
  @Transactional(readOnly = true)
  public SchoolGradeResponse getById(UUID id) {
    SchoolGrade schoolGrade =
        schoolGradeRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));
    return toResponse(schoolGrade);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SchoolGradeResponse> getAll(boolean activeOnly) {
    List<SchoolGrade> schoolGrades =
        activeOnly
            ? schoolGradeRepository.findAllActiveNotDeleted()
            : schoolGradeRepository.findAllNotDeleted();
    return schoolGrades.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public void deactivate(UUID id) {
    SchoolGrade schoolGrade =
        schoolGradeRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));

    if (subjectRepository.countBySchoolGradeIdAndIsActiveTrueAndDeletedAtIsNull(id) > 0) {
      throw new AppException(ErrorCode.SCHOOL_GRADE_HAS_SUBJECTS);
    }

    schoolGrade.setIsActive(false);
    schoolGradeRepository.save(schoolGrade);
  }

  @Override
  @Transactional
  public SchoolGradeResponse activate(UUID id) {
    SchoolGrade schoolGrade =
        schoolGradeRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));
    schoolGrade.setIsActive(true);
    return toResponse(schoolGradeRepository.save(schoolGrade));
  }

  private SchoolGradeResponse toResponse(SchoolGrade schoolGrade) {
    return SchoolGradeResponse.builder()
        .id(schoolGrade.getId())
        .gradeLevel(schoolGrade.getGradeLevel())
        .name(schoolGrade.getName())
        .description(schoolGrade.getDescription())
        .active(Boolean.TRUE.equals(schoolGrade.getIsActive()))
        .createdAt(schoolGrade.getCreatedAt())
        .updatedAt(schoolGrade.getUpdatedAt())
        .build();
  }
}
