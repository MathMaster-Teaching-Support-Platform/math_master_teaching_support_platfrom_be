package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.dto.request.BulkApproveQuestionsRequest;
import com.fptu.math_master.dto.request.ImportQuestionsRequest;
import com.fptu.math_master.dto.request.UpdateQuestionRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.ImportQuestionsResponse;
import com.fptu.math_master.dto.response.QuestionResponse;
import com.fptu.math_master.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Questions", description = "Question CRUD operations and batch import")
public class QuestionController {

  QuestionService questionService;

  @PostMapping
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create a new question",
      description = "Teacher creates a single question with text, type, options, and answer")
  public ApiResponse<QuestionResponse> createQuestion(
      @Valid @RequestBody CreateQuestionRequest request) {
    log.info("REST request to create question");
    return ApiResponse.<QuestionResponse>builder()
        .message("Question created successfully")
        .result(questionService.createQuestion(request))
        .build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(summary = "Get question by ID", description = "Retrieve question details by ID")
  public ApiResponse<QuestionResponse> getQuestion(@PathVariable UUID id) {
    log.info("REST request to get question: {}", id);
    return ApiResponse.<QuestionResponse>builder()
        .message("Question retrieved successfully")
        .result(questionService.getQuestionById(id))
        .build();
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get my questions",
      description = "Retrieve all questions created by current teacher with pagination")
  public ApiResponse<Page<QuestionResponse>> getMyQuestions(
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt")
          String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sort) {

    log.info("REST request to get my questions: page={}, size={}", page, size);

    Sort.Direction sortDirection = Sort.Direction.fromString(sort.toUpperCase());
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    return ApiResponse.<Page<QuestionResponse>>builder()
        .message("Questions retrieved successfully")
        .result(questionService.getMyQuestions(pageable))
        .build();
  }

  @GetMapping("/bank/{bankId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get questions by bank",
      description = "Retrieve all questions in a specific question bank")
  public ApiResponse<Page<QuestionResponse>> getQuestionsByBank(
      @PathVariable UUID bankId,
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

    log.info("REST request to get questions for bank: {}", bankId);

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    return ApiResponse.<Page<QuestionResponse>>builder()
        .message("Questions retrieved successfully")
        .result(questionService.getQuestionsByBank(bankId, pageable))
        .build();
  }

  @GetMapping("/template/{templateId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get questions by template",
      description = "Retrieve all questions generated from a specific template")
  public ApiResponse<List<QuestionResponse>> getQuestionsByTemplate(@PathVariable UUID templateId) {
    log.info("REST request to get questions from template: {}", templateId);
    return ApiResponse.<List<QuestionResponse>>builder()
        .message("Questions retrieved successfully")
        .result(questionService.getQuestionsByTemplate(templateId))
        .build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Update question",
      description = "Update question details (only by creator or admin)")
  public ApiResponse<QuestionResponse> updateQuestion(
      @PathVariable UUID id, @Valid @RequestBody UpdateQuestionRequest request) {
    log.info("REST request to update question: {}", id);
    return ApiResponse.<QuestionResponse>builder()
        .message("Question updated successfully")
        .result(questionService.updateQuestion(id, request))
        .build();
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Approve AI draft question",
      description = "Approve a question currently in AI_DRAFT status so it can be used downstream.")
  public ApiResponse<QuestionResponse> approveQuestion(@PathVariable UUID id) {
    log.info("REST request to approve question: {}", id);
    return ApiResponse.<QuestionResponse>builder()
        .message("Question approved successfully")
        .result(questionService.approveQuestion(id))
        .build();
  }

  @PostMapping("/bulk-approve")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Bulk approve AI draft questions",
      description = "Approve multiple AI_DRAFT questions in one call.")
  public ApiResponse<Integer> bulkApproveQuestions(
      @Valid @RequestBody BulkApproveQuestionsRequest request) {
    log.info("REST request to bulk approve {} questions", request.getQuestionIds().size());
    Integer approved = questionService.bulkApproveQuestions(request.getQuestionIds());
    return ApiResponse.<Integer>builder()
        .message("Bulk approve completed")
        .result(approved)
        .build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(summary = "Delete question", description = "Soft delete a question (only by creator)")
  public ApiResponse<Void> deleteQuestion(@PathVariable UUID id) {
    log.info("REST request to delete question: {}", id);
    questionService.deleteQuestion(id);
    return ApiResponse.<Void>builder().message("Question deleted successfully").build();
  }

  @PostMapping("/search")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Search questions",
      description = "Search questions with filters (search term, difficulty, type)")
  public ApiResponse<Page<QuestionResponse>> searchQuestions(
      @Parameter(description = "Search term") @RequestParam(required = false) String search,
      @Parameter(description = "Difficulty level filter") @RequestParam(required = false)
          String difficulty,
      @Parameter(description = "Question type filter") @RequestParam(required = false) String type,
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

    log.info(
        "REST request to search questions: search={}, difficulty={}, type={}",
        search,
        difficulty,
        type);

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    return ApiResponse.<Page<QuestionResponse>>builder()
        .message("Questions found successfully")
        .result(questionService.searchQuestions(search, difficulty, type, pageable))
        .build();
  }

  @PostMapping("/import")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Import questions from file",
      description =
          "Batch import questions from CSV file. "
              + "CSV format: question_text, question_type, cognitive_level, correct_answer, "
              + "explanation, points, difficulty, options (A:value,B:value), tags")
  public ApiResponse<ImportQuestionsResponse> importQuestions(
      @Valid @RequestBody ImportQuestionsRequest request) {
    log.info("REST request to import questions");
    return ApiResponse.<ImportQuestionsResponse>builder()
        .message("Questions imported successfully")
        .result(questionService.importQuestionsFromFile(request))
        .build();
  }

  @PostMapping(value = "/import-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Import questions from uploaded file",
      description = "Upload a CSV file to batch import questions")
  public ApiResponse<ImportQuestionsResponse> importQuestionsFromFile(
      @RequestPart(value = "file") MultipartFile file,
      @RequestPart(value = "bankId", required = false) UUID bankId,
      @RequestPart(value = "continueOnError", required = false) Boolean continueOnError) {

    log.info(
        "REST request to import questions from file: {}, bankId={}",
        file.getOriginalFilename(),
        bankId);

    try {
      String fileContent = new String(file.getBytes(), "UTF-8");

      ImportQuestionsRequest request =
          ImportQuestionsRequest.builder()
              .fileContent(fileContent)
              .fileFormat("CSV")
              .questionBankId(bankId)
              .continueOnError(continueOnError != null ? continueOnError : true)
              .build();

      return ApiResponse.<ImportQuestionsResponse>builder()
          .message("Questions imported successfully")
          .result(questionService.importQuestionsFromFile(request))
          .build();

    } catch (Exception e) {
      log.error("Error reading file: {}", e.getMessage());
      throw new RuntimeException("Failed to read file: " + e.getMessage());
    }
  }
}
