package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.dto.request.ImportQuestionsRequest;
import com.fptu.math_master.dto.request.UpdateQuestionRequest;
import com.fptu.math_master.dto.response.ImportQuestionsResponse;
import com.fptu.math_master.dto.response.QuestionResponse;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.QuestionSourceType;
import com.fptu.math_master.enums.QuestionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.QuestionService;
import com.fptu.math_master.util.SecurityUtils;
import com.fptu.math_master.util.CSVParser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuestionServiceImpl implements QuestionService {

  QuestionRepository questionRepository;
  UserRepository userRepository;
  QuestionBankRepository questionBankRepository;
  QuestionTemplateRepository questionTemplateRepository;

  @Override
  public QuestionResponse createQuestion(CreateQuestionRequest request) {
    // Validate LOB fields are not null/empty before saving
    if (request.getQuestionText() == null || request.getQuestionText().trim().isEmpty()) {
      log.warn("Question text is empty");
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    if (request.getCorrectAnswer() == null || request.getCorrectAnswer().trim().isEmpty()) {
      log.warn("Correct answer is empty");
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    String questionText = request.getQuestionText().trim();
    log.info(
        "Creating question: {}", questionText.substring(0, Math.min(50, questionText.length())));

    UUID currentUserId = getCurrentUserId();

    // Set defaults for optional fields
    String explanation =
        request.getExplanation() != null
            ? request.getExplanation().trim()
            : "No explanation provided";
    String correctAnswer = request.getCorrectAnswer().trim();

    // If chapterId not provided but templateId is, fetch chapterId from template
    UUID chapterId = request.getChapterId();
    if (chapterId == null && request.getTemplateId() != null) {
      chapterId = questionTemplateRepository
          .findById(request.getTemplateId())
          .map(QuestionTemplate::getChapterId)
          .orElse(null);
      log.info("Auto-populated chapterId {} from template {}", chapterId, request.getTemplateId());
    }

    Question question =
        Question.builder()
            .questionText(questionText)
            .questionType(request.getQuestionType())
            .options(request.getOptions())
            .correctAnswer(correctAnswer)
            .explanation(explanation)
          .solutionSteps(request.getSolutionSteps())
          .diagramData(request.getDiagramData())
            .points(request.getPoints())
            .cognitiveLevel(request.getCognitiveLevel())
            .tags(request.getTags())
            .questionBankId(request.getQuestionBankId())
            .templateId(request.getTemplateId())
            .chapterId(chapterId)  // Set chapterId
          .canonicalQuestionId(request.getCanonicalQuestionId())
            .questionStatus(QuestionStatus.UNDER_REVIEW)
            .questionSourceType(QuestionSourceType.MANUAL)
            .build();
    question.setCreatedBy(currentUserId);

    question = questionRepository.save(question);

    // Log EXACTLY what was saved to the database
    log.info("Question saved to DB with ID: {}", question.getId());
    log.info(
        "  - questionText type: {}, value: {}",
        question.getQuestionText() == null
            ? "null"
            : question.getQuestionText().getClass().getSimpleName(),
        question.getQuestionText() == null
            ? "null"
            : question
                .getQuestionText()
                .substring(0, Math.min(50, question.getQuestionText().length())));
    log.info(
        "  - correctAnswer type: {}, value: {}",
        question.getCorrectAnswer() == null
            ? "null"
            : question.getCorrectAnswer().getClass().getSimpleName(),
        question.getCorrectAnswer());
    log.info(
        "  - explanation type: {}, value: {}",
        question.getExplanation() == null
            ? "null"
            : question.getExplanation().getClass().getSimpleName(),
        question.getExplanation() == null
            ? "null"
            : question
                .getExplanation().substring(0, Math.min(50, question.getExplanation().length())));
    log.info("  - chapterId: {}", question.getChapterId());

    return mapToResponse(question);
  }

  @Override
  public QuestionResponse getQuestionById(UUID id) {
    log.info("Fetching question by id: {}", id);
    Question question =
        questionRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(
                () -> {
                  log.warn("Question not found: {}", id);
                  return new AppException(ErrorCode.QUESTION_NOT_FOUND);
                });

    return mapToResponse(question);
  }

  @Override
  public Page<QuestionResponse> getMyQuestions(String name, String tag, Pageable pageable) {
    UUID currentUserId = getCurrentUserId();
    log.info("Fetching questions for user: {}", currentUserId);

    String namePattern = (name != null && !name.isBlank()) ? "%" + name.trim() + "%" : null;
    String tagPattern = (tag != null && !tag.isBlank()) ? "%" + tag.trim() + "%" : null;

    // Native query already has ORDER BY hardcoded; strip sort from Pageable to prevent
    // Hibernate appending camelCase field names that PostgreSQL can't resolve.
    Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return questionRepository
        .findByCreatedByWithSearch(currentUserId, namePattern, tagPattern, unsorted)
        .map(this::mapToResponse);
  }

  @Override
  public Page<QuestionResponse> getQuestionsByBank(UUID bankId, Pageable pageable) {
    // Validate bank exists
    questionBankRepository
        .findById(bankId)
        .orElseThrow(
            () -> {
              log.warn("Question bank not found: {}", bankId);
              return new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND);
            });

    log.info("Fetching questions for bank: {}", bankId);
    return questionRepository
        .findByQuestionBankIdAndNotDeleted(bankId, pageable)
        .map(this::mapToResponse);
  }

  @Override
  public List<QuestionResponse> getQuestionsByTemplate(UUID templateId) {
    log.info("Fetching questions for template: {}", templateId);
    return questionRepository.findByTemplateIdAndNotDeleted(templateId).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Override
  public Page<QuestionResponse> getQuestionsByCanonicalQuestion(
      UUID canonicalQuestionId, Pageable pageable) {
    log.info("Fetching questions for canonical question: {}", canonicalQuestionId);
    return questionRepository
        .findByCanonicalQuestionIdAndNotDeleted(canonicalQuestionId, pageable)
        .map(this::mapToResponse);
  }

  @Override
  public QuestionResponse updateQuestion(UUID id, UpdateQuestionRequest request) {
    Question question =
        questionRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(
                () -> {
                  log.warn("Question not found: {}", id);
                  return new AppException(ErrorCode.QUESTION_NOT_FOUND);
                });

    // Verify user owns this question
    UUID currentUserId = getCurrentUserId();
    if (!question.getCreatedBy().equals(currentUserId)) {
      log.warn(
          "User {} attempted to update question {} owned by {}",
          currentUserId,
          id,
          question.getCreatedBy());
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    // Update fields
    if (request.getQuestionText() != null) {
      question.setQuestionText(request.getQuestionText());
    }
    if (request.getOptions() != null) {
      question.setOptions(request.getOptions());
    }
    if (request.getCorrectAnswer() != null) {
      question.setCorrectAnswer(request.getCorrectAnswer());
    }
    if (request.getExplanation() != null) {
      question.setExplanation(request.getExplanation());
    }
    if (request.getSolutionSteps() != null) {
      question.setSolutionSteps(request.getSolutionSteps());
    }
    if (request.getDiagramData() != null) {
      question.setDiagramData(request.getDiagramData());
    }
    if (request.getPoints() != null) {
      question.setPoints(request.getPoints());
    }
    if (request.getCognitiveLevel() != null) {
      question.setCognitiveLevel(request.getCognitiveLevel());
    }
    if (request.getTags() != null) {
      question.setTags(request.getTags());
    }
    if (request.getStatus() != null) {
      if (request.getStatus() == QuestionStatus.APPROVED
          && !isReviewable(question.getQuestionStatus())) {
        throw new AppException(ErrorCode.QUESTION_REVIEW_STATUS_INVALID);
      }
      question.setQuestionStatus(request.getStatus());
    }

    question.setUpdatedAt(Instant.now());
    question = questionRepository.save(question);

    log.info("Question updated: {}", id);
    return mapToResponse(question);
  }

  @Override
  public QuestionResponse approveQuestion(UUID id) {
    Question question =
        questionRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    if (!question.getCreatedBy().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    if (!isReviewable(question.getQuestionStatus())) {
      throw new AppException(ErrorCode.QUESTION_REVIEW_STATUS_INVALID);
    }

    question.setQuestionStatus(QuestionStatus.APPROVED);
    question.setUpdatedAt(Instant.now());
    question = questionRepository.save(question);
    return mapToResponse(question);
  }

  /**
   * A question is in a reviewable state if it has been generated but not yet
   * approved or archived. The unified Blueprint flow uses {@code UNDER_REVIEW};
   * older rows still in {@code AI_DRAFT} are accepted for one transition window.
   */
  private static boolean isReviewable(QuestionStatus s) {
    return s == QuestionStatus.UNDER_REVIEW || s == QuestionStatus.AI_DRAFT;
  }

  @Override
  public Integer bulkApproveQuestions(List<UUID> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return 0;
    }

    UUID currentUserId = getCurrentUserId();
    int approvedCount = 0;

    for (UUID id : questionIds) {
      Question question =
          questionRepository
              .findByIdAndNotDeleted(id)
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

      if (!question.getCreatedBy().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      if (!isReviewable(question.getQuestionStatus())) {
        throw new AppException(ErrorCode.QUESTION_REVIEW_STATUS_INVALID);
      }

      question.setQuestionStatus(QuestionStatus.APPROVED);
      question.setUpdatedAt(Instant.now());
      questionRepository.save(question);
      approvedCount++;
    }

    return approvedCount;
  }

  @Override
  public Page<QuestionResponse> listReviewQueue(UUID templateId, Pageable pageable) {
    UUID currentUserId = getCurrentUserId();
    Page<Question> page =
        questionRepository.findReviewQueue(currentUserId, templateId, pageable);
    // Avoid N+1: resolve every templateName the page needs in a single query.
    Map<UUID, String> templateNames = batchLookupTemplateNames(page.getContent());
    return page.map(q -> mapToResponse(q, templateNames));
  }

  /**
   * Single-query lookup of {@code templateName} for the given questions. Returns
   * an empty map when no question references a template.
   */
  private Map<UUID, String> batchLookupTemplateNames(java.util.List<Question> questions) {
    Set<UUID> ids = new java.util.LinkedHashSet<>();
    for (Question q : questions) {
      if (q.getTemplateId() != null) ids.add(q.getTemplateId());
    }
    if (ids.isEmpty()) return java.util.Collections.emptyMap();
    Map<UUID, String> out = new java.util.HashMap<>();
    for (com.fptu.math_master.entity.QuestionTemplate t :
        questionTemplateRepository.findAllById(ids)) {
      out.put(t.getId(), t.getName());
    }
    return out;
  }

  /**
   * Reject UNDER_REVIEW (or legacy AI_DRAFT) questions in one call. Rejection moves
   * the question to ARCHIVED so it leaves the review queue but stays in the audit
   * trail. The optional reason is stamped onto generationMetadata.rejectionReason.
   */
  @Override
  public Integer bulkRejectQuestions(List<UUID> questionIds, String reason) {
    if (questionIds == null || questionIds.isEmpty()) return 0;
    UUID currentUserId = getCurrentUserId();
    int rejectedCount = 0;

    for (UUID id : questionIds) {
      Question question =
          questionRepository
              .findByIdAndNotDeleted(id)
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

      if (!question.getCreatedBy().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      if (!isReviewable(question.getQuestionStatus())) {
        throw new AppException(ErrorCode.QUESTION_REVIEW_STATUS_INVALID);
      }

      Map<String, Object> meta = question.getGenerationMetadata();
      if (meta == null) meta = new LinkedHashMap<>();
      if (reason != null && !reason.isBlank()) {
        meta.put("rejectionReason", reason);
      }
      meta.put("rejectedAt", Instant.now().toString());
      meta.put("rejectedBy", currentUserId.toString());
      question.setGenerationMetadata(meta);
      question.setQuestionStatus(QuestionStatus.ARCHIVED);
      question.setUpdatedAt(Instant.now());
      questionRepository.save(question);
      rejectedCount++;
    }

    return rejectedCount;
  }

  @Override
  public void deleteQuestion(UUID id) {
    Question question =
        questionRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(
                () -> {
                  log.warn("Question not found: {}", id);
                  return new AppException(ErrorCode.QUESTION_NOT_FOUND);
                });

    // Verify user owns this question
    UUID currentUserId = getCurrentUserId();
    if (!question.getCreatedBy().equals(currentUserId)) {
      log.warn(
          "User {} attempted to delete question {} owned by {}",
          currentUserId,
          id,
          question.getCreatedBy());
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    // Soft delete
    questionRepository.softDeleteById(id);
    log.info("Question deleted: {}", id);
  }

  @Override
  public ImportQuestionsResponse importQuestionsFromFile(ImportQuestionsRequest request) {
    log.info("Starting question import, format: {}", request.getFileFormat());

    ImportQuestionsResponse response =
        ImportQuestionsResponse.builder()
            .results(new ArrayList<>())
            .successCount(0)
            .failureCount(0)
            .totalRows(0)
            .build();

    UUID currentUserId = getCurrentUserId();

    // Parse CSV/file content
    CSVParser.ParseResult parseResult = CSVParser.parseCSV(request.getFileContent());

    response.setTotalRows(parseResult.questions.size() + parseResult.errors.size());

    // Import each question
    for (int i = 0; i < parseResult.questions.size(); i++) {
      CreateQuestionRequest questionRequest = parseResult.questions.get(i);
      int rowNumber = i + 2; // Header is row 1

      try {
        // Set bank ID if provided in request
        if (request.getQuestionBankId() != null) {
          questionRequest.setQuestionBankId(request.getQuestionBankId());
        }

        Question question =
            Question.builder()
                .questionText(questionRequest.getQuestionText())
                .questionType(questionRequest.getQuestionType())
                .options(questionRequest.getOptions())
                .correctAnswer(questionRequest.getCorrectAnswer())
                .explanation(questionRequest.getExplanation())
              .solutionSteps(questionRequest.getSolutionSteps())
              .diagramData(questionRequest.getDiagramData())
                .points(questionRequest.getPoints())
                .cognitiveLevel(questionRequest.getCognitiveLevel())
                .tags(questionRequest.getTags())
                .questionBankId(questionRequest.getQuestionBankId())
                .templateId(questionRequest.getTemplateId())
              .canonicalQuestionId(questionRequest.getCanonicalQuestionId())
                .questionStatus(QuestionStatus.AI_DRAFT)
                .questionSourceType(QuestionSourceType.BANK_IMPORTED)
                .build();
        question.setCreatedBy(currentUserId);

        question = questionRepository.save(question);

        response
            .getResults()
            .add(
                ImportQuestionsResponse.ImportRowResult.builder()
                    .rowNumber(rowNumber)
                    .questionId(question.getId().toString())
                    .success(true)
                    .build());

        response.setSuccessCount(response.getSuccessCount() + 1);
        log.info("Successfully imported question from row {}: {}", rowNumber, question.getId());

      } catch (Exception e) {
        response.setFailureCount(response.getFailureCount() + 1);
        response
            .getResults()
            .add(
                ImportQuestionsResponse.ImportRowResult.builder()
                    .rowNumber(rowNumber)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());

        log.warn("Failed to import question from row {}: {}", rowNumber, e.getMessage());

        if (!request.getContinueOnError()) {
          response.setStatus("FAILURE");
          return response;
        }
      }
    }

    // Add any parsing errors to results
    for (String error : parseResult.errors) {
      response.setFailureCount(response.getFailureCount() + 1);
      response
          .getResults()
          .add(
              ImportQuestionsResponse.ImportRowResult.builder()
                  .success(false)
                  .errorMessage(error)
                  .build());
    }

    // Determine overall status
    if (Objects.equals(response.getSuccessCount(), response.getTotalRows())) {
      response.setStatus("SUCCESS");
    } else if (response.getSuccessCount() > 0) {
      response.setStatus("PARTIAL_SUCCESS");
    } else {
      response.setStatus("FAILURE");
    }

    log.info(
        "Import complete: {} success, {} failures out of {}",
        response.getSuccessCount(),
        response.getFailureCount(),
        response.getTotalRows());

    return response;
  }

  @Override
  public Page<QuestionResponse> searchByKeywordAndTags(
      String keyword, List<String> tags, Pageable pageable) {
    UUID currentUserId = getCurrentUserId();
    String keywordPattern =
        (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;
    String tagsParam =
        (tags != null && !tags.isEmpty())
            ? tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.joining(","))
            : null;
    if (tagsParam != null && tagsParam.isBlank()) tagsParam = null;

    Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return questionRepository
        .searchByKeywordAndTags(currentUserId, keywordPattern, tagsParam, unsorted)
        .map(this::mapToResponse);
  }

  @Override
  public Page<QuestionResponse> searchQuestions(
      String searchTerm, String type, Pageable pageable) {
    UUID currentUserId = getCurrentUserId();

    if (searchTerm != null && !searchTerm.isEmpty()) {
      String searchPattern = "%" + searchTerm.toLowerCase() + "%";
      return questionRepository
          .searchByCreatedBy(currentUserId, searchPattern, pageable)
          .map(this::mapToResponse);
    } else {
      com.fptu.math_master.enums.QuestionType typeEnum = null;
      if (type != null && !type.isBlank()) {
        try {
          typeEnum = com.fptu.math_master.enums.QuestionType.valueOf(type);
        } catch (IllegalArgumentException ignored) {
          // Keep null to mean "no filter"
        }
      }

      return questionRepository
          .findByFilters(currentUserId, typeEnum, pageable)
          .map(this::mapToResponse);
    }
  }

  @Override
  public Integer assignQuestionsToBank(UUID bankId, List<UUID> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return 0;
    }

    UUID currentUserId = getCurrentUserId();

    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    if (!bank.getTeacherId().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }

    Set<UUID> uniqueQuestionIds = new LinkedHashSet<>(questionIds);
    int assignedCount = 0;

    for (UUID questionId : uniqueQuestionIds) {
      Question question =
          questionRepository
              .findByIdAndNotDeleted(questionId)
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

      if (!question.getCreatedBy().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      question.setQuestionBankId(bankId);
      question.setUpdatedAt(Instant.now());
      questionRepository.save(question);
      assignedCount++;
    }

    log.info("Assigned {} questions to bank {}", assignedCount, bankId);
    return assignedCount;
  }

  @Override
  public Integer removeQuestionsFromBank(UUID bankId, List<UUID> questionIds) {
    if (questionIds == null || questionIds.isEmpty()) {
      return 0;
    }

    UUID currentUserId = getCurrentUserId();

    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    if (!bank.getTeacherId().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }

    Set<UUID> uniqueQuestionIds = new LinkedHashSet<>(questionIds);
    int removedCount = 0;

    for (UUID questionId : uniqueQuestionIds) {
      Question question =
          questionRepository
              .findByIdAndNotDeleted(questionId)
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

      if (!question.getCreatedBy().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }

      if (bankId.equals(question.getQuestionBankId())) {
        question.setQuestionBankId(null);
        question.setUpdatedAt(Instant.now());
        questionRepository.save(question);
        removedCount++;
      }
    }

    log.info("Removed {} questions from bank {}", removedCount, bankId);
    return removedCount;
  }

  /**
   * Single-question variant: looks up {@code templateName} on demand. Used by
   * {@code approve}, {@code update}, and other one-off paths. List paths should
   * call {@link #mapToResponse(Question, Map)} with a batched lookup to avoid
   * N+1 queries.
   */
  private QuestionResponse mapToResponse(Question question) {
    Map<UUID, String> single = java.util.Collections.emptyMap();
    if (question.getTemplateId() != null) {
      String name = questionTemplateRepository
          .findById(question.getTemplateId())
          .map(com.fptu.math_master.entity.QuestionTemplate::getName)
          .orElse(null);
      if (name != null) single = java.util.Map.of(question.getTemplateId(), name);
    }
    return mapToResponse(question, single);
  }

  private QuestionResponse mapToResponse(Question question, Map<UUID, String> templateNames) {
    String creatorName =
        userRepository.findById(question.getCreatedBy()).map(User::getFullName).orElse("Unknown");

    String bankName = null;
    if (question.getQuestionBankId() != null) {
      bankName =
          questionBankRepository
              .findById(question.getQuestionBankId())
              .map(QuestionBank::getName)
              .orElse(null);
    }

    String templateName =
        question.getTemplateId() == null ? null : templateNames.get(question.getTemplateId());

    return QuestionResponse.builder()
        .id(question.getId())
        .createdBy(question.getCreatedBy())
        .creatorName(creatorName)
        .questionText(question.getQuestionText())
        .questionType(question.getQuestionType())
        .options(question.getOptions())
        .correctAnswer(question.getCorrectAnswer())
        .explanation(question.getExplanation())
        .solutionSteps(question.getSolutionSteps())
        .diagramData(question.getDiagramData())
        .points(question.getPoints())
        .cognitiveLevel(question.getCognitiveLevel())
        .questionStatus(question.getQuestionStatus())
        .questionSourceType(question.getQuestionSourceType())
        .tags(question.getTags())
        .templateId(question.getTemplateId())
        .templateName(templateName)
        .canonicalQuestionId(question.getCanonicalQuestionId())
        .questionBankId(question.getQuestionBankId())
        .questionBankName(bankName)
        .createdAt(question.getCreatedAt())
        .updatedAt(question.getUpdatedAt())
        .build();
  }

  private UUID getCurrentUserId() {
    JwtAuthenticationToken authentication =
        (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    return UUID.fromString(authentication.getName());
  }
}
