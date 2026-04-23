package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.RefundRequestRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.RefundRequestResponse;
import com.fptu.math_master.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Refund Management", description = "APIs for managing refund requests")
public class RefundController {

  RefundService refundService;

  @PostMapping("/orders/{orderId}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Create refund request", description = "Create a refund request for an order")
  public ResponseEntity<ApiResponse<RefundRequestResponse>> createRefundRequest(
      @PathVariable UUID orderId,
      @Valid @RequestBody RefundRequestRequest request) {
    log.info("Creating refund request for order: {}", orderId);
    RefundRequestResponse response = refundService.createRefundRequest(orderId, request);
    return ResponseEntity.ok(ApiResponse.<RefundRequestResponse>builder()
        .code(200)
        .message("Refund request created successfully")
        .result(response)
        .build());
  }

  @GetMapping("/{refundRequestId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
  @Operation(summary = "Get refund request", description = "Get refund request details by ID")
  public ResponseEntity<ApiResponse<RefundRequestResponse>> getRefundRequest(
      @PathVariable UUID refundRequestId) {
    log.info("Getting refund request: {}", refundRequestId);
    RefundRequestResponse response = refundService.getRefundRequest(refundRequestId);
    return ResponseEntity.ok(ApiResponse.<RefundRequestResponse>builder()
        .code(200)
        .message("Refund request retrieved successfully")
        .result(response)
        .build());
  }

  @GetMapping("/my-requests")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get my refund requests", description = "Get all refund requests for the current student")
  public ResponseEntity<ApiResponse<Page<RefundRequestResponse>>> getMyRefundRequests(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info("Getting my refund requests - page: {}, size: {}", page, size);
    Pageable pageable = PageRequest.of(page, size);
    Page<RefundRequestResponse> response = refundService.getMyRefundRequests(pageable);
    return ResponseEntity.ok(ApiResponse.<Page<RefundRequestResponse>>builder()
        .code(200)
        .message("Refund requests retrieved successfully")
        .result(response)
        .build());
  }

  @GetMapping("/pending")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get pending refund requests", description = "Get all pending refund requests (admin only)")
  public ResponseEntity<ApiResponse<Page<RefundRequestResponse>>> getPendingRefundRequests(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info("Getting pending refund requests - page: {}, size: {}", page, size);
    Pageable pageable = PageRequest.of(page, size);
    Page<RefundRequestResponse> response = refundService.getPendingRefundRequests(pageable);
    return ResponseEntity.ok(ApiResponse.<Page<RefundRequestResponse>>builder()
        .code(200)
        .message("Pending refund requests retrieved successfully")
        .result(response)
        .build());
  }

  @PostMapping("/{refundRequestId}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Approve refund request", description = "Approve a refund request (admin only)")
  public ResponseEntity<ApiResponse<RefundRequestResponse>> approveRefundRequest(
      @PathVariable UUID refundRequestId,
      @RequestParam(required = false) String adminNotes) {
    log.info("Approving refund request: {}", refundRequestId);
    RefundRequestResponse response = refundService.approveRefundRequest(refundRequestId, adminNotes);
    return ResponseEntity.ok(ApiResponse.<RefundRequestResponse>builder()
        .code(200)
        .message("Refund request approved successfully")
        .result(response)
        .build());
  }

  @PostMapping("/{refundRequestId}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Reject refund request", description = "Reject a refund request (admin only)")
  public ResponseEntity<ApiResponse<RefundRequestResponse>> rejectRefundRequest(
      @PathVariable UUID refundRequestId,
      @RequestParam String adminNotes) {
    log.info("Rejecting refund request: {}", refundRequestId);
    RefundRequestResponse response = refundService.rejectRefundRequest(refundRequestId, adminNotes);
    return ResponseEntity.ok(ApiResponse.<RefundRequestResponse>builder()
        .code(200)
        .message("Refund request rejected successfully")
        .result(response)
        .build());
  }

  @PostMapping("/{refundRequestId}/cancel")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Cancel refund request", description = "Cancel a pending refund request")
  public ResponseEntity<ApiResponse<RefundRequestResponse>> cancelRefundRequest(
      @PathVariable UUID refundRequestId) {
    log.info("Cancelling refund request: {}", refundRequestId);
    RefundRequestResponse response = refundService.cancelRefundRequest(refundRequestId);
    return ResponseEntity.ok(ApiResponse.<RefundRequestResponse>builder()
        .code(200)
        .message("Refund request cancelled successfully")
        .result(response)
        .build());
  }
}
