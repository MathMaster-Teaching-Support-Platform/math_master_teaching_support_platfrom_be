package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.QuestionBankRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.QuestionBankResponse;
import com.fptu.math_master.service.QuestionBankService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/question-banks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class QuestionBankController {

  QuestionBankService questionBankService;

  @PostMapping
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create a new question bank",
      description =
          "Teacher creates a question bank to organize questions by topic. "
              + "Name is required (max 255 chars), description is optional, "
              + "subject and grade level can be selected, and can be marked as public/private.")
  public ApiResponse<QuestionBankResponse> createQuestionBank(
      @Valid @RequestBody QuestionBankRequest request) {
    log.info("REST request to create question bank: {}", request.getName());
    return ApiResponse.<QuestionBankResponse>builder()
        .message("Question bank created successfully")
        .result(questionBankService.createQuestionBank(request))
        .build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Update question bank",
      description =
          "Teacher can edit question bank information. Only owner or admin can edit. "
              + "Warning is shown if questions are being used in assessments.")
  public ApiResponse<QuestionBankResponse> updateQuestionBank(
      @PathVariable UUID id, @Valid @RequestBody QuestionBankRequest request) {
    log.info("REST request to update question bank: {}", id);
    return ApiResponse.<QuestionBankResponse>builder()
        .message("Question bank updated successfully")
        .result(questionBankService.updateQuestionBank(id, request))
        .build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Delete question bank",
      description =
          "Teacher can delete question bank. Soft delete is performed (set deleted_at). "
              + "Questions inside are NOT deleted, only question_bank_id is set to NULL. "
              + "Confirmation required with warning if questions are in use.")
  public ApiResponse<Void> deleteQuestionBank(@PathVariable UUID id) {
    log.info("REST request to delete question bank: {}", id);
    questionBankService.deleteQuestionBank(id);
    return ApiResponse.<Void>builder()
        .message(
            "Question bank deleted successfully. Questions have been unlinked but not deleted.")
        .build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Get question bank by ID",
      description =
          "Get question bank details. Accessible by owner, public banks can be viewed by others.")
  public ApiResponse<QuestionBankResponse> getQuestionBankById(@PathVariable UUID id) {
    log.info("REST request to get question bank: {}", id);
    return ApiResponse.<QuestionBankResponse>builder()
        .result(questionBankService.getQuestionBankById(id))
        .build();
  }

  @GetMapping("/my")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "List my question banks",
      description =
          "Teacher views all their question banks in grid/list view. "
              + "Shows name, subject, grade level, question count, created date. "
              + "Supports pagination (20 items/page by default).")
  public ApiResponse<Page<QuestionBankResponse>> getMyQuestionBanks(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection) {

    log.info("REST request to get my question banks - page: {}, size: {}", page, size);

    Sort sort =
        sortDirection.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return ApiResponse.<Page<QuestionBankResponse>>builder()
        .result(questionBankService.getMyQuestionBanks(pageable))
        .build();
  }

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Search and filter question banks",
      description =
          "Search question banks with filters: subject, grade level, public/private status, and search term. "
              + "Returns both owned and public question banks. Supports sorting and pagination.")
  public ApiResponse<Page<QuestionBankResponse>> searchQuestionBanks(
      @RequestParam(required = false) String subject,
      @RequestParam(required = false) String gradeLevel,
      @RequestParam(required = false) Boolean isPublic,
      @RequestParam(required = false) String searchTerm,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection) {

    log.info(
        "REST request to search question banks - subject: {}, gradeLevel: {}, isPublic: {}, searchTerm: {}",
        subject,
        gradeLevel,
        isPublic,
        searchTerm);

    Sort sort =
        sortDirection.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return ApiResponse.<Page<QuestionBankResponse>>builder()
        .result(
            questionBankService.searchQuestionBanks(
                subject, gradeLevel, isPublic, searchTerm, pageable))
        .build();
  }

  @PatchMapping("/{id}/toggle-public")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Toggle public/private status",
      description =
          "Teacher can share question bank by toggling 'Make Public' switch. "
              + "If public: other teachers can view and copy questions. "
              + "If private: only owner can view.")
  public ApiResponse<QuestionBankResponse> togglePublicStatus(@PathVariable UUID id) {
    log.info("REST request to toggle public status for question bank: {}", id);
    return ApiResponse.<QuestionBankResponse>builder()
        .message("Question bank visibility updated successfully")
        .result(questionBankService.togglePublicStatus(id))
        .build();
  }

  @GetMapping("/{id}/can-edit")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Check if user can edit question bank",
      description =
          "Verify if current user has permission to edit the question bank (owner or admin).")
  public ApiResponse<Boolean> canEditQuestionBank(@PathVariable UUID id) {
    log.info("REST request to check edit permission for question bank: {}", id);
    return ApiResponse.<Boolean>builder()
        .result(questionBankService.canEditQuestionBank(id))
        .build();
  }

  @GetMapping("/{id}/can-delete")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Check if user can delete question bank",
      description =
          "Verify if current user has permission to delete the question bank (owner or admin).")
  public ApiResponse<Boolean> canDeleteQuestionBank(@PathVariable UUID id) {
    log.info("REST request to check delete permission for question bank: {}", id);
    return ApiResponse.<Boolean>builder()
        .result(questionBankService.canDeleteQuestionBank(id))
        .build();
  }
}
