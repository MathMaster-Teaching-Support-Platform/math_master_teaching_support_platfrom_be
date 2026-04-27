package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.VerifyWithdrawalOtpRequest;
import com.fptu.math_master.dto.request.WithdrawalRequestDto;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.WithdrawalRequestResponse;
import com.fptu.math_master.enums.WithdrawalStatus;
import com.fptu.math_master.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User-facing endpoints for manual withdrawal requests.
 * All endpoints require authentication. No ADMIN role restriction.
 */
@RestController
@RequestMapping("/withdrawal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Withdrawal", description = "APIs for manual withdrawal requests")
@SecurityRequirement(name = "bearerAuth")
public class WithdrawalController {

  WithdrawalService withdrawalService;

  @Operation(
      summary = "Submit withdrawal request",
      description = "Validates password and wallet balance, creates a PENDING_VERIFY request and sends a 6-digit OTP to the user's email (valid 10 minutes).")
  @PostMapping("/request")
  public ApiResponse<WithdrawalRequestResponse> createWithdrawalRequest(
      @Valid @RequestBody WithdrawalRequestDto request) {
    UUID userId = getCurrentUserId();
    return ApiResponse.<WithdrawalRequestResponse>builder()
        .result(withdrawalService.createWithdrawalRequest(userId, request))
        .build();
  }

  @Operation(
      summary = "Verify withdrawal OTP",
      description = "Verifies the 6-digit OTP. On success the request moves to PENDING_ADMIN. On expiry the request is cancelled.")
  @PostMapping("/verify-otp")
  public ApiResponse<WithdrawalRequestResponse> verifyOtp(
      @Valid @RequestBody VerifyWithdrawalOtpRequest request) {
    UUID userId = getCurrentUserId();
    return ApiResponse.<WithdrawalRequestResponse>builder()
        .result(withdrawalService.verifyOtp(userId, request))
        .build();
  }

  @Operation(
      summary = "Get my withdrawal requests",
      description = "Paginated list of the current user's withdrawal history. Optionally filter by status.")
  @GetMapping("/my-requests")
  public ApiResponse<Page<WithdrawalRequestResponse>> getMyRequests(
      @RequestParam(required = false) WithdrawalStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    UUID userId = getCurrentUserId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ApiResponse.<Page<WithdrawalRequestResponse>>builder()
        .result(withdrawalService.getMyRequests(userId, status, pageable))
        .build();
  }

  @Operation(
      summary = "Cancel withdrawal request",
      description = "User can cancel a request only while it is in PENDING_VERIFY or PENDING_ADMIN status.")
  @PutMapping("/{withdrawalRequestId}/cancel")
  public ApiResponse<WithdrawalRequestResponse> cancelRequest(
      @PathVariable UUID withdrawalRequestId) {
    UUID userId = getCurrentUserId();
    return ApiResponse.<WithdrawalRequestResponse>builder()
        .result(withdrawalService.cancelRequest(userId, withdrawalRequestId))
        .build();
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return UUID.fromString(jwtAuth.getToken().getSubject());
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }
}
