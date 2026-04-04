package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CanonicalQuestionRequest;
import com.fptu.math_master.dto.response.CanonicalQuestionResponse;
import com.fptu.math_master.entity.CanonicalQuestion;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CanonicalQuestionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.CanonicalQuestionService;
import com.fptu.math_master.util.SecurityUtils;
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
            .difficulty(request.getDifficulty())
            .build();
    canonicalQuestion.setCreatedBy(currentUserId);

    canonicalQuestion = canonicalQuestionRepository.save(canonicalQuestion);
    return mapToResponse(canonicalQuestion);
  }

  @Override
  @Transactional(readOnly = true)
  public CanonicalQuestionResponse getCanonicalQuestionById(UUID id) {
    CanonicalQuestion canonicalQuestion =
        canonicalQuestionRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    if (!canonicalQuestion.getCreatedBy().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.TEMPLATE_ACCESS_DENIED);
    }
    return mapToResponse(canonicalQuestion);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CanonicalQuestionResponse> getMyCanonicalQuestions(Pageable pageable) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    return canonicalQuestionRepository
        .findByCreatedByAndNotDeleted(currentUserId, pageable)
        .map(this::mapToResponse);
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
        .difficulty(canonicalQuestion.getDifficulty())
        .createdAt(canonicalQuestion.getCreatedAt())
        .updatedAt(canonicalQuestion.getUpdatedAt())
        .build();
  }
}