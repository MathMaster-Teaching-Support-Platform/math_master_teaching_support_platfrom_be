package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CanonicalQuestionRequest;
import com.fptu.math_master.dto.request.GenerateCanonicalQuestionsRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CanonicalQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionsBatchResponse;
import com.fptu.math_master.service.CanonicalQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/canonical-questions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class CanonicalQuestionController {

  CanonicalQuestionService canonicalQuestionService;

  @PostMapping
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(summary = "Create canonical question", description = "Teacher creates canonical question as math source-of-truth.")
  public ApiResponse<CanonicalQuestionResponse> createCanonicalQuestion(
      @Valid @RequestBody CanonicalQuestionRequest request) {
    return ApiResponse.<CanonicalQuestionResponse>builder()
        .message("Canonical question created successfully")
        .result(canonicalQuestionService.createCanonicalQuestion(request))
        .build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(summary = "Get canonical question", description = "Get canonical question detail owned by current teacher.")
  public ApiResponse<CanonicalQuestionResponse> getCanonicalQuestionById(@PathVariable UUID id) {
    return ApiResponse.<CanonicalQuestionResponse>builder()
        .result(canonicalQuestionService.getCanonicalQuestionById(id))
        .build();
  }

  @GetMapping("/my")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(summary = "List my canonical questions", description = "List canonical questions created by current teacher.")
  public ApiResponse<Page<CanonicalQuestionResponse>> getMyCanonicalQuestions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection) {

    Sort sort =
        sortDirection.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return ApiResponse.<Page<CanonicalQuestionResponse>>builder()
        .result(canonicalQuestionService.getMyCanonicalQuestions(pageable))
        .build();
  }

  @PostMapping("/{id}/generate-questions")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Generate questions from canonical question",
      description =
          "Generate AI_DRAFT questions by combining canonical prompt structure with a selected parameterized template.")
  public ApiResponse<GeneratedQuestionsBatchResponse> generateQuestionsFromCanonical(
      @PathVariable UUID id, @Valid @RequestBody GenerateCanonicalQuestionsRequest request) {
    log.info(
        "REST request to generate {} questions from canonical {} using template {}",
        request.getCount(),
        id,
        request.getTemplateId());
    return ApiResponse.<GeneratedQuestionsBatchResponse>builder()
        .message("Questions generated from canonical flow and saved as AI_DRAFT.")
        .result(canonicalQuestionService.generateQuestionsFromCanonical(id, request))
        .build();
  }
}