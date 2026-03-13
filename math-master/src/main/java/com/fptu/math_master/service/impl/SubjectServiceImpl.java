package com.fptu.math_master.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.dto.request.CreateSubjectRequest;
import com.fptu.math_master.dto.request.LinkGradeSubjectRequest;
import com.fptu.math_master.dto.response.SubjectResponse;
import com.fptu.math_master.entity.GradeSubject;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.GradeSubjectRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.service.SubjectService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectServiceImpl implements SubjectService {

  SubjectRepository subjectRepository;
  GradeSubjectRepository gradeSubjectRepository;

  @Override
  @Transactional
  public SubjectResponse createSubject(CreateSubjectRequest request) {
    log.info("Creating subject: code={}", request.getCode());
    if (subjectRepository.existsByCode(request.getCode())) {
      throw new AppException(ErrorCode.SUBJECT_ALREADY_EXISTS);
    }
    Subject subject =
        Subject.builder()
            .name(request.getName())
            .code(request.getCode())
            .description(request.getDescription())
            .gradeMin(request.getGradeMin())
            .gradeMax(request.getGradeMax())
            .isActive(true)
            .build();
    subject = subjectRepository.save(subject);
    log.info("Subject created: id={}", subject.getId());
    return buildResponse(subject);
  }

  @Override
  @Transactional(readOnly = true)
  public SubjectResponse getSubjectById(UUID subjectId) {
    Subject subject = loadOrThrow(subjectId);
    return buildResponse(subject);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubjectResponse> getAllSubjects() {
    return subjectRepository.findAllActive().stream()
        .map(this::buildResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubjectResponse> getSubjectsByGrade(Integer gradeLevel) {
    return subjectRepository.findActiveByGradeLevel(gradeLevel).stream()
        .map(this::buildResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public SubjectResponse linkToGrade(UUID subjectId, LinkGradeSubjectRequest request) {
    Subject subject = loadOrThrow(subjectId);
    if (gradeSubjectRepository.existsByGradeLevelAndSubjectId(request.getGradeLevel(), subjectId)) {
      throw new AppException(ErrorCode.GRADE_SUBJECT_ALREADY_EXISTS);
    }
    GradeSubject link =
        GradeSubject.builder()
            .subjectId(subjectId)
            .gradeLevel(request.getGradeLevel())
            .isActive(true)
            .build();
    gradeSubjectRepository.save(link);
    log.info("Linked subject {} to grade {}", subjectId, request.getGradeLevel());
    return buildResponse(subject);
  }

  @Override
  @Transactional
  public void unlinkFromGrade(UUID subjectId, Integer gradeLevel) {
    loadOrThrow(subjectId);
    GradeSubject link =
        gradeSubjectRepository
            .findByGradeLevelAndSubjectId(gradeLevel, subjectId)
            .orElseThrow(() -> new AppException(ErrorCode.GRADE_SUBJECT_NOT_FOUND));
    link.setIsActive(false);
    gradeSubjectRepository.save(link);
    log.info("Unlinked subject {} from grade {}", subjectId, gradeLevel);
  }

  @Override
  @Transactional
  public void deactivateSubject(UUID subjectId) {
    Subject subject = loadOrThrow(subjectId);
    subject.setIsActive(false);
    subjectRepository.save(subject);
    log.info("Subject deactivated: id={}", subjectId);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private Subject loadOrThrow(UUID subjectId) {
    return subjectRepository
        .findById(subjectId)
        .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
  }

  private SubjectResponse buildResponse(Subject subject) {
    List<Integer> grades =
        gradeSubjectRepository.findBySubjectIdAndIsActiveTrue(subject.getId()).stream()
            .map(GradeSubject::getGradeLevel)
            .sorted()
            .collect(Collectors.toList());

    return SubjectResponse.builder()
        .id(subject.getId())
        .name(subject.getName())
        .code(subject.getCode())
        .description(subject.getDescription())
        .gradeMin(subject.getGradeMin())
        .gradeMax(subject.getGradeMax())
        .isActive(subject.getIsActive())
        .gradeLevels(grades)
        .createdAt(subject.getCreatedAt())
        .updatedAt(subject.getUpdatedAt())
        .build();
  }
}
