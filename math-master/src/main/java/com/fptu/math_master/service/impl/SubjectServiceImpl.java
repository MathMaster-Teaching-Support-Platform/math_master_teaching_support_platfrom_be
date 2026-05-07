package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateSubjectRequest;
import com.fptu.math_master.dto.request.LinkGradeSubjectRequest;
import com.fptu.math_master.dto.request.UpdateSubjectRequest;
import com.fptu.math_master.dto.response.SubjectResponse;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.service.SubjectService;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectServiceImpl implements SubjectService {

  private static final Pattern NON_ASCII_WORD = Pattern.compile("[^A-Z0-9]+");
  private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}");

  SubjectRepository subjectRepository;
  SchoolGradeRepository schoolGradeRepository;
  ChapterRepository chapterRepository;

  @Override
  @Transactional
  public SubjectResponse createSubject(CreateSubjectRequest request) {
    log.info(
        "Creating subject: name={}, schoolGradeId={}",
        request.getName(),
        request.getSchoolGradeId());

    SchoolGrade schoolGrade =
        schoolGradeRepository
            .findByIdAndNotDeleted(request.getSchoolGradeId())
            .filter(g -> g.getDeletedAt() == null && Boolean.TRUE.equals(g.getIsActive()))
            .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));

    String subjectCode = generateUniqueCode(request.getName());

    Subject subject =
        Subject.builder()
            .name(request.getName())
            .code(subjectCode)
            .schoolGradeId(schoolGrade.getId())
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
  @Transactional
  public SubjectResponse updateSubject(UUID subjectId, UpdateSubjectRequest request) {
    Subject subject = loadOrThrow(subjectId);

    if (request.getSchoolGradeId() != null) {
      SchoolGrade schoolGrade =
          schoolGradeRepository
              .findByIdAndNotDeleted(request.getSchoolGradeId())
              .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
              .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));
      subject.setSchoolGradeId(schoolGrade.getId());
    }

    if (request.getName() != null && !request.getName().isBlank()) {
      subject.setName(request.getName().trim());
    }
    if (request.getDescription() != null) {
      subject.setDescription(request.getDescription());
    }
    if (request.getGradeMin() != null) {
      subject.setGradeMin(request.getGradeMin());
    }
    if (request.getGradeMax() != null) {
      subject.setGradeMax(request.getGradeMax());
    }
    if (request.getIsActive() != null) {
      subject.setIsActive(request.getIsActive());
    }

    if (subject.getGradeMin() != null
        && subject.getGradeMax() != null
        && subject.getGradeMin() > subject.getGradeMax()) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    subject = subjectRepository.save(subject);
    log.info("Subject updated: id={}", subjectId);
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
  public List<SubjectResponse> getAllSubjectsIncludingInactive() {
    return subjectRepository.findAllIncludingInactive().stream()
        .map(this::buildResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public SubjectResponse activateSubject(UUID subjectId) {
    Subject subject = loadOrThrow(subjectId);
    subject.setIsActive(true);
    subject = subjectRepository.save(subject);
    log.info("Subject activated: id={}", subjectId);
    return buildResponse(subject);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubjectResponse> getSubjectsByGrade(Integer gradeLevel) {
    return subjectRepository.findActiveByGradeLevel(gradeLevel).stream()
        .map(this::buildResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubjectResponse> getSubjectsBySchoolGradeId(UUID schoolGradeId) {
    schoolGradeRepository
        .findByIdAndNotDeleted(schoolGradeId)
        .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
        .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));

    return subjectRepository.findBySchoolGradeIdAndIsActiveTrueOrderByName(schoolGradeId).stream()
        .map(this::buildResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubjectResponse> getAllSubjectsBySchoolGradeId(UUID schoolGradeId) {
    schoolGradeRepository
        .findByIdAndNotDeleted(schoolGradeId)
        .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));

    return subjectRepository.findBySchoolGradeIdOrderByName(schoolGradeId).stream()
        .map(this::buildResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public SubjectResponse linkToGrade(UUID subjectId, LinkGradeSubjectRequest request) {
    Subject subject = loadOrThrow(subjectId);

    SchoolGrade schoolGrade =
        schoolGradeRepository
            .findByGradeLevel(request.getGradeLevel())
            .filter(g -> g.getDeletedAt() == null && Boolean.TRUE.equals(g.getIsActive()))
            .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));

    subject.setSchoolGradeId(schoolGrade.getId());
    subjectRepository.save(subject);
    log.info("Linked subject {} to grade {}", subjectId, request.getGradeLevel());
    return buildResponse(subject);
  }

  @Override
  @Transactional
  public void unlinkFromGrade(UUID subjectId, Integer gradeLevel) {
    Subject subject = loadOrThrow(subjectId);
    if (subject.getSchoolGrade() == null
        || !subject.getSchoolGrade().getGradeLevel().equals(gradeLevel)) {
      throw new AppException(ErrorCode.GRADE_SUBJECT_NOT_FOUND);
    }
    subject.setSchoolGradeId(null);
    subjectRepository.save(subject);
    log.info("Unlinked subject {} from grade {}", subjectId, gradeLevel);
  }

  @Override
  @Transactional
  public void deactivateSubject(UUID subjectId) {
    Subject subject = loadOrThrow(subjectId);

    if (chapterRepository.countActiveBySubjectId(subjectId) > 0) {
      throw new AppException(ErrorCode.SUBJECT_HAS_CHAPTERS);
    }

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
    Integer primaryGradeLevel =
        subject.getSchoolGrade() != null ? subject.getSchoolGrade().getGradeLevel() : null;
    List<Integer> gradeLevels = primaryGradeLevel == null ? List.of() : List.of(primaryGradeLevel);

    return SubjectResponse.builder()
        .id(subject.getId())
        .name(subject.getName())
        .code(subject.getCode())
        .description(subject.getDescription())
        .gradeMin(subject.getGradeMin())
        .gradeMax(subject.getGradeMax())
        .primaryGradeLevel(primaryGradeLevel)
        .schoolGradeId(subject.getSchoolGradeId())
        .isActive(subject.getIsActive())
        .gradeLevels(gradeLevels)
        .createdAt(subject.getCreatedAt())
        .updatedAt(subject.getUpdatedAt())
        .build();
  }

  private String generateUniqueCode(String subjectName) {
    String normalized =
        COMBINING_MARKS
            .matcher(Normalizer.normalize(subjectName, Normalizer.Form.NFD))
            .replaceAll("");
    String baseCode =
        NON_ASCII_WORD
            .matcher(normalized.toUpperCase(Locale.ROOT))
            .replaceAll("_")
            .replaceAll("^_+|_+$", "");
    if (baseCode.isBlank()) {
      baseCode = "SUBJECT";
    }

    String candidate = baseCode;
    int suffix = 1;
    while (subjectRepository.existsByCode(candidate)) {
      suffix++;
      candidate = baseCode + "_" + suffix;
    }
    return candidate;
  }
}
