package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.dto.request.ImportQuestionsRequest;
import com.fptu.math_master.dto.request.UpdateQuestionRequest;
import com.fptu.math_master.dto.response.ImportQuestionsResponse;
import com.fptu.math_master.dto.response.QuestionResponse;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.QuestionSourceType;
import com.fptu.math_master.enums.QuestionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.QuestionService;
import com.fptu.math_master.util.CSVParser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
    log.info("Creating question: {}", questionText.substring(0, Math.min(50, questionText.length())));

    UUID currentUserId = getCurrentUserId();

    // Set defaults for optional fields
    String explanation = request.getExplanation() != null ? request.getExplanation().trim() : "No explanation provided";
    String correctAnswer = request.getCorrectAnswer().trim();

    Question question =
        Question.builder()
            .createdBy(currentUserId)
            .questionText(questionText)
            .questionType(request.getQuestionType())
            .options(request.getOptions())
            .correctAnswer(correctAnswer)
            .explanation(explanation)
            .points(request.getPoints())
            .difficulty(request.getDifficulty())
            .cognitiveLevel(request.getCognitiveLevel())
            .tags(request.getTags())
            .questionBankId(request.getQuestionBankId())
            .templateId(request.getTemplateId())
            .questionStatus(QuestionStatus.AI_DRAFT)
            .questionSourceType(QuestionSourceType.MANUAL)
            .build();

    question = questionRepository.save(question);

    // Log EXACTLY what was saved to the database
    log.info("Question saved to DB with ID: {}", question.getId());
    log.info("  - questionText type: {}, value: {}",
        question.getQuestionText() == null ? "null" : question.getQuestionText().getClass().getSimpleName(),
        question.getQuestionText() == null ? "null" : question.getQuestionText().substring(0, Math.min(50, question.getQuestionText().length())));
    log.info("  - correctAnswer type: {}, value: {}",
        question.getCorrectAnswer() == null ? "null" : question.getCorrectAnswer().getClass().getSimpleName(),
        question.getCorrectAnswer());
    log.info("  - explanation type: {}, value: {}",
        question.getExplanation() == null ? "null" : question.getExplanation().getClass().getSimpleName(),
        question.getExplanation() == null ? "null" : question.getExplanation().substring(0, Math.min(50, question.getExplanation().length())));

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
  public Page<QuestionResponse> getMyQuestions(Pageable pageable) {
    UUID currentUserId = getCurrentUserId();
    log.info("Fetching questions for user: {}", currentUserId);

    return questionRepository
        .findByCreatedByAndNotDeleted(currentUserId, pageable)
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
    if (request.getPoints() != null) {
      question.setPoints(request.getPoints());
    }
    if (request.getDifficulty() != null) {
      question.setDifficulty(request.getDifficulty());
    }
    if (request.getCognitiveLevel() != null) {
      question.setCognitiveLevel(request.getCognitiveLevel());
    }
    if (request.getTags() != null) {
      question.setTags(request.getTags());
    }
    if (request.getStatus() != null) {
      question.setQuestionStatus(request.getStatus());
    }

    question.setUpdatedAt(Instant.now());
    question = questionRepository.save(question);

    log.info("Question updated: {}", id);
    return mapToResponse(question);
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
                .createdBy(currentUserId)
                .questionText(questionRequest.getQuestionText())
                .questionType(questionRequest.getQuestionType())
                .options(questionRequest.getOptions())
                .correctAnswer(questionRequest.getCorrectAnswer())
                .explanation(questionRequest.getExplanation())
                .points(questionRequest.getPoints())
                .difficulty(questionRequest.getDifficulty())
                .cognitiveLevel(questionRequest.getCognitiveLevel())
                .tags(questionRequest.getTags())
                .questionBankId(questionRequest.getQuestionBankId())
                .templateId(questionRequest.getTemplateId())
                .questionStatus(QuestionStatus.AI_DRAFT)
                .questionSourceType(QuestionSourceType.BANK_IMPORTED)
                .build();

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
    if (response.getSuccessCount() == response.getTotalRows()) {
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
  public Page<QuestionResponse> searchQuestions(
      String searchTerm, String difficulty, String type, Pageable pageable) {
    UUID currentUserId = getCurrentUserId();

    if (searchTerm != null && !searchTerm.isEmpty()) {
      String searchPattern = "%" + searchTerm.toLowerCase() + "%";
      return questionRepository
          .searchByCreatedBy(currentUserId, searchPattern, pageable)
          .map(this::mapToResponse);
    } else {
      com.fptu.math_master.enums.QuestionDifficulty difficultyEnum = null;
      if (difficulty != null && !difficulty.isBlank()) {
        try {
          difficultyEnum = com.fptu.math_master.enums.QuestionDifficulty.valueOf(difficulty);
        } catch (IllegalArgumentException ignored) {
          // Keep null to mean "no filter" (backward-compatible behavior)
        }
      }

      com.fptu.math_master.enums.QuestionType typeEnum = null;
      if (type != null && !type.isBlank()) {
        try {
          typeEnum = com.fptu.math_master.enums.QuestionType.valueOf(type);
        } catch (IllegalArgumentException ignored) {
          // Keep null to mean "no filter"
        }
      }

      return questionRepository.findByFilters(currentUserId, difficultyEnum, typeEnum, pageable)
          .map(this::mapToResponse);
    }
  }

  private QuestionResponse mapToResponse(Question question) {
    String creatorName =
        userRepository
            .findById(question.getCreatedBy())
            .map(User::getFullName)
            .orElse("Unknown");

    String bankName = null;
    if (question.getQuestionBankId() != null) {
      bankName =
          questionBankRepository
              .findById(question.getQuestionBankId())
              .map(QuestionBank::getName)
              .orElse(null);
    }

    return QuestionResponse.builder()
        .id(question.getId())
        .createdBy(question.getCreatedBy())
        .creatorName(creatorName)
        .questionText(question.getQuestionText())
        .questionType(question.getQuestionType())
        .options(question.getOptions())
        .correctAnswer(question.getCorrectAnswer())
        .explanation(question.getExplanation())
        .points(question.getPoints())
        .difficulty(question.getDifficulty())
        .cognitiveLevel(question.getCognitiveLevel())
        .questionStatus(question.getQuestionStatus())
        .questionSourceType(question.getQuestionSourceType())
        .tags(question.getTags())
        .templateId(question.getTemplateId())
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
