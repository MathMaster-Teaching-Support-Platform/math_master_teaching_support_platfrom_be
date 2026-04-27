package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.RejectWithdrawalRequest;
import com.fptu.math_master.dto.response.AdminWithdrawalRequestResponse;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.enums.WithdrawalStatus;
import com.fptu.math_master.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Admin endpoints for managing manual withdrawal requests.
 * Requires ADMIN role.
 */
@RestController
@RequestMapping("/admin/withdrawal-requests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin — Withdrawal", description = "Admin APIs for managing manual withdrawal requests")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWithdrawalController {

  WithdrawalService withdrawalService;

  @Operation(
      summary = "List all withdrawal requests",
      description = "Paginated list with optional status filter and full-text search across userName, userEmail, bankAccountNumber.")
  @GetMapping
  public ApiResponse<Page<AdminWithdrawalRequestResponse>> getWithdrawalRequests(
      @RequestParam(required = false) WithdrawalStatus status,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String order) {

    Sort sort = "ASC".equalsIgnoreCase(order) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    return ApiResponse.<Page<AdminWithdrawalRequestResponse>>builder()
        .result(withdrawalService.getAdminRequests(status, search, pageable))
        .build();
  }

  @Operation(
      summary = "Process (approve) a withdrawal request",
      description = "Upload the bank transfer proof image, debit the user's wallet balance, create a WITHDRAWAL transaction, and send a bill email to the user.")
  @PutMapping(value = "/{withdrawalRequestId}/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<AdminWithdrawalRequestResponse> processWithdrawal(
      @PathVariable UUID withdrawalRequestId,
      @RequestPart("proofImage") MultipartFile proofImage,
      @RequestPart(value = "adminNote", required = false) String adminNote) {

    return ApiResponse.<AdminWithdrawalRequestResponse>builder()
        .result(withdrawalService.processWithdrawal(withdrawalRequestId, proofImage, adminNote))
        .build();
  }

  @Operation(
      summary = "Reject a withdrawal request",
      description = "Sets status to REJECTED with a reason and sends a rejection email to the user.")
  @PutMapping("/{withdrawalRequestId}/reject")
  public ApiResponse<AdminWithdrawalRequestResponse> rejectWithdrawal(
      @PathVariable UUID withdrawalRequestId,
      @Valid @RequestBody RejectWithdrawalRequest request) {

    return ApiResponse.<AdminWithdrawalRequestResponse>builder()
        .result(withdrawalService.rejectWithdrawal(withdrawalRequestId, request))
        .build();
  }
}
