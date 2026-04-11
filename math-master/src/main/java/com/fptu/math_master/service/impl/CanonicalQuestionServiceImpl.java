package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CanonicalQuestionRequest;
import com.fptu.math_master.dto.request.GenerateCanonicalQuestionsRequest;
import com.fptu.math_master.dto.response.CanonicalQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionsBatchResponse;
import com.fptu.math_master.dto.response.QuestionResponse;
import com.fptu.math_master.entity.CanonicalQuestion;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CanonicalQuestionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.CanonicalQuestionService;
import com.fptu.math_master.service.QuestionService;
import com.fptu.math_master.service.QuestionTemplateService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.UUID;
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
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CanonicalQuestionServiceImpl implements CanonicalQuestionService {

  CanonicalQuestionRepository canonicalQuestionRepository;
  UserRepository userRepository;
  QuestionTemplateService questionTemplateService;
  QuestionService questionService;

  @Override
  @Transactional
  public CanonicalQuestionResponse createCanonicalQuestion(CanonicalQuestionRequest request) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    CanonicalQuestion canonicalQuestion =
        CanonicalQuestion.builder()
            .title(request.getTitle())
            .problemText(request.getProblemText())
            .solutionSteps(request.getSolutionSteps())
            .diagramDefinition(request.getDiagramDefinition())
            .problemType(request.getProblemType())
        .cognitiveLevel(request.getCognitiveLevel())
            .build();
    canonicalQuestion.setCreatedBy(currentUserId);

    canonicalQuestion = canonicalQuestionRepository.save(canonicalQuestion);
    return mapToResponse(canonicalQuestion);
  }

  @Override
  @Transactional
  public CanonicalQuestionResponse updateCanonicalQuestion(UUID id, CanonicalQuestionRequest request) {
    CanonicalQuestion canonicalQuestion = getCanonicalQuestionAndValidateAccess(id);
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    canonicalQuestion.setTitle(request.getTitle());
    canonicalQuestion.setProblemText(request.getProblemText());
    canonicalQuestion.setSolutionSteps(request.getSolutionSteps());
    canonicalQuestion.setDiagramDefinition(request.getDiagramDefinition());
    canonicalQuestion.setProblemType(request.getProblemType());
    canonicalQuestion.setCognitiveLevel(request.getCognitiveLevel());
    canonicalQuestion.setUpdatedBy(currentUserId);
    canonicalQuestion.setUpdatedAt(Instant.now());

    canonicalQuestion = canonicalQuestionRepository.save(canonicalQuestion);
    return mapToResponse(canonicalQuestion);
  }

  @Override
  @Transactional
  public void deleteCanonicalQuestion(UUID id) {
    CanonicalQuestion canonicalQuestion = getCanonicalQuestionAndValidateAccess(id);
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    canonicalQuestion.setDeletedAt(Instant.now());
    canonicalQuestion.setDeletedBy(currentUserId);
    canonicalQuestion.setUpdatedBy(currentUserId);
    canonicalQuestion.setUpdatedAt(Instant.now());
    canonicalQuestionRepository.save(canonicalQuestion);
  }

  @Override
  @Transactional(readOnly = true)
  public CanonicalQuestionResponse getCanonicalQuestionById(UUID id) {
    CanonicalQuestion canonicalQuestion = getCanonicalQuestionAndValidateAccess(id);
    return mapToResponse(canonicalQuestion);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionResponse> getQuestionsByCanonicalQuestion(UUID id, Pageable pageable) {
    getCanonicalQuestionAndValidateAccess(id);
    return questionService.getQuestionsByCanonicalQuestion(id, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CanonicalQuestionResponse> getMyCanonicalQuestions(Pageable pageable) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    return canonicalQuestionRepository
        .findByCreatedByAndNotDeleted(currentUserId, pageable)
        .map(this::mapToResponse);
  }

  @Override
  @Transactional
  public GeneratedQuestionsBatchResponse generateQuestionsFromCanonical(
      UUID canonicalQuestionId, GenerateCanonicalQuestionsRequest request) {
    getCanonicalQuestionAndValidateAccess(canonicalQuestionId);

    return questionTemplateService.generateQuestionsFromCanonical(canonicalQuestionId, request);
  }

  private CanonicalQuestion getCanonicalQuestionAndValidateAccess(UUID id) {
    CanonicalQuestion canonicalQuestion =
        canonicalQuestionRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    if (!canonicalQuestion.getCreatedBy().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.TEMPLATE_ACCESS_DENIED);
    }
    return canonicalQuestion;
  }

  private CanonicalQuestionResponse mapToResponse(CanonicalQuestion canonicalQuestion) {
    String creatorName =
        userRepository
            .findById(canonicalQuestion.getCreatedBy())
            .map(User::getFullName)
            .orElse("Unknown");

    return CanonicalQuestionResponse.builder()
        .id(canonicalQuestion.getId())
        .createdBy(canonicalQuestion.getCreatedBy())
        .creatorName(creatorName)
        .title(canonicalQuestion.getTitle())
        .problemText(canonicalQuestion.getProblemText())
        .solutionSteps(canonicalQuestion.getSolutionSteps())
        .diagramDefinition(canonicalQuestion.getDiagramDefinition())
        .problemType(canonicalQuestion.getProblemType())
        .cognitiveLevel(canonicalQuestion.getCognitiveLevel())
        .createdAt(canonicalQuestion.getCreatedAt())
        .updatedAt(canonicalQuestion.getUpdatedAt())
        .build();
  }
}