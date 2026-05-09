package com.fptu.math_master.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fptu.math_master.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AddTemplateMappingRequest;
import com.fptu.math_master.dto.request.BatchAddTemplateMappingsRequest;
import com.fptu.math_master.dto.request.BatchUpsertMatrixRowCellsRequest;
import com.fptu.math_master.dto.request.BuildExamMatrixRequest;
import com.fptu.math_master.dto.request.BuildSimpleExamMatrixRequest;
import com.fptu.math_master.dto.request.ExamMatrixPartRequest;
import com.fptu.math_master.dto.request.ExamMatrixRequest;
import com.fptu.math_master.dto.request.FinalizePreviewRequest;
import com.fptu.math_master.dto.request.GeneratePreviewRequest;
import com.fptu.math_master.dto.request.MatrixCellRequest;
import com.fptu.math_master.dto.request.MatrixRowRequest;
import com.fptu.math_master.dto.response.BatchTemplateMappingsResponse;
import com.fptu.math_master.dto.response.BankMappingResponse;
import com.fptu.math_master.dto.response.ExamMatrixPartResponse;
import com.fptu.math_master.dto.response.ExamMatrixResponse;
import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.FinalizePreviewResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.dto.response.MatchingTemplatesResponse;
import com.fptu.math_master.dto.response.MatrixCellResponse;
import com.fptu.math_master.dto.response.MatrixChapterGroupResponse;
import com.fptu.math_master.dto.response.MatrixRowResponse;
import com.fptu.math_master.dto.response.MatrixValidationReport;
import com.fptu.math_master.dto.response.PreviewCandidatesResponse;
import com.fptu.math_master.dto.response.TemplateMappingResponse;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.ExamPart;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.repository.ExamMatrixRepository;
import com.fptu.math_master.repository.ExamMatrixBankMappingRepository;
import com.fptu.math_master.repository.ExamMatrixPartRepository;
import com.fptu.math_master.repository.ExamMatrixRowRepository;
import com.fptu.math_master.repository.ExamMatrixTemplateMappingRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.ExamMatrixService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamMatrixServiceImpl implements ExamMatrixService {

  ExamMatrixRepository examMatrixRepository;
  ExamMatrixBankMappingRepository bankMappingRepository;
  ExamMatrixPartRepository examMatrixPartRepository;
  ExamMatrixTemplateMappingRepository templateMappingRepository;
  ExamMatrixRowRepository examMatrixRowRepository;
  QuestionTemplateRepository questionTemplateRepository;
  QuestionRepository questionRepository;
  QuestionBankRepository questionBankRepository;
  AssessmentRepository assessmentRepository;
  AssessmentQuestionRepository assessmentQuestionRepository;
  UserRepository userRepository;
  CurriculumRepository curriculumRepository;
  ChapterRepository chapterRepository;
  SubjectRepository subjectRepository;
  AIEnhancementService aiEnhancementService;

  // ── Matrix CRUD ─────────────────────────────────────────────────────────

  @Override
  @Transactional
  public ExamMatrixResponse createExamMatrix(ExamMatrixRequest request) {
    log.info("Creating exam matrix: {}", request.getName());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);

    // BE-5: Determine effective numberOfParts from parts[] or numberOfParts field
    int effectiveNumberOfParts = (request.getParts() != null && !request.getParts().isEmpty())
        ? request.getParts().size()
        : (request.getNumberOfParts() != null ? request.getNumberOfParts() : 1);

    ExamMatrix matrix =
        ExamMatrix.builder()
            .teacherId(currentUserId)
            .name(request.getName())
            .description(request.getDescription())
            .isReusable(request.getIsReusable() != null ? request.getIsReusable() : false)
            .totalQuestionsTarget(request.getTotalQuestionsTarget())
            .totalPointsTarget(request.getTotalPointsTarget())
            .questionBankId(request.getQuestionBankId())
            .numberOfParts(effectiveNumberOfParts)
            .gradeLevel(request.getGradeLevel())
            .status(MatrixStatus.DRAFT)
            .build();

    matrix = examMatrixRepository.save(matrix);

    // BE-5: Create parts using createOrReplaceParts
    createOrReplaceParts(matrix.getId(), request.getParts(), request.getNumberOfParts());

    log.info("Exam matrix created with id: {}", matrix.getId());
    return buildMatrixResponse(matrix, Collections.emptyList());
  }

  @Override
  @Transactional
  public ExamMatrixResponse updateExamMatrix(UUID matrixId, ExamMatrixRequest request) {
    log.info("Updating exam matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    matrix.setName(request.getName());
    matrix.setDescription(request.getDescription());
    if (request.getIsReusable() != null) {
      matrix.setIsReusable(request.getIsReusable());
    }
    if (request.getTotalQuestionsTarget() != null) {
      matrix.setTotalQuestionsTarget(request.getTotalQuestionsTarget());
    }
    if (request.getTotalPointsTarget() != null) {
      matrix.setTotalPointsTarget(request.getTotalPointsTarget());
    }
    if (request.getQuestionBankId() != null) {
      matrix.setQuestionBankId(request.getQuestionBankId());
    }

    // BE-6: Handle part changes
    if (request.getParts() != null || request.getNumberOfParts() != null) {
      int newPartCount = (request.getParts() != null && !request.getParts().isEmpty())
          ? request.getParts().size()
          : (request.getNumberOfParts() != null ? request.getNumberOfParts() : matrix.getNumberOfParts());
      int oldPartCount = matrix.getNumberOfParts();

      // Create or replace parts
      Map<Integer, ExamMatrixPart> partsCache =
          createOrReplaceParts(matrixId, request.getParts(), request.getNumberOfParts());

      // Delete cells for removed parts
      if (newPartCount < oldPartCount) {
        bankMappingRepository.deleteByExamMatrixIdAndPartNumberGreaterThan(matrixId, newPartCount);
        log.info("Deleted cells with partNumber > {} for matrix {}", newPartCount, matrixId);
      }

      // Re-sync existing cells: update partId + questionType from new parts
      List<ExamMatrixBankMapping> existingCells =
          bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);
      for (ExamMatrixBankMapping cell : existingCells) {
        ExamMatrixPart newPart = partsCache.get(cell.getPartNumber());
        if (newPart != null) {
          cell.setPartId(newPart.getId());
          cell.setQuestionType(newPart.getQuestionType());
          bankMappingRepository.save(cell);
        }
      }

      matrix.setNumberOfParts(newPartCount);
    }

    matrix = examMatrixRepository.save(matrix);
    log.info("Exam matrix updated: {}", matrixId);
    return buildMatrixResponse(
        matrix, templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId));
  }  @Override
  @Transactional(readOnly = true)
  public ExamMatrixResponse getExamMatrixById(UUID matrixId) {
    log.info("Getting exam matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);
    return buildMatrixResponse(matrix, mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public ExamMatrixResponse getExamMatrixByAssessmentId(UUID assessmentId) {
    log.info("Getting exam matrix for assessment: {}", assessmentId);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    ExamMatrix matrix =
        examMatrixRepository
            .findByAssessmentIdAndNotDeleted(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrix.getId());
    return buildMatrixResponse(matrix, mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ExamMatrixResponse> getMyExamMatrices() {
    log.info("Getting my exam matrices");

    UUID currentUserId = getCurrentUserId();
    List<ExamMatrix> matrices = examMatrixRepository.findByTeacherIdAndNotDeleted(currentUserId);

    return matrices.stream()
        .map(
            m -> {
              List<ExamMatrixTemplateMapping> mappings =
                  templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(m.getId());
              return buildMatrixResponse(m, mappings);
            })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ExamMatrixResponse> getMyExamMatricesPaged(
      String search, MatrixStatus status, Pageable pageable) {
    log.info("Getting my exam matrices paged: search={}, status={}", search, status);

    UUID currentUserId = getCurrentUserId();
    String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
    MatrixStatus statusFilter = status;

    return examMatrixRepository
        .findByTeacherIdWithFilters(currentUserId, statusFilter, searchTerm, pageable)
        .map(
            m -> {
              List<ExamMatrixTemplateMapping> mappings =
                  templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(m.getId());
              return buildMatrixResponse(m, mappings);
            });
  }

  @Override
  @Transactional
  public void deleteExamMatrix(UUID matrixId) {
    log.info("Deleting exam matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    matrix.setDeletedAt(Instant.now());
    examMatrixRepository.save(matrix);
    log.info("Exam matrix soft-deleted: {}", matrixId);
  }



  // ── Template Mappings ───────────────────────────────────────────────────

  @Override
  @Transactional
  public TemplateMappingResponse addTemplateMapping(
      UUID matrixId, AddTemplateMappingRequest request) {
    log.info(
        "Adding template mapping to matrix {}: templateId={}", matrixId, request.getTemplateId());

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(request.getTemplateId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    ExamMatrixTemplateMapping mapping =
        ExamMatrixTemplateMapping.builder()
            .examMatrixId(matrixId)
            .templateId(request.getTemplateId())
            .cognitiveLevel(request.getCognitiveLevel())
            .questionCount(request.getQuestionCount())
            .pointsPerQuestion(request.getPointsPerQuestion())
            .build();

    mapping = templateMappingRepository.save(mapping);
    log.info("Template mapping added with id: {}", mapping.getId());
    return buildMappingResponse(mapping, template.getName());
  }

  @Override
  @Transactional
  public BatchTemplateMappingsResponse addTemplateMappings(
      UUID matrixId, BatchAddTemplateMappingsRequest request) {
    log.info(
        "Adding batch template mappings to matrix {}, count={}",
        matrixId,
        request.getMappings().size());

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    List<TemplateMappingResponse> addedMappings = new ArrayList<>();

    for (AddTemplateMappingRequest mappingRequest : request.getMappings()) {
      QuestionTemplate template =
          questionTemplateRepository
              .findByIdWithCreator(mappingRequest.getTemplateId())
              .filter(t -> t.getDeletedAt() == null)
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

      ExamMatrixTemplateMapping mapping =
          ExamMatrixTemplateMapping.builder()
              .examMatrixId(matrixId)
              .templateId(mappingRequest.getTemplateId())
              .cognitiveLevel(mappingRequest.getCognitiveLevel())
              .questionCount(mappingRequest.getQuestionCount())
              .pointsPerQuestion(mappingRequest.getPointsPerQuestion())
              .build();

      mapping = templateMappingRepository.save(mapping);
      addedMappings.add(buildMappingResponse(mapping, template.getName()));
      log.debug("Template mapping added with id: {}", mapping.getId());
    }

    log.info("Batch: {} template mappings added to matrix {}", addedMappings.size(), matrixId);

    return BatchTemplateMappingsResponse.builder()
        .totalMappingsAdded(addedMappings.size())
        .addedMappings(addedMappings)
        .message(
            String.format(
                "%d template mappings added successfully to matrix.", addedMappings.size()))
        .build();
  }

  @Override
  @Transactional
  public void removeTemplateMapping(UUID matrixId, UUID mappingId) {
    log.info("Removing template mapping {} from matrix {}", mappingId, matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    ExamMatrixTemplateMapping mapping =
        templateMappingRepository
            .findByIdAndExamMatrixId(mappingId, matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    templateMappingRepository.delete(mapping);
    log.info("Template mapping removed: {}", mappingId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemplateMappingResponse> getTemplateMappings(UUID matrixId) {
    log.info("Getting template mappings for matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);

    return mappings.stream()
        .map(
            m -> {
              String templateName =
                  questionTemplateRepository
                      .findById(m.getTemplateId())
                      .map(QuestionTemplate::getName)
                      .orElse(null);
              return buildMappingResponse(m, templateName);
            })
        .collect(Collectors.toList());
  }

  // ── Validation & Lifecycle ──────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public MatrixValidationReport validateMatrix(UUID matrixId) {
    log.info("Validating matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    List<ExamMatrixTemplateMapping> mappings =
      templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);
    List<ExamMatrixBankMapping> bankMappings =
      bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();




    // Traditional validation for template/fixed-count matrices
    if (mappings.isEmpty() && bankMappings.isEmpty()) {
      errors.add("Matrix has no mappings. Add at least one bank mapping.");
    }

    // Aggregate totals
    int templateQuestions = mappings.stream().mapToInt(ExamMatrixTemplateMapping::getQuestionCount).sum();
    int bankQuestions =
      bankMappings.stream()
        .mapToInt(this::getQuestionCountFromBankMapping)
        .sum();
    int totalQuestions = templateQuestions + bankQuestions;
    BigDecimal templateTotalPoints =
        mappings.stream()
            .map(
                m ->
                    m.getTotalPoints() != null
                        ? m.getTotalPoints()
                        : m.getPointsPerQuestion()
                            .multiply(BigDecimal.valueOf(m.getQuestionCount())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal bankTotalPoints =
      bankMappings.stream()
        .map(
          m -> {
            int count = getQuestionCountFromBankMapping(m);
            BigDecimal pointsPerQuestion =
              m.getPointsPerQuestion() != null ? m.getPointsPerQuestion() : BigDecimal.ONE;
            return pointsPerQuestion.multiply(BigDecimal.valueOf(count));
          })
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalPoints = templateTotalPoints.add(bankTotalPoints);

    Map<String, Integer> bankCoverageByDifficulty = new LinkedHashMap<>();
    boolean aiFallbackLikely = false;

    // BUG-1 & BUG-2 FIX: Build caches for rows and parts
    Map<UUID, ExamMatrixRow> rowById = examMatrixRowRepository
        .findByExamMatrixIdOrderByOrderIndex(matrixId).stream()
        .collect(Collectors.toMap(ExamMatrixRow::getId, r -> r));

    Map<Integer, ExamMatrixPart> partsCache = examMatrixPartRepository
        .findByExamMatrixIdOrderByPartNumber(matrixId).stream()
        .collect(Collectors.toMap(ExamMatrixPart::getPartNumber, p -> p));

    // The matrix is now a pure blueprint — bank is picked at generation time.
    // If the matrix carries a default bank we still sanity-check coverage
    // against it; otherwise we only validate the blueprint structure and
    // leave per-bank coverage to the generation step.
    UUID matrixDefaultBankId = matrix.getQuestionBankId();
    if (matrixDefaultBankId == null && !bankMappings.isEmpty()) {
      warnings.add(
          "Ngân hàng câu hỏi sẽ được chọn ở bước tạo đề. Việc kiểm tra số lượng "
              + "câu sẵn có sẽ chạy lại sau khi bạn chọn ngân hàng.");
    }

    for (ExamMatrixBankMapping bankMapping : bankMappings) {
      int required = getQuestionCountFromBankMapping(bankMapping);
      if (required < 0) {
        errors.add("Bank mapping has invalid questionCount value.");
        continue;
      }
      if (required == 0) {
        continue;
      }

      String coverageKey =
          bankMapping.getCognitiveLevel() != null
              ? bankMapping.getCognitiveLevel().name()
              : "UNSPECIFIED";
      bankCoverageByDifficulty.merge(
          coverageKey,
          required,
          (left, right) -> (left == null ? 0 : left) + (right == null ? 0 : right));

      // BUG-1 FIX: Use chapter+type-aware count query
      ExamMatrixRow row = rowById.get(bankMapping.getMatrixRowId());
      ExamMatrixPart part = partsCache.get(bankMapping.getPartNumber());
      
      if (row == null || row.getChapterId() == null) {
        warnings.add(
            String.format(
                "Mapping %s has no associated row or chapter - skipping validation.",
                bankMapping.getId()));
        continue;
      }
      
      if (part == null) {
        warnings.add(
            String.format(
                "Mapping %s has no associated part (partNumber=%d) - skipping validation.",
                bankMapping.getId(),
                bankMapping.getPartNumber()));
        continue;
      }

      // Skip per-bank availability check when no default bank is set on the
      // matrix — the FE/BE will recompute it once the user picks a bank.
      if (matrixDefaultBankId == null) {
        continue;
      }

      long approvedAvailable =
          questionRepository.countApprovedByBankAndChapterAndCognitiveAndType(
              matrixDefaultBankId,
              row.getChapterId(),
              bankMapping.getCognitiveLevel().name(),
              part.getQuestionType().name());

      if (approvedAvailable < required) {
        aiFallbackLikely = true;
        // BUG-2 FIX: Human-readable error messages with chapter name, part type, cognitive level
        warnings.add(
            String.format(
                "Chương '%s' — %s — %s: cần %d câu, ngân hàng chỉ có %d.",
                row.getChapterName() != null ? row.getChapterName() : "Không xác định",
                part.getQuestionType().name(),
                bankMapping.getCognitiveLevel().name(),
                required,
                approvedAvailable));
      }
    }

    if (totalQuestions == 0) {
      errors.add("Total question count across all mappings must be greater than 0.");
    }
    if (totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Total points across all mappings must be greater than 0.");
    }

    Integer totalQuestionsTarget = matrix.getTotalQuestionsTarget();
    BigDecimal totalPointsTarget = matrix.getTotalPointsTarget();

    boolean questionsMatchTarget = true;
    if (totalQuestionsTarget != null) {
      if (totalQuestionsTarget <= 0) {
        errors.add("Total questions target must be greater than 0.");
        questionsMatchTarget = false;
      } else {
        questionsMatchTarget = totalQuestions == totalQuestionsTarget;
        if (!questionsMatchTarget) {
          errors.add(
              String.format(
                  "Total questions mismatch: target=%d, actual=%d.",
                  totalQuestionsTarget, totalQuestions));
        }
      }
    } else {
      warnings.add(
          "Matrix does not define totalQuestionsTarget; question-count target matching is skipped.");
    }

    boolean pointsMatchTarget = true;
    if (totalPointsTarget != null) {
      if (totalPointsTarget.compareTo(BigDecimal.ZERO) <= 0) {
        errors.add("Total points target must be greater than 0.");
        pointsMatchTarget = false;
      } else {
        pointsMatchTarget = totalPoints.compareTo(totalPointsTarget) == 0;
        if (!pointsMatchTarget) {
          errors.add(
              String.format(
                  "Total points mismatch: target=%s, actual=%s.",
                  totalPointsTarget.toPlainString(), totalPoints.toPlainString()));
        }
      }
    } else {
      warnings.add("Matrix does not define totalPointsTarget; points target matching is skipped.");
    }

    // Cognitive level coverage
    Map<String, Integer> cognitiveLevelCoverage = new LinkedHashMap<>();
    mappings.forEach(
      m ->
        cognitiveLevelCoverage.merge(
          m.getCognitiveLevel().name(),
          m.getQuestionCount(),
          (left, right) -> (left == null ? 0 : left) + (right == null ? 0 : right)));
    bankMappings.forEach(
      m -> {
        int count = getQuestionCountFromBankMapping(m);
          cognitiveLevelCoverage.merge(
              m.getCognitiveLevel().name(),
              count,
              (left, right) -> (left == null ? 0 : left) + (right == null ? 0 : right));
      });

    long distinctLevels = cognitiveLevelCoverage.size();
    boolean allCognitiveLevelsCovered = distinctLevels >= 3;
    if (!allCognitiveLevelsCovered) {
      warnings.add(
          String.format(
              "Only %d cognitive level(s) covered (recommended: at least 3).", distinctLevels));
    }

    boolean canApprove = errors.isEmpty();

    return MatrixValidationReport.builder()
        .canApprove(canApprove)
        .errors(errors)
        .warnings(warnings)
        .totalTemplateMappings(mappings.size())
      .totalBankMappings(bankMappings.size())
        .totalQuestions(totalQuestions)
        .totalPoints(totalPoints)
        .totalQuestionsTarget(totalQuestionsTarget)
        .totalPointsTarget(totalPointsTarget)
        .cognitiveLevelCoverage(cognitiveLevelCoverage)
      .bankCoverageByDifficulty(bankCoverageByDifficulty)
        .questionsMatchTarget(questionsMatchTarget)
        .pointsMatchTarget(pointsMatchTarget)
        .allCognitiveLevelsCovered(allCognitiveLevelsCovered)
      .aiFallbackLikely(aiFallbackLikely)
        .build();
  }

  @Override
  @Transactional
  public ExamMatrixResponse approveMatrix(UUID matrixId) {
    log.info("Approving matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    MatrixValidationReport report = validateMatrix(matrixId);
    if (!report.isCanApprove()) {
      log.error("Matrix validation failed: {}", report.getErrors());
      throw new AppException(ErrorCode.MATRIX_VALIDATION_FAILED);
    }

    matrix.setStatus(MatrixStatus.APPROVED);
    matrix = examMatrixRepository.save(matrix);

    log.info("Matrix approved: {}", matrixId);
    return buildMatrixResponse(
        matrix, templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId));
  }

  @Override
  @Transactional
  public void lockMatrix(UUID matrixId) {
    log.info("Locking matrix: {}", matrixId);

    ExamMatrix matrix =
        examMatrixRepository
            .findByIdAndNotDeleted(matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    if (matrix.getStatus() == MatrixStatus.LOCKED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_LOCKED);
    }
    if (matrix.getStatus() != MatrixStatus.APPROVED) {
      throw new AppException(ErrorCode.MATRIX_NOT_APPROVED);
    }

    matrix.setStatus(MatrixStatus.LOCKED);
    examMatrixRepository.save(matrix);
    log.info("Matrix locked: {}", matrixId);
  }

  @Override
  @Transactional
  public ExamMatrixResponse resetMatrix(UUID matrixId) {
    log.info("Resetting matrix to DRAFT: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    if (matrix.getStatus() != MatrixStatus.APPROVED) {
      throw new AppException(ErrorCode.MATRIX_NOT_APPROVED_FOR_RESET);
    }

    matrix.setStatus(MatrixStatus.DRAFT);
    matrix = examMatrixRepository.save(matrix);

    log.info("Matrix {} reset to DRAFT", matrixId);
    return buildMatrixResponse(
        matrix, templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId));
  }

  // ── Question Generation (template-mapping based) ────────────────────────

  @Override
  @Transactional(readOnly = true)
  public MatchingTemplatesResponse listMatchingTemplates(
      UUID matrixId, String q, int page, int size, boolean onlyMine, boolean publicOnly) {

    log.info(
        "Listing matching templates for matrix {}, q={}, page={}, size={}, onlyMine={}, publicOnly={}",
        matrixId,
        q,
        page,
        size,
        onlyMine,
        publicOnly);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    UUID currentUserId = getCurrentUserId();
    String searchTerm = (q != null && !q.isBlank()) ? q.trim() : null;

    // Derive aggregate requirements from all mappings: most common cognitiveLevel
    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);
    CognitiveLevel dominantLevel =
        mappings.stream()
            .collect(
                Collectors.groupingBy(
                    ExamMatrixTemplateMapping::getCognitiveLevel, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

    int totalQuestionCount =
        mappings.stream().mapToInt(ExamMatrixTemplateMapping::getQuestionCount).sum();

    // Query templates
    var pageRequest = PageRequest.of(Math.max(page, 0), size > 0 ? size : 20);
    var templatePage =
        questionTemplateRepository.findMatchingTemplatesForCell(
            currentUserId, onlyMine, publicOnly, null, dominantLevel, searchTerm, pageRequest);

    // Score and sort
    List<QuestionTemplate> candidates = new ArrayList<>(templatePage.getContent());
    candidates.sort(
        Comparator.comparingInt(
                (QuestionTemplate t) -> computeRelevanceScore(t, null, dominantLevel))
            .reversed());

    MatchingTemplatesResponse.MappingRequirementsInfo requirementsInfo =
        MatchingTemplatesResponse.MappingRequirementsInfo.builder()
            .matrixId(matrixId)
            .cognitiveLevel(dominantLevel)
            .questionCount(totalQuestionCount)
            .build();

    List<MatchingTemplatesResponse.TemplateItem> items =
        candidates.stream()
            .map(t -> mapToTemplateItem(t, currentUserId, null, dominantLevel))
            .collect(Collectors.toList());

    String hint =
        items.isEmpty()
            ? "No matching templates found. Create a new template or adjust filters."
            : null;

    return MatchingTemplatesResponse.builder()
        .mappingRequirements(requirementsInfo)
        .totalTemplatesFound((int) templatePage.getTotalElements())
        .templates(items)
        .hint(hint)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public PreviewCandidatesResponse generatePreview(
      UUID matrixId, UUID mappingId, GeneratePreviewRequest request) {

    log.info(
        "Generating preview for matrixId={}, mappingId={}, templateId={}, count={}",
        matrixId,
        mappingId,
        request.getTemplateId(),
        request.getCount());

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    ExamMatrixTemplateMapping mapping =
        templateMappingRepository
            .findByIdAndExamMatrixId(mappingId, matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    if (!mapping.getTemplateId().equals(request.getTemplateId())) {
      throw new AppException(ErrorCode.TEMPLATE_MAPPING_TEMPLATE_MISMATCH);
    }

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(request.getTemplateId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (template.getStatus() != null && "DRAFT".equalsIgnoreCase(template.getStatus().name())) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    List<String> warnings = new ArrayList<>();

    if (matrix.getStatus() == MatrixStatus.APPROVED || matrix.getStatus() == MatrixStatus.LOCKED) {
      warnings.add(
          String.format(
              "Matrix is currently %s. This preview is for exploration only. "
                  + "Reset the matrix to DRAFT before finalizing.",
              matrix.getStatus()));
    }

    // Difficulty mismatch warning
    QuestionDifficulty requestedDifficulty = request.getDifficulty();
    if (requestedDifficulty != null) {
      warnings.add(
          String.format(
              "Using requested difficulty override '%s' for preview only.", requestedDifficulty));
    }

    int targetCount = request.getCount();
    int maxAttempts = targetCount * 10;
    List<PreviewCandidatesResponse.CandidateQuestion> candidates = new ArrayList<>();
    Set<String> seenTexts = new HashSet<>();

    long baseSeed = request.getSeed() != null ? request.getSeed() : System.currentTimeMillis();
    int attemptsMade = 0;
    int sampleIndex = 0;

    while (candidates.size() < targetCount && attemptsMade < maxAttempts) {
      attemptsMade++;
      try {
        int effectiveIndex = (int) ((baseSeed + sampleIndex) % Integer.MAX_VALUE);
        GeneratedQuestionSample sample =
            aiEnhancementService.generateQuestion(template, effectiveIndex);
        sampleIndex++;

        if (sample == null || sample.getQuestionText() == null) continue;
        if (sample.getQuestionText().startsWith("[LLM generation failed]")) {
          log.warn("LLM generation failed on attempt {}, skipping", attemptsMade);
          continue;
        }

        String textKey = sample.getQuestionText().trim().toLowerCase();
        if (seenTexts.contains(textKey)) continue;
        seenTexts.add(textKey);

        // Difficulty filter — relax after half of max attempts
        if (requestedDifficulty != null
            && sample.getCalculatedDifficulty() != null
            && !requestedDifficulty.equals(sample.getCalculatedDifficulty())) {
          if (attemptsMade < maxAttempts / 2) continue;
          warnings.add(
              String.format(
                  "Could not generate enough '%s' questions; included '%s' candidate(s) to meet count.",
                  requestedDifficulty, sample.getCalculatedDifficulty()));
        }

        candidates.add(
            PreviewCandidatesResponse.CandidateQuestion.builder()
                .index(candidates.size() + 1)
                .questionText(sample.getQuestionText())
                .options(sample.getOptions())
                .correctAnswerKey(sample.getCorrectAnswer())
                .usedParameters(sample.getUsedParameters())
                .answerCalculation(sample.getAnswerCalculation())
                .calculatedDifficulty(sample.getCalculatedDifficulty())
                .explanation(sample.getExplanation())
                .build());

      } catch (Exception e) {
        log.warn("Error generating candidate on attempt {}: {}", attemptsMade, e.getMessage());
      }
    }

    if (candidates.size() < targetCount) {
      warnings.add(
          String.format(
              "Only %d of %d requested questions could be generated. "
                  + "Template parameter ranges may be too narrow.",
              candidates.size(), targetCount));
    }

    String templateName =
        questionTemplateRepository
            .findById(request.getTemplateId())
            .map(QuestionTemplate::getName)
            .orElse(null);

    PreviewCandidatesResponse.MappingInfo mappingInfo =
        PreviewCandidatesResponse.MappingInfo.builder()
            .templateMappingId(mapping.getId())
            .templateId(mapping.getTemplateId())
            .templateName(templateName)
            .cognitiveLevel(mapping.getCognitiveLevel())
            .questionCount(mapping.getQuestionCount())
            .build();

    log.info(
        "Preview generated: {}/{} candidates for mappingId={}",
        candidates.size(),
        targetCount,
        mappingId);

    return PreviewCandidatesResponse.builder()
        .templateId(template.getId())
        .templateName(template.getName())
        .templateMappingId(mappingId)
        .matrixId(matrixId)
        .requestedCount(targetCount)
        .generatedCount(candidates.size())
        .mappingRequirements(mappingInfo)
        .candidates(candidates)
        .warnings(warnings.isEmpty() ? null : warnings)
        .build();
  }

  @Override
  @Transactional
  public FinalizePreviewResponse finalizePreview(
      UUID matrixId, UUID mappingId, FinalizePreviewRequest request) {

    log.info(
        "Finalizing preview for matrixId={}, mappingId={}, templateId={}, count={}, replaceExisting={}",
        matrixId,
        mappingId,
        request.getTemplateId(),
        request.getQuestions().size(),
        request.getReplaceExisting());

    // ── 1. Validate matrix + ownership ──────────────────────────────────
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    // ── 2. Validate mapping belongs to matrix ────────────────────────────
    ExamMatrixTemplateMapping mapping =
        templateMappingRepository
            .findByIdAndExamMatrixId(mappingId, matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    if (!mapping.getTemplateId().equals(request.getTemplateId())) {
      throw new AppException(ErrorCode.TEMPLATE_MAPPING_TEMPLATE_MISMATCH);
    }

    // ── 3. Validate template ─────────────────────────────────────────────
    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(request.getTemplateId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (template.getStatus() != null && "DRAFT".equalsIgnoreCase(template.getStatus().name())) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    UUID currentUserId = getCurrentUserId();
    List<String> warnings = new ArrayList<>();

    // ── 4. Replace existing assessment questions for this mapping if requested ──
    if (Boolean.TRUE.equals(request.getReplaceExisting())) {
      log.info(
          "replaceExisting=true: removing existing assessment questions for mappingId={}",
          mappingId);
      // Find all AssessmentQuestion rows linked to this template mapping and remove them
      // across any assessment that references this matrix
      assessmentRepository
          .findByExamMatrixIdAndNotDeleted(matrixId)
          .forEach(
              a -> {
                List<AssessmentQuestion> existing =
                    assessmentQuestionRepository
                        .findByAssessmentIdOrderByOrderIndex(a.getId())
                        .stream()
                        .filter(aq -> mappingId.equals(aq.getMatrixTemplateMappingId()))
                        .toList();
                if (!existing.isEmpty()) {
                  List<UUID> qIds =
                      existing.stream()
                          .map(AssessmentQuestion::getQuestionId)
                          .collect(Collectors.toList());
                  assessmentQuestionRepository.deleteByAssessmentIdAndQuestionIdIn(a.getId(), qIds);
                }
              });
    }

    // ── 5. Per-question validation ───────────────────────────────────────
    Set<String> batchTexts = new HashSet<>();
    List<FinalizePreviewRequest.QuestionItem> validItems = new ArrayList<>();

    for (int i = 0; i < request.getQuestions().size(); i++) {
      FinalizePreviewRequest.QuestionItem item = request.getQuestions().get(i);
      String label = "Question[" + (i + 1) + "]: ";

      if (item.getQuestionText() == null || item.getQuestionText().isBlank()) {
        warnings.add(label + "skipped – questionText is blank.");
        continue;
      }

      String textKey = item.getQuestionText().trim().toLowerCase();
      if (batchTexts.contains(textKey)) {
        warnings.add(label + "skipped – duplicate questionText within this request.");
        continue;
      }

      // MCQ validation
      if (item.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
        Map<String, String> opts = item.getOptions();
        if (opts == null
            || !opts.keySet().containsAll(Set.of("A", "B", "C", "D"))
            || opts.size() != 4) {
          throw new AppException(ErrorCode.MCQ_INVALID_OPTIONS);
        }
        long distinct = opts.values().stream().map(String::trim).distinct().count();
        if (distinct < 4) throw new AppException(ErrorCode.MCQ_INVALID_OPTIONS);
        String ca = item.getCorrectAnswer();
        if (ca == null || !Set.of("A", "B", "C", "D").contains(ca.toUpperCase())) {
          throw new AppException(ErrorCode.MCQ_INVALID_CORRECT_OPTION);
        }
      }

      // Cognitive level mismatch warning
      if (mapping.getCognitiveLevel() != null
          && item.getCognitiveLevel() != null
          && !mapping.getCognitiveLevel().equals(item.getCognitiveLevel())) {
        warnings.add(
            label
                + "cognitiveLevel '"
                + item.getCognitiveLevel()
                + "' differs from mapping cognitiveLevel '"
                + mapping.getCognitiveLevel()
                + "'.");
      }

      batchTexts.add(textKey);
      validItems.add(item);
    }

    if (validItems.isEmpty()) {
      throw new AppException(ErrorCode.FINALIZE_EMPTY_QUESTIONS);
    }

    // ── 6. Overflow check (append mode) ─────────────────────────────────
    if (!Boolean.TRUE.equals(request.getReplaceExisting())) {
      long currentCount =
          assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId).stream()
              .mapToLong(
                  a ->
                      assessmentQuestionRepository.countByAssessmentIdAndMatrixTemplateMappingId(
                          a.getId(), mappingId))
              .max()
              .orElse(0L);
      long afterAdd = currentCount + validItems.size();
      if (afterAdd > mapping.getQuestionCount()) {
        warnings.add(
            String.format(
                "Appending %d question(s) to existing %d will exceed mapping target of %d.",
                validItems.size(), currentCount, mapping.getQuestionCount()));
      }
    }

    // ── 7. Persist questions ─────────────────────────────────────────────
    List<UUID> savedQuestionIds = new ArrayList<>();

    for (FinalizePreviewRequest.QuestionItem item : validItems) {
      Map<String, Object> metadata =
          item.getGenerationMetadata() != null
              ? new HashMap<>(item.getGenerationMetadata())
              : new HashMap<>();
      metadata.put("generatedAt", Instant.now().toString());
      metadata.put("templateId", template.getId().toString());
      metadata.put("templateName", template.getName());
      metadata.put("finalizedBy", currentUserId.toString());

      Map<String, Object> optionsJsonb =
          item.getOptions() != null ? new HashMap<>(item.getOptions()) : null;

      Question question =
          Question.builder()
              .questionBankId(request.getQuestionBankId())
              .chapterId(template.getChapterId())
              .lessonId(template.getLessonId())
              .questionType(item.getQuestionType())
              .questionText(item.getQuestionText())
              .options(optionsJsonb)
              .correctAnswer(item.getCorrectAnswer())
              .explanation(item.getExplanation())
              .points(request.getPointsPerQuestion())
              .cognitiveLevel(item.getCognitiveLevel())
              .tags(item.getTags())
              .templateId(template.getId())
              .generationMetadata(metadata)
              .build();
      question.setCreatedBy(currentUserId);

      question = questionRepository.save(question);
      savedQuestionIds.add(question.getId());

      template.incrementUsageCount();
    }

    questionTemplateRepository.save(template);

    // ── 8. Write assessment_questions for all assessments using this matrix ──
    List<Assessment> linkedAssessments =
        assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId);

    for (Assessment assessment : linkedAssessments) {
      Integer maxOrder = assessmentQuestionRepository.findMaxOrderIndex(assessment.getId());
      int orderIndex = (maxOrder != null ? maxOrder : 0) + 1;

      for (UUID questionId : savedQuestionIds) {
        AssessmentQuestion aq =
            AssessmentQuestion.builder()
                .assessmentId(assessment.getId())
                .questionId(questionId)
                .orderIndex(orderIndex++)
                .pointsOverride(request.getPointsPerQuestion())
                .matrixTemplateMappingId(mappingId)
                .build();
        assessmentQuestionRepository.save(aq);
      }
    }

    // ── 9. Count current questions for this mapping ──────────────────────
    long currentMappingQuestionCount =
        linkedAssessments.isEmpty()
            ? savedQuestionIds.size()
            : assessmentQuestionRepository
                .findByAssessmentIdOrderByOrderIndex(linkedAssessments.getFirst().getId())
                .stream()
                .filter(aq -> mappingId.equals(aq.getMatrixTemplateMappingId()))
                .count();

    log.info(
        "Finalize complete: {} questions saved for mappingId={}",
        savedQuestionIds.size(),
        mappingId);

    return FinalizePreviewResponse.builder()
        .templateMappingId(mappingId)
        .matrixId(matrixId)
        .templateId(template.getId())
        .requestedCount(request.getQuestions().size())
        .savedCount(savedQuestionIds.size())
        .questionIds(savedQuestionIds)
        .currentMappingQuestionCount((int) currentMappingQuestionCount)
        .mappingTargetCount(mapping.getQuestionCount())
        .warnings(warnings.isEmpty() ? null : warnings)
        .build();
  }

  // ── Structured Matrix Builder ───────────────────────────────────────────

  @Override
  @Transactional
  public ExamMatrixTableResponse buildMatrix(BuildExamMatrixRequest request) {
    log.info("Building structured exam matrix: name={}", request.getName());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);

    // Matrix is now a pure blueprint — bank is picked at generation time.
    // If the caller still provides a default bank we validate accessibility,
    // but it is no longer required at build time.
    if (request.getQuestionBankId() != null) {
      QuestionBank bank =
          questionBankRepository
              .findByIdAndNotDeleted(request.getQuestionBankId())
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));
      if (!bank.getTeacherId().equals(currentUserId)
          && !Boolean.TRUE.equals(bank.getIsPublic())
          && !hasRole(PredefinedRole.ADMIN_ROLE)) {
        throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
      }
    }

    ExamMatrix matrix =
        ExamMatrix.builder()
            .teacherId(currentUserId)
            .gradeLevel(request.getGradeLevel())
            .questionBankId(request.getQuestionBankId())
            .name(request.getName())
            .description(request.getDescription())
            .isReusable(request.getIsReusable() != null ? request.getIsReusable() : false)
            .totalQuestionsTarget(request.getTotalQuestionsTarget())
            .totalPointsTarget(request.getTotalPointsTarget())
        .numberOfParts(request.getNumberOfParts() != null ? request.getNumberOfParts() : 1)
            .status(MatrixStatus.DRAFT)
            .build();

    matrix = examMatrixRepository.save(matrix);
    final UUID matrixId = matrix.getId();

    // Persist rows and their cells
    int globalOrder = 1;
    for (MatrixRowRequest rowSpec : request.getRows()) {
      persistRow(matrixId, rowSpec, globalOrder++);
    }

    log.info("Structured matrix built: id={}", matrixId);
    return buildTableResponse(matrix);
  }

  @Override
  @Transactional
  public ExamMatrixTableResponse buildSimpleMatrix(BuildSimpleExamMatrixRequest request) {
    log.info(
        "Building simple exam matrix: name={}, grade={}, chapters={}",
        request.getName(),
        request.getGradeLevel(),
        request.getChapters().size());

    // Matrix is now a pure blueprint — bank is optional at build time. When
    // a default bank IS provided we sanity-check that its grade matches the
    // matrix grade so legacy callers fail fast instead of blowing up later.
    if (request.getQuestionBankId() != null) {
      QuestionBank bank =
          questionBankRepository
              .findByIdAndNotDeleted(request.getQuestionBankId())
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));
      if (bank.getSchoolGradeId() != null && bank.getSchoolGrade() != null) {
        Integer bankGrade = bank.getSchoolGrade().getGradeLevel();
        if (bankGrade != null && !bankGrade.equals(request.getGradeLevel())) {
          throw new AppException(ErrorCode.BANK_GRADE_MISMATCH);
        }
      }
    }

    Set<UUID> seenChapterIds = new HashSet<>();
    List<MatrixRowRequest> rows = new ArrayList<>();
    int orderIndex = 1;
    for (BuildSimpleExamMatrixRequest.ChapterCognitiveCounts spec : request.getChapters()) {
      if (!seenChapterIds.add(spec.getChapterId())) {
        // Happy-case: one row per chapter, deduplicate caller mistakes early.
        throw new AppException(ErrorCode.MATRIX_VALIDATION_FAILED);
      }

      Chapter chapter =
          chapterRepository
              .findById(spec.getChapterId())
              .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));

      BigDecimal pointsPerQuestion =
          spec.getPointsPerQuestion() != null
              ? spec.getPointsPerQuestion()
              : request.getPointsPerQuestion();

      List<MatrixCellRequest> cells = new ArrayList<>(4);
      addCellIfPositive(cells, CognitiveLevel.NHAN_BIET, spec.getNb(), pointsPerQuestion);
      addCellIfPositive(cells, CognitiveLevel.THONG_HIEU, spec.getTh(), pointsPerQuestion);
      addCellIfPositive(cells, CognitiveLevel.VAN_DUNG, spec.getVd(), pointsPerQuestion);
      addCellIfPositive(cells, CognitiveLevel.VAN_DUNG_CAO, spec.getVdc(), pointsPerQuestion);

      if (cells.isEmpty()) {
        // Skip rows where the caller asked for zero of every level — keeps the
        // resulting matrix tidy instead of carrying empty rows that fail validation.
        continue;
      }

      rows.add(
          MatrixRowRequest.builder()
              .chapterId(chapter.getId())
              .questionTypeName(chapter.getTitle())
              .orderIndex(orderIndex++)
              .cells(cells)
              .build());
    }

    if (rows.isEmpty()) {
      throw new AppException(ErrorCode.MATRIX_VALIDATION_FAILED);
    }

    int totalQuestions =
        rows.stream()
            .flatMap(r -> r.getCells().stream())
            .mapToInt(MatrixCellRequest::getQuestionCount)
            .sum();

    BuildExamMatrixRequest expanded =
        BuildExamMatrixRequest.builder()
            .name(request.getName())
            .description(request.getDescription())
            .questionBankId(request.getQuestionBankId())
            .gradeLevel(request.getGradeLevel())
            .isReusable(request.getIsReusable())
            .totalQuestionsTarget(totalQuestions)
            .numberOfParts(1)
            .rows(rows)
            .build();

    return buildMatrix(expanded);
  }

  private void addCellIfPositive(
      List<MatrixCellRequest> cells,
      CognitiveLevel level,
      Integer count,
      BigDecimal pointsPerQuestion) {
    if (count == null || count <= 0) return;
    cells.add(
        MatrixCellRequest.builder()
            .partNumber(1)
            .cognitiveLevel(level)
            .questionCount(count)
            .pointsPerQuestion(pointsPerQuestion)
            .build());
  }

  @Override
  @Transactional(readOnly = true)
  public ExamMatrixTableResponse getMatrixTable(UUID matrixId) {
    log.info("Fetching matrix table: {}", matrixId);
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    return buildTableResponse(matrix);
  }

  @Override
  @Transactional
  public ExamMatrixTableResponse addMatrixRow(UUID matrixId, MatrixRowRequest rowRequest) {
    log.info("Adding row to matrix: {}", matrixId);
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    List<ExamMatrixRow> existing =
        examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId);
    int nextOrder =
        existing.stream()
                .mapToInt(r -> r.getOrderIndex() != null ? r.getOrderIndex() : 0)
                .max()
                .orElse(0)
            + 1;

    validateSchoolLevelConsistency(matrix, rowRequest.getChapterId());
    persistRow(matrixId, rowRequest, nextOrder);
    return buildTableResponse(matrix);
  }

  @Override
  @Transactional
  public ExamMatrixTableResponse removeMatrixRow(UUID matrixId, UUID rowId) {
    log.info("Removing row {} from matrix {}", rowId, matrixId);
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    ExamMatrixRow row =
        examMatrixRowRepository
            .findById(rowId)
            .filter(r -> r.getExamMatrixId().equals(matrixId))
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_ROW_NOT_FOUND));

    // Manually delete bank mappings first to avoid foreign key constraint violation
    // This works regardless of whether the CASCADE DELETE migration has been applied
    bankMappingRepository.deleteByMatrixRowId(rowId);
    
    // Delete template mappings (old system)
    templateMappingRepository.deleteByMatrixRowId(rowId);
    
    // Now delete the row
    examMatrixRowRepository.delete(row);
    examMatrixRowRepository.flush();
    
    return buildTableResponse(matrix);
  }

  @Override
  @Transactional
  public ExamMatrixTableResponse upsertMatrixRowCells(
      UUID matrixId, UUID rowId, List<MatrixCellRequest> cells) {
    log.info("Upserting cells for row {} in matrix {}", rowId, matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    ExamMatrixRow row =
        examMatrixRowRepository
            .findById(rowId)
            .filter(r -> r.getExamMatrixId().equals(matrixId))
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_ROW_NOT_FOUND));

    upsertRowCells(matrixId, row, cells, null);
    return buildTableResponse(matrix);
  }

  @Override
  @Transactional
  public ExamMatrixTableResponse batchUpsertMatrixRowCells(
      UUID matrixId, BatchUpsertMatrixRowCellsRequest request) {
    log.info("Batch upserting row cells in matrix {} with {} rows", matrixId, request.getRows().size());

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    Map<UUID, ExamMatrixRow> rowsById =
        examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId).stream()
            .collect(Collectors.toMap(ExamMatrixRow::getId, r -> r));

    for (BatchUpsertMatrixRowCellsRequest.RowCellsItem item : request.getRows()) {
      ExamMatrixRow row = rowsById.get(item.getRowId());
      if (row == null) {
        throw new AppException(ErrorCode.EXAM_MATRIX_ROW_NOT_FOUND);
      }
      upsertRowCells(matrixId, row, item.getCells(), null);
    }

    return buildTableResponse(matrix);
  }

  // ── Private helpers ─────────────────────────────────────────────────────

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return UUID.fromString(jwtAuth.getToken().getSubject());
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  private boolean hasRole(String roleName) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    String prefixed = "ROLE_" + roleName;
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(a -> a.equals(roleName) || a.equals(prefixed));
  }

  private void validateTeacherRole(UUID userId) {
    if (!hasRole(PredefinedRole.TEACHER_ROLE) && !hasRole(PredefinedRole.ADMIN_ROLE)) {
      User user =
          userRepository
              .findByIdWithRoles(userId)
              .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
      boolean isTeacher =
          user.getRoles().stream()
              .anyMatch(
                  role ->
                      PredefinedRole.TEACHER_ROLE.equals(role.getName())
                          || PredefinedRole.ADMIN_ROLE.equals(role.getName()));
      if (!isTeacher) {
        throw new AppException(ErrorCode.NOT_A_TEACHER);
      }
    }
  }

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId) && !hasRole(PredefinedRole.ADMIN_ROLE)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }
  }

  private ExamMatrix loadMatrixOrThrow(UUID matrixId) {
    return examMatrixRepository
        .findByIdAndNotDeleted(matrixId)
        .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));
  }

  private void validateNotApprovedOrLocked(ExamMatrix matrix) {
    if (matrix.getStatus() == MatrixStatus.LOCKED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_LOCKED);
    }
    if (matrix.getStatus() == MatrixStatus.APPROVED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_APPROVED);
    }
  }

  /**
   * BE-4: Creates or replaces parts for a matrix.
   * Called on both create and update flows.
   * Performance: parts are cached per-request via returned map.
   */
  private Map<Integer, ExamMatrixPart> createOrReplaceParts(
      UUID matrixId, List<ExamMatrixPartRequest> parts, Integer numberOfParts) {

    // 1. Resolve effective parts
    List<ExamMatrixPartRequest> effectiveParts;
    if (parts != null && !parts.isEmpty()) {
      if (parts.size() > 3) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
      }
      effectiveParts = parts;
    } else {
      int n = (numberOfParts != null) ? numberOfParts : 1;
      QuestionType[] defaults = {QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE, QuestionType.SHORT_ANSWER};
      effectiveParts = new ArrayList<>();
      for (int i = 1; i <= n; i++) {
        effectiveParts.add(ExamMatrixPartRequest.builder()
            .partNumber(i)
            .questionType(defaults[i-1])
            .build());
      }
    }

    // 2. Delete existing parts (cascade sets partId=NULL on cells)
    examMatrixPartRepository.deleteByExamMatrixId(matrixId);

    // 3. Insert new parts + build cache
    Map<Integer, ExamMatrixPart> cache = new HashMap<>();
    for (ExamMatrixPartRequest req : effectiveParts) {
      ExamMatrixPart part = examMatrixPartRepository.save(
          ExamMatrixPart.builder()
              .examMatrixId(matrixId)
              .partNumber(req.getPartNumber())
              .questionType(req.getQuestionType())
              .name(req.getName())
              .build());
      cache.put(part.getPartNumber(), part);
    }

    return cache; // Reuse in upsert — no re-query needed
  }

  /**
   * BE-8: Build part responses for API response.
   * MUST be called in BOTH buildTableResponse() AND buildMatrixResponse().
   */
  private List<ExamMatrixPartResponse> buildPartResponses(UUID matrixId) {
    return examMatrixPartRepository.findByExamMatrixIdOrderByPartNumber(matrixId).stream()
        .map(p -> ExamMatrixPartResponse.builder()
            .id(p.getId())
            .partNumber(p.getPartNumber())
            .questionType(p.getQuestionType())
            .name(p.getName())
            .build())
        .collect(Collectors.toList());
  }



  private ExamMatrixResponse buildMatrixResponse(
      ExamMatrix matrix, List<ExamMatrixTemplateMapping> mappings) {

    String teacherName =
        userRepository.findById(matrix.getTeacherId()).map(User::getFullName).orElse("Unknown");

    List<BankMappingResponse> bankMappingResponses =
      bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrix.getId()).stream()
        .map(this::buildBankMappingResponse)
        .collect(Collectors.toList());

    UUID subjectId = matrix.getSubjectId();
    String subjectName = null;
    if (subjectId != null) {
      subjectName = subjectRepository.findById(subjectId).map(Subject::getName).orElse(null);
    }

    UUID questionBankId = matrix.getQuestionBankId();
    String questionBankName = null;
    if (questionBankId != null) {
      questionBankName = questionBankRepository.findById(questionBankId)
          .map(QuestionBank::getName)
          .orElse(null);
    }

    // BE-8: Build parts responses (ALWAYS populated)
    List<ExamMatrixPartResponse> partResponses = buildPartResponses(matrix.getId());

    return ExamMatrixResponse.builder()
        .id(matrix.getId())
        .teacherId(matrix.getTeacherId())
        .teacherName(teacherName)
        .name(matrix.getName())
        .description(matrix.getDescription())
        .isReusable(matrix.getIsReusable())
        .numberOfParts(matrix.getNumberOfParts())
        .parts(partResponses)
        .gradeLevel(matrix.getGradeLevel())
        .subjectId(subjectId)
        .subjectName(subjectName)
        .questionBankId(questionBankId)
        .questionBankName(questionBankName)
        .totalQuestionsTarget(matrix.getTotalQuestionsTarget())
        .totalPointsTarget(matrix.getTotalPointsTarget())
        .status(matrix.getStatus())
        .templateMappingCount(0)
        .templateMappings(Collections.emptyList())
        .bankMappingCount(bankMappingResponses.size())
        .bankMappings(bankMappingResponses)
        .createdAt(matrix.getCreatedAt())
        .updatedAt(matrix.getUpdatedAt())
        .build();
  }

  private BankMappingResponse buildBankMappingResponse(ExamMatrixBankMapping mapping) {
    // Get bank ID from the matrix since mappings no longer store it directly
    ExamMatrix matrix = examMatrixRepository.findById(mapping.getExamMatrixId()).orElse(null);
    UUID bankId = matrix != null ? matrix.getQuestionBankId() : null;
    String bankName = bankId != null 
        ? questionBankRepository.findById(bankId).map(QuestionBank::getName).orElse(null)
        : null;
    
    return BankMappingResponse.builder()
        .id(mapping.getId())
        .examMatrixId(mapping.getExamMatrixId())
        .questionBankName(bankName)
        .matrixRowId(mapping.getMatrixRowId())
        .questionCount(getQuestionCountFromBankMapping(mapping))
        .cognitiveLevel(mapping.getCognitiveLevel())
        .pointsPerQuestion(mapping.getPointsPerQuestion() != null ? mapping.getPointsPerQuestion() : BigDecimal.ONE)
        .createdAt(mapping.getCreatedAt())
        .updatedAt(mapping.getUpdatedAt())
        .build();
  }

  private int getQuestionCountFromBankMapping(ExamMatrixBankMapping mapping) {
    return mapping.getQuestionCount() != null ? Math.max(0, mapping.getQuestionCount()) : 0;
  }

  private TemplateMappingResponse buildMappingResponse(
      ExamMatrixTemplateMapping mapping, String templateName) {

    BigDecimal total =
        mapping.getTotalPoints() != null
            ? mapping.getTotalPoints()
            : mapping
                .getPointsPerQuestion()
                .multiply(BigDecimal.valueOf(mapping.getQuestionCount()));

    return TemplateMappingResponse.builder()
        .id(mapping.getId())
        .examMatrixId(mapping.getExamMatrixId())
        .templateId(mapping.getTemplateId())
        .templateName(templateName)
        .cognitiveLevel(mapping.getCognitiveLevel())
        .questionCount(mapping.getQuestionCount())
        .pointsPerQuestion(mapping.getPointsPerQuestion())
        .totalPoints(total)
        .createdAt(mapping.getCreatedAt())
        .updatedAt(mapping.getUpdatedAt())
        .build();
  }

  private int computeRelevanceScore(
      QuestionTemplate t, QuestionType requiredType, CognitiveLevel requiredLevel) {
    int score = 0;
    if (requiredType != null && requiredType.equals(t.getTemplateType())) score += 40;
    if (requiredLevel != null && requiredLevel.equals(t.getCognitiveLevel())) score += 30;
    if (t.getUsageCount() != null && t.getUsageCount() > 0) {
      score += Math.min(t.getUsageCount() / 10, 10);
    }
    return score;
  }

  private MatchingTemplatesResponse.TemplateItem mapToTemplateItem(
      QuestionTemplate t,
      UUID currentUserId,
      QuestionType requiredType,
      CognitiveLevel requiredLevel) {

    String creatorName = t.getCreator() != null ? t.getCreator().getFullName() : null;

    return MatchingTemplatesResponse.TemplateItem.builder()
        .templateId(t.getId())
        .name(t.getName())
        .description(t.getDescription())
        .templateType(t.getTemplateType())
        .cognitiveLevel(t.getCognitiveLevel())
        .tags(t.getTags())
        .mine(t.getCreatedBy().equals(currentUserId))
        .isPublic(t.getIsPublic())
        .createdBy(t.getCreatedBy())
        .createdByName(creatorName)
        .usageCount(t.getUsageCount())
        .avgSuccessRate(t.getAvgSuccessRate())
        .createdAt(t.getCreatedAt())
        .updatedAt(t.getUpdatedAt())
        .relevanceScore(computeRelevanceScore(t, requiredType, requiredLevel))
        .build();
  }

  /**
   * Validates that the chapter being added belongs to the same school level (cấp) as the matrix.
   * Only enforced when the matrix has a gradeLevel set; silently passes otherwise.
   */
  private void validateSchoolLevelConsistency(ExamMatrix matrix, UUID chapterId) {
    Integer matrixGrade = matrix.getGradeLevel();
    if (matrixGrade == null) return;

    Chapter chapter =
        chapterRepository
            .findById(chapterId)
            .filter(ch -> ch.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));

    if (chapter.getSubjectId() == null) return;

    Subject subject =
        subjectRepository
            .findById(chapter.getSubjectId())
            .filter(s -> s.getDeletedAt() == null)
            .orElse(null);

    if (subject == null || subject.getSchoolGrade() == null) return;

    Integer chapterGrade = subject.getSchoolGrade().getGradeLevel();
    if (chapterGrade == null) return;

    if (toSchoolLevel(matrixGrade) != toSchoolLevel(chapterGrade)) {
      throw new AppException(ErrorCode.MATRIX_GRADE_MISMATCH);
    }
  }

  private int toSchoolLevel(int grade) {
    if (grade <= 5) return 1;
    if (grade <= 9) return 2;
    return 3;
  }

  /**
   * Persist one {@link ExamMatrixRow} with bank-based cognitive cells.
   * <p>
   * Each row represents one "dạng bài" selected by teacher with a source question bank
   * and one difficulty. Each cell contributes a question-count for one cognitive level.
   */
  private void persistRow(UUID matrixId, MatrixRowRequest rowSpec, int orderIndex) {
    String qTypeName = rowSpec.getQuestionTypeName();
    if (qTypeName == null || qTypeName.isBlank()) {
      throw new AppException(ErrorCode.MATRIX_ROW_QUESTION_TYPE_REQUIRED);
    }

    // Phase 3: No longer validate or store questionBankId at row level
    // The bank is validated and stored at matrix level

    Chapter chapter =
        chapterRepository
            .findById(rowSpec.getChapterId())
            .filter(ch -> ch.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));

    UUID subjectId = chapter.getSubjectId();
    String subjectName = null;
    String schoolGradeName = null;
    Integer gradeLevel = null;
    if (subjectId != null) {
      Subject subject =
          subjectRepository
              .findById(subjectId)
              .filter(s -> s.getDeletedAt() == null)
              .orElse(null);
      if (subject != null) {
        subjectName = subject.getName();
        // Get grade level from SchoolGrade relationship
        if (subject.getSchoolGrade() != null) {
          schoolGradeName = subject.getSchoolGrade().getName();
          gradeLevel = subject.getSchoolGrade().getGradeLevel();
        }
      }
    }

    // Matrix can have multiple grades and subjects - no validation needed

    ExamMatrixRow row =
        ExamMatrixRow.builder()
            .examMatrixId(matrixId)
            .chapterId(rowSpec.getChapterId())
            .lessonId(rowSpec.getLessonId())
            .subjectId(subjectId)
            .subjectName(subjectName)
            .schoolGradeName(schoolGradeName)
            .chapterName(chapter.getTitle())
            .questionDifficulty(rowSpec.getQuestionDifficulty())
            .questionTypeName(qTypeName)
            .referenceQuestions(rowSpec.getReferenceQuestions())
            .orderIndex(rowSpec.getOrderIndex() != null ? rowSpec.getOrderIndex() : orderIndex)
            .build();

    row = examMatrixRowRepository.save(row);
    if (rowSpec.getCells() != null && !rowSpec.getCells().isEmpty()) {
      upsertRowCells(matrixId, row, rowSpec.getCells(), null);
    }
  }

  private void upsertRowCells(
      UUID matrixId,
      ExamMatrixRow row,
      List<MatrixCellRequest> cells,
      BigDecimal defaultPointsPerQuestion) {
    if (cells == null || cells.isEmpty()) {
      return;
    }

    // Load matrix to get numberOfParts for validation
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);

    // BE-7: PERFORMANCE - Load parts ONCE per request, not per cell
    Map<Integer, ExamMatrixPart> partsCache = examMatrixPartRepository
        .findByExamMatrixIdOrderByPartNumber(matrixId).stream()
        .collect(Collectors.toMap(ExamMatrixPart::getPartNumber, p -> p));

    // BE-7: Proper upsert logic - find existing cells and update/insert/delete as needed
    for (MatrixCellRequest cell : cells) {
      // Validate partNumber is provided and within range
      if (cell.getPartNumber() == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
      }

      if (cell.getPartNumber() < 1 || cell.getPartNumber() > 3) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
      }

      // Validate partNumber does not exceed matrix numberOfParts
      if (cell.getPartNumber() > matrix.getNumberOfParts()) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
      }

      // BE-1: Use partsCache instead of ExamPart.typeForPart()
      ExamMatrixPart part = partsCache.get(cell.getPartNumber());
      if (part == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
      }

      // DIAGNOSTIC: Log all mappings for this row so we can see what's actually in the DB
      List<ExamMatrixBankMapping> allRowMappings =
          bankMappingRepository.findByExamMatrixIdAndMatrixRowIdOrderByCreatedAt(matrixId, row.getId());
      log.info("[CELL DEBUG] All stored mappings for row={}: count={}", row.getId(), allRowMappings.size());
      for (ExamMatrixBankMapping m : allRowMappings) {
        log.info("[CELL DEBUG]   stored: id={} part={} level={} count={} deletedAt={}",
            m.getId(), m.getPartNumber(), m.getCognitiveLevel(), m.getQuestionCount(), m.getDeletedAt());
      }

      // Find existing cell by unique key (matrixId, rowId, partNumber, cognitiveLevel)
      // ALSO try legacy English enum equivalent (old data stored UNDERSTAND/REMEMBER/APPLY/ANALYZE)
      Optional<ExamMatrixBankMapping> existing =
          bankMappingRepository.findByExamMatrixIdAndMatrixRowIdAndPartNumberAndCognitiveLevel(
              matrixId, row.getId(), cell.getPartNumber(), cell.getCognitiveLevel());

      if (existing.isEmpty()) {
        CognitiveLevel legacyLevel = toLegacyCognitiveLevel(cell.getCognitiveLevel());
        if (legacyLevel != null) {
          existing = bankMappingRepository.findByExamMatrixIdAndMatrixRowIdAndPartNumberAndCognitiveLevel(
              matrixId, row.getId(), cell.getPartNumber(), legacyLevel);
          if (existing.isPresent()) {
            log.info("[CELL DEBUG] Found mapping via legacy level {} for new level {}",
                legacyLevel, cell.getCognitiveLevel());
          }
        }
      }

      int questionCount = cell.getQuestionCount() != null ? Math.max(0, cell.getQuestionCount()) : 0;

      log.info("[CELL DEBUG] row={} part={} level={} count={} existingFound={}",
          row.getId(), cell.getPartNumber(), cell.getCognitiveLevel(), questionCount, existing.isPresent());

      if (questionCount == 0) {
        // DELETE if exists
        if (existing.isPresent()) {
          log.info("[CELL DEBUG] DELETING mapping id={}", existing.get().getId());
          bankMappingRepository.delete(existing.get());
          bankMappingRepository.flush();
          log.info("[CELL DEBUG] DELETE flushed for mapping id={}", existing.get().getId());
        } else {
          log.info("[CELL DEBUG] DELETE skipped - no existing mapping found");
        }
        continue;
      }

      BigDecimal pointsPerQuestion =
          cell.getPointsPerQuestion() != null
              ? cell.getPointsPerQuestion()
              : (defaultPointsPerQuestion != null ? defaultPointsPerQuestion : BigDecimal.ONE);

      if (existing.isPresent()) {
        // UPDATE existing cell — also normalize legacy English enum to Vietnamese
        ExamMatrixBankMapping mapping = existing.get();
        log.info("[CELL DEBUG] UPDATING mapping id={} newCount={}", mapping.getId(), questionCount);
        mapping.setQuestionCount(questionCount);
        mapping.setPointsPerQuestion(pointsPerQuestion);
        mapping.setQuestionType(part.getQuestionType());
        mapping.setPartId(part.getId());
        // Normalize legacy English cognitiveLevel → Vietnamese on every update
        mapping.setCognitiveLevel(cell.getCognitiveLevel());
        bankMappingRepository.save(mapping);
        log.info("[CELL DEBUG] UPDATE saved for mapping id={}", mapping.getId());
      } else {
        // INSERT new cell
        ExamMatrixBankMapping mapping =
            ExamMatrixBankMapping.builder()
                .examMatrixId(matrixId)
                .matrixRowId(row.getId())
                .partNumber(cell.getPartNumber())
                .partId(part.getId())
                .questionType(part.getQuestionType())
                .cognitiveLevel(cell.getCognitiveLevel())
                .questionCount(questionCount)
                .pointsPerQuestion(pointsPerQuestion)
                .build();
        bankMappingRepository.save(mapping);
      }
    }
  }

  /**
   * Build the full hierarchical {@link ExamMatrixTableResponse} for a matrix.
   * Groups rows by chapter in order_index order.
   */
  private ExamMatrixTableResponse buildTableResponse(ExamMatrix matrix) {
    UUID matrixId = matrix.getId();

    // Load rows ordered by orderIndex
    List<ExamMatrixRow> rows =
        examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId);

    // Load all bank-based cells for this matrix keyed by rowId
    List<ExamMatrixBankMapping> allCells =
      bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);
    Map<UUID, List<ExamMatrixBankMapping>> cellsByRow =
        allCells.stream()
            .filter(c -> c.getMatrixRowId() != null)
        .collect(Collectors.groupingBy(ExamMatrixBankMapping::getMatrixRowId));

    // Group rows by chapter
    Map<UUID, List<ExamMatrixRow>> rowsByChapter =
        rows.stream()
            .filter(r -> r.getChapterId() != null)
            .collect(
                Collectors.groupingBy(
                    ExamMatrixRow::getChapterId,
                    java.util.LinkedHashMap::new,
                    Collectors.toList()));

    // Handle rows without a chapter
    List<ExamMatrixRow> uncategorised =
        rows.stream().filter(r -> r.getChapterId() == null).collect(Collectors.toList());
    if (!uncategorised.isEmpty()) {
      rowsByChapter.put(null, uncategorised);
    }

    // Grand-total accumulators
    Map<String, Integer> grandTotalByCognitive = new LinkedHashMap<>();
    int grandTotalQuestions = 0;
    BigDecimal grandTotalPoints = BigDecimal.ZERO;

    List<MatrixChapterGroupResponse> chapterGroups = new ArrayList<>();

    for (Map.Entry<UUID, List<ExamMatrixRow>> entry : rowsByChapter.entrySet()) {
      UUID chapterId = entry.getKey();
      List<ExamMatrixRow> chapterRows = entry.getValue();

      String chapterTitle = null;
      Integer chapterOrder = null;
      if (chapterId != null) {
        var chapOpt = chapterRepository.findById(chapterId);
        if (chapOpt.isPresent()) {
          chapterTitle = chapOpt.get().getTitle();
          chapterOrder = chapOpt.get().getOrderIndex();
        }
      }

      Map<String, Integer> chapterTotalByCognitive = new LinkedHashMap<>();
      int chapterTotalQ = 0;
      BigDecimal chapterTotalPts = BigDecimal.ZERO;
      List<MatrixRowResponse> rowResponses = new ArrayList<>();

      for (ExamMatrixRow row : chapterRows) {
        List<ExamMatrixBankMapping> templateCells =
            cellsByRow.getOrDefault(row.getId(), Collections.emptyList());
        Map<String, Integer> countByCognitive = new LinkedHashMap<>();
        List<MatrixCellResponse> cellResponses = new ArrayList<>();
        int rowTotalQ = 0;
        BigDecimal rowTotalPts = BigDecimal.ZERO;

        // Fetch the actual grade from the Chapter entity, not the snapshot
        String actualGradeName = row.getSchoolGradeName(); // Default to snapshot
        if (row.getChapterId() != null) {
          var chapterOpt = chapterRepository.findById(row.getChapterId());
          if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            // Get grade from chapter's subject's schoolGrade
            if (chapter.getSubject() != null && chapter.getSubject().getSchoolGrade() != null) {
              SchoolGrade schoolGrade = chapter.getSubject().getSchoolGrade();
              if (schoolGrade.getName() != null) {
                actualGradeName = schoolGrade.getName();
              }
            }
          }
        }

        for (ExamMatrixBankMapping cell : templateCells) {
          String label = cognitiveLevelLabel(cell.getCognitiveLevel());
            int count = getQuestionCountFromBankMapping(cell);
          BigDecimal pointsPerQuestion =
            cell.getPointsPerQuestion() != null ? cell.getPointsPerQuestion() : BigDecimal.ONE;
          BigDecimal pts = pointsPerQuestion.multiply(BigDecimal.valueOf(count));

            countByCognitive.merge(
              label,
              count,
              (left, right) -> (left == null ? 0 : left) + (right == null ? 0 : right));
          rowTotalQ += count;
          rowTotalPts = rowTotalPts.add(pts);

          cellResponses.add(
              MatrixCellResponse.builder()
                  .mappingId(cell.getId())
                  .partNumber(cell.getPartNumber())  // Phase 5: Direct read from DB
                  .questionType(cell.getQuestionType())  // Phase 5: Direct read from DB
                  .cognitiveLevel(cell.getCognitiveLevel())
                  .cognitiveLevelLabel(label)
                  .questionCount(count)
                    .pointsPerQuestion(pointsPerQuestion)
                  .totalPoints(pts)
                  .build());
        }

        rowResponses.add(
            MatrixRowResponse.builder()
                .rowId(row.getId())
                .chapterId(row.getChapterId())
                .lessonId(row.getLessonId())
                .subjectName(row.getSubjectName())
                .schoolGradeName(actualGradeName)  // Use actual grade from Chapter/Subject
                .chapterName(row.getChapterName())
                .questionDifficulty(row.getQuestionDifficulty())
                .questionTypeName(row.getQuestionTypeName())
                .referenceQuestions(row.getReferenceQuestions())
                .orderIndex(row.getOrderIndex())
                .cells(cellResponses)
                .countByCognitive(countByCognitive)
                .rowTotalQuestions(rowTotalQ)
                .rowTotalPoints(rowTotalPts)
                .build());

        // Accumulate chapter totals
        countByCognitive.forEach(
          (k, v) ->
            chapterTotalByCognitive.merge(
              k,
              v,
              (left, right) -> (left == null ? 0 : left) + (right == null ? 0 : right)));
        chapterTotalQ += rowTotalQ;
        chapterTotalPts = chapterTotalPts.add(rowTotalPts);
      }

      chapterGroups.add(
          MatrixChapterGroupResponse.builder()
              .chapterId(chapterId)
              .chapterTitle(chapterTitle)
              .chapterOrderIndex(chapterOrder)
              .rows(rowResponses)
              .totalByCognitive(chapterTotalByCognitive)
              .chapterTotalQuestions(chapterTotalQ)
              .chapterTotalPoints(chapterTotalPts)
              .build());

      // Accumulate grand totals
        chapterTotalByCognitive.forEach(
          (k, v) ->
            grandTotalByCognitive.merge(
              k,
              v,
              (left, right) -> (left == null ? 0 : left) + (right == null ? 0 : right)));
      grandTotalQuestions += chapterTotalQ;
      grandTotalPoints = grandTotalPoints.add(chapterTotalPts);
    }

    // Resolve subject info from the first chapter in the matrix
    String finalSubjectName = null;
    UUID finalSubjectId = null;
    if (!chapterGroups.isEmpty() && chapterGroups.get(0).getChapterId() != null) {
      var chapOpt = chapterRepository.findById(chapterGroups.get(0).getChapterId());
      if (chapOpt.isPresent()) {
        Chapter firstChapter = chapOpt.get();
        if (firstChapter.getSubjectId() != null) {
          finalSubjectId = firstChapter.getSubjectId();
          finalSubjectName =
              subjectRepository
                  .findById(firstChapter.getSubjectId())
                  .map(Subject::getName)
                  .orElse(null);
        }
      }
    }

    String teacherName =
        userRepository.findById(matrix.getTeacherId()).map(User::getFullName).orElse("Unknown");

    // BE-8: Build parts responses (ALWAYS populated)
    List<ExamMatrixPartResponse> partResponses = buildPartResponses(matrix.getId());

    return ExamMatrixTableResponse.builder()
        .id(matrix.getId())
        .name(matrix.getName())
        .description(matrix.getDescription())
        .teacherId(matrix.getTeacherId())
        .teacherName(teacherName)
        .gradeLevel(matrix.getGradeLevel())
        .numberOfParts(matrix.getNumberOfParts())
        .parts(partResponses)
        .subjectId(finalSubjectId)
        .subjectName(finalSubjectName)
        .isReusable(matrix.getIsReusable())
        .status(matrix.getStatus())
        .chapters(chapterGroups)
        .grandTotalByCognitive(grandTotalByCognitive)
        .grandTotalQuestions(grandTotalQuestions)
        .grandTotalPoints(grandTotalPoints)
        .totalQuestionsTarget(matrix.getTotalQuestionsTarget())
        .totalPointsTarget(matrix.getTotalPointsTarget())
        .createdAt(matrix.getCreatedAt())
        .updatedAt(matrix.getUpdatedAt())
        .build();
  }

  /** Maps a {@link CognitiveLevel} to its short Vietnamese label (NB/TH/VD/VDC). */
  private static String cognitiveLevelLabel(CognitiveLevel level) {
    return switch (level) {
      case NHAN_BIET, REMEMBER -> "NB";
      case THONG_HIEU, UNDERSTAND -> "TH";
      case VAN_DUNG, APPLY -> "VD";
      case VAN_DUNG_CAO, ANALYZE, EVALUATE, CREATE -> "VDC";
    };
  }

  /**
   * Maps a new Vietnamese CognitiveLevel to its legacy English equivalent.
   * Old matrix data was stored with REMEMBER/UNDERSTAND/APPLY/ANALYZE enum values.
   * New FE sends NHAN_BIET/THONG_HIEU/VAN_DUNG/VAN_DUNG_CAO.
   * This allows the upsert to find old rows by trying the legacy key.
   */
  private static CognitiveLevel toLegacyCognitiveLevel(CognitiveLevel level) {
    if (level == null) return null;
    return switch (level) {
      case NHAN_BIET -> CognitiveLevel.REMEMBER;
      case THONG_HIEU -> CognitiveLevel.UNDERSTAND;
      case VAN_DUNG -> CognitiveLevel.APPLY;
      case VAN_DUNG_CAO -> CognitiveLevel.ANALYZE;
      default -> null; // Already a legacy value or unknown
    };
  }
}
