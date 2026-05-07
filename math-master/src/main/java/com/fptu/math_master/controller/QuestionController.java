package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.BulkApproveQuestionsRequest;
import com.fptu.math_master.dto.request.BulkAssignQuestionsToBankRequest;
import com.fptu.math_master.dto.request.BulkRejectQuestionsRequest;
import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.dto.request.ImportQuestionsRequest;
import com.fptu.math_master.dto.request.QuestionBatchImportRequest;
import com.fptu.math_master.dto.request.SetClausePointsRequest;
import com.fptu.math_master.dto.request.UpdateQuestionRequest;
import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.ImportQuestionsResponse;
import com.fptu.math_master.dto.response.QuestionBatchImportResponse;
import com.fptu.math_master.dto.response.QuestionExcelPreviewResponse;
import com.fptu.math_master.dto.response.QuestionResponse;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.service.QuestionExcelImportService;
import com.fptu.math_master.service.QuestionService;
import com.fptu.math_master.service.AIEnhancementService;
import org.springframework.http.ResponseEntity;
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
  QuestionExcelImportService questionExcelImportService;
  AIEnhancementService aiEnhancementService;

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
      description = "Retrieve all questions created by current teacher with pagination and optional search")
  public ApiResponse<Page<QuestionResponse>> getMyQuestions(
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt")
          String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sort,
      @Parameter(description = "Search by question text") @RequestParam(required = false) String name,
      @Parameter(description = "Search by tag") @RequestParam(required = false) String tag,
      @Parameter(description = "Filter by grade (school_grade_id)")
          @RequestParam(required = false)
          UUID gradeId,
      @Parameter(description = "Filter by chapter") @RequestParam(required = false) UUID chapterId) {

    log.info(
        "REST request to get my questions: page={}, size={}, name={}, gradeId={}, chapterId={}",
        page,
        size,
        name,
        gradeId,
        chapterId);

    Sort.Direction sortDirection = Sort.Direction.fromString(sort.toUpperCase());
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    return ApiResponse.<Page<QuestionResponse>>builder()
        .message("Questions retrieved successfully")
        .result(questionService.getMyQuestions(name, tag, gradeId, chapterId, pageable))
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
      summary = "Bulk approve questions in review",
      description = "Approve multiple UNDER_REVIEW (or legacy AI_DRAFT) questions in one call.")
  public ApiResponse<Integer> bulkApproveQuestions(
      @Valid @RequestBody BulkApproveQuestionsRequest request) {
    log.info("REST request to bulk approve {} questions", request.getQuestionIds().size());
    Integer approved = questionService.bulkApproveQuestions(request.getQuestionIds());
    return ApiResponse.<Integer>builder()
        .message("Bulk approve completed")
        .result(approved)
        .build();
  }

  @PostMapping("/bulk-reject")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Bulk reject questions in review",
      description =
          "Reject multiple UNDER_REVIEW (or legacy AI_DRAFT) questions in one call. Rejected "
              + "questions move to ARCHIVED and leave the review queue.")
  public ApiResponse<Integer> bulkRejectQuestions(
      @Valid @RequestBody BulkRejectQuestionsRequest request) {
    log.info("REST request to bulk reject {} questions", request.getQuestionIds().size());
    Integer rejected =
        questionService.bulkRejectQuestions(request.getQuestionIds(), request.getReason());
    return ApiResponse.<Integer>builder()
        .message("Bulk reject completed")
        .result(rejected)
        .build();
  }

  @GetMapping("/review-queue")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "List my questions in review",
      description =
          "Returns the caller's UNDER_REVIEW (and legacy AI_DRAFT) questions, optionally filtered "
              + "to one template. Used by the FE review-queue page after a generation batch.")
  public ApiResponse<Page<QuestionResponse>> reviewQueue(
      @RequestParam(required = false) UUID templateId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    log.info("REST request review-queue templateId={} page={} size={}", templateId, page, size);
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<QuestionResponse>>builder()
        .message("Review queue retrieved")
        .result(questionService.listReviewQueue(templateId, pageable))
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
      summary = "Search questions (legacy)",
      description = "Search questions with filters (search term, type). Use GET /questions/search for new flow.")
  public ApiResponse<Page<QuestionResponse>> searchQuestions(
      @Parameter(description = "Search term") @RequestParam(required = false) String search,
      @Parameter(description = "Question type filter") @RequestParam(required = false) String type,
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

    log.info("REST request to search questions: search={}, type={}", search, type);

    // Use database column name for native query, Java property name for JPQL
    String sortProperty = (search != null && !search.isEmpty()) ? "created_at" : "createdAt";
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortProperty));

    return ApiResponse.<Page<QuestionResponse>>builder()
        .message("Questions found successfully")
        .result(questionService.searchQuestions(search, type, pageable))
        .build();
  }

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Search questions by keyword and tags",
      description =
          "Full-text search on question content (keyword) with optional multi-tag filter, "
              + "and optional narrowing by chapter + cognitive level. "
              + "tags param accepts multiple values (e.g. ?tags=TH&tags=VD). "
              + "cognitiveLevel accepts the Vietnamese bucket name (NHAN_BIET / THONG_HIEU / "
              + "VAN_DUNG / VAN_DUNG_CAO); Bloom-style English levels stored on questions are "
              + "folded onto the matching bucket server-side. Supports pagination.")
  public ApiResponse<Page<QuestionResponse>> searchByKeywordAndTags(
      @Parameter(description = "Keyword to search in question text") @RequestParam(required = false) String keyword,
      @Parameter(description = "Tag filter (multi-value)") @RequestParam(required = false) List<String> tags,
      @Parameter(description = "Narrow to a single chapter") @RequestParam(required = false) UUID chapterId,
      @Parameter(description = "Narrow to a Vietnamese cognitive bucket (NHAN_BIET/THONG_HIEU/VAN_DUNG/VAN_DUNG_CAO)")
          @RequestParam(required = false)
          String cognitiveLevel,
      @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

    log.info(
        "REST request to search questions by keyword={} tags={} chapterId={} cognitiveLevel={}",
        keyword,
        tags,
        chapterId,
        cognitiveLevel);
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<QuestionResponse>>builder()
        .message("Questions found successfully")
        .result(questionService.searchByKeywordAndTags(keyword, tags, chapterId, cognitiveLevel, pageable))
        .build();
  }

  @PatchMapping("/bank/{bankId}/batch-assign")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Batch assign questions to a bank",
      description =
          "Assign multiple existing questions into one question bank in a single request.")
  public ApiResponse<Integer> batchAssignQuestionsToBank(
      @PathVariable UUID bankId,
      @Valid @RequestBody BulkAssignQuestionsToBankRequest request) {

    log.info(
        "REST request to batch assign {} questions to bank {}",
        request.getQuestionIds().size(),
        bankId);

    Integer assignedCount = questionService.assignQuestionsToBank(bankId, request.getQuestionIds());
    return ApiResponse.<Integer>builder()
        .message("Questions assigned to bank successfully")
        .result(assignedCount)
        .build();
  }

  @PatchMapping("/bank/{bankId}/batch-remove")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Batch remove questions from a bank",
      description =
          "Remove multiple existing questions from one question bank in a single request.")
  public ApiResponse<Integer> batchRemoveQuestionsFromBank(
      @PathVariable UUID bankId,
      @Valid @RequestBody BulkAssignQuestionsToBankRequest request) {

    log.info(
        "REST request to batch remove {} questions from bank {}",
        request.getQuestionIds().size(),
        bankId);

    Integer removedCount = questionService.removeQuestionsFromBank(bankId, request.getQuestionIds());
    return ApiResponse.<Integer>builder()
        .message("Questions removed from bank successfully")
        .result(removedCount)
        .build();
  }

  @PostMapping("/import")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Import questions from file",
      description =
          "Batch import questions from CSV file. "
              + "CSV format: question_text, question_type, cognitive_level, correct_answer, "
              + "explanation, points, options (A:value,B:value), tags")
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

  @PostMapping(value = "/bulk-import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Preview Excel Bulk Import for Questions",
      description = "Upload Excel file (.xlsx) and preview parsed questions with validation results.")
  public ApiResponse<QuestionExcelPreviewResponse> previewBulkImport(
      @RequestParam("file") MultipartFile file) {
    log.info("REST request to preview question bulk import from Excel: {}", file.getOriginalFilename());
    QuestionExcelPreviewResponse response = questionExcelImportService.previewExcelImport(file);
    String message =
        String.format(
            "Preview completed: %d valid, %d invalid rows out of %d total",
            response.getValidRows(), response.getInvalidRows(), response.getTotalRows());
    return ApiResponse.<QuestionExcelPreviewResponse>builder().message(message).result(response).build();
  }

  @PostMapping("/bulk-import/submit")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Submit Question Bulk Import",
      description = "Import validated questions in batch. Returns success/failure summary.")
  public ApiResponse<QuestionBatchImportResponse> submitBulkImport(
      @Valid @RequestBody QuestionBatchImportRequest request) {
    log.info("REST request to submit question bulk import with {} questions", request.getQuestions().size());
    QuestionBatchImportResponse response = questionExcelImportService.importQuestionsBatch(request);
    String message =
        String.format(
            "Import completed: %d succeeded, %d failed out of %d total",
            response.getSuccessCount(), response.getFailedCount(), response.getTotalRows());
    return ApiResponse.<QuestionBatchImportResponse>builder().message(message).result(response).build();
  }

  @GetMapping("/bulk-import/template")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Download Excel Template for Question Import",
      description = "Download a blank Excel template with headers and example rows for bulk question import.")
  public ResponseEntity<byte[]> downloadExcelTemplate() {
    log.info("REST request to download question Excel import template");
    byte[] excelBytes = questionExcelImportService.generateExcelTemplate();
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=question_import.xlsx")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excelBytes);
  }

  @PostMapping("/enhance")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Enhance question with AI",
      description = "Use AI to generate explanation and solution steps for a question. Works for all question types (MCQ, TF, SA).")
  public ApiResponse<AIEnhancedQuestionResponse> enhanceQuestion(
      @Valid @RequestBody AIEnhancementRequest request) {
    log.info("REST request to enhance question with AI: type={}", request.getQuestionType());
    AIEnhancedQuestionResponse response = aiEnhancementService.enhanceQuestion(request);
    
    String message;
    if (response.isEnhanced() && response.isValid()) {
      message = "Question enhanced successfully with AI-generated explanation and solution steps.";
    } else if (!response.isValid()) {
      message = "AI enhancement validation failed. Errors: " + String.join(", ", response.getValidationErrors());
    } else {
      message = "AI enhancement failed. Please try again.";
    }
    
    return ApiResponse.<AIEnhancedQuestionResponse>builder()
        .message(message)
        .result(response)
        .build();
  }

  // ===========================================================================
  // FEATURE 4: Set Overdrive Points Per TF Clause
  // POST /questions/{questionId}/clauses/points
  // ===========================================================================

  @PostMapping("/{questionId}/clauses/points")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Set Overdrive Points Per TF Clause (Feature 4)",
      description =
          "For TRUE_FALSE questions: teacher sets how many points each clause (A/B/C/D) is worth."
              + " Validation: sum(clause_points) must equal total_point."
              + " Persists overdrive_point per clause into the options JSONB column."
              + " Example: total=1.0, A=0.25, B=0.25, C=0.25, D=0.25")
  public ApiResponse<QuestionResponse> setClausePoints(
      @PathVariable UUID questionId,
      @Valid @RequestBody SetClausePointsRequest request) {
    log.info("REST [Feature4] set clause points for questionId={}", questionId);
    aiEnhancementService.setClausePoints(questionId, request);
    return ApiResponse.<QuestionResponse>builder()
        .message(String.format("Clause points set: %s (total=%.2f)",
            request.getClausePoints(), request.getTotalPoint().doubleValue()))
        .result(questionService.getQuestionById(questionId))
        .build();
  }
}

