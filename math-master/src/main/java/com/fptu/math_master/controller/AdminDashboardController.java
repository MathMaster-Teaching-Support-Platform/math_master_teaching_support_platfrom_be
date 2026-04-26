package com.fptu.math_master.controller;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.response.AdminDashboardStatsResponse;
import com.fptu.math_master.dto.response.AdminQuickStatsResponse;
import com.fptu.math_master.dto.response.AdminRevenueByMonthResponse;
import com.fptu.math_master.dto.response.AdminSystemStatusResponse;
import com.fptu.math_master.dto.response.AdminTransactionPageResponse;
import com.fptu.math_master.dto.response.AdminTransactionResponse;
import com.fptu.math_master.dto.response.AdminTransactionStatsResponse;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.service.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "APIs for admin dashboard statistics and monitoring")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

  private final AdminDashboardService adminDashboardService;

  @GetMapping("/dashboard/stats")
  @Operation(
      summary = "Get dashboard stats",
      description = "Returns total users, revenue, enrollments and transactions for the given month, each with growth % vs previous month.")
  public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats(
      @RequestParam(required = false) String month) {
    return ResponseEntity.ok()
        .header("Cache-Control", "private, max-age=30")
        .body(
            ApiResponse.<AdminDashboardStatsResponse>builder()
                .result(adminDashboardService.getDashboardStats(month))
                .build());
  }

  @GetMapping("/dashboard/revenue-by-month")
  @Operation(
      summary = "Get monthly revenue for a year",
      description = "Returns 12 monthly revenue totals (VNĐ) for the specified year.")
  public ResponseEntity<ApiResponse<AdminRevenueByMonthResponse>> getRevenueByMonth(
      @RequestParam(required = false) Integer year) {
    int targetYear = (year != null) ? year : Year.now().getValue();
    return ResponseEntity.ok()
        .header("Cache-Control", "private, max-age=600")
        .body(
            ApiResponse.<AdminRevenueByMonthResponse>builder()
                .result(adminDashboardService.getRevenueByMonth(targetYear))
                .build());
  }

  @GetMapping("/dashboard/quick-stats")
  @Operation(
      summary = "Get quick statistics",
      description = "Returns conversion rate, active users, total documents created, and satisfaction rate (placeholder -1 until feedback feature is built).")
  public ApiResponse<AdminQuickStatsResponse> getQuickStats() {
    return ApiResponse.<AdminQuickStatsResponse>builder()
        .result(adminDashboardService.getQuickStats())
        .build();
  }

  @GetMapping("/system/status")
  @Operation(
      summary = "Get system service status",
      description = "Returns real-time status of Web Server, Database, AI Service, and Storage.")
  public ResponseEntity<ApiResponse<AdminSystemStatusResponse>> getSystemStatus() {
    return ResponseEntity.ok()
        .header("Cache-Control", "private, max-age=15")
        .body(
            ApiResponse.<AdminSystemStatusResponse>builder()
                .result(adminDashboardService.getSystemStatus())
                .build());
  }

  @GetMapping("/transactions")
  @Operation(
      summary = "Get all transactions (admin)",
      description = "Returns paginated list of all wallet top-up transactions with optional status filter and full-text search.")
  public ApiResponse<AdminTransactionPageResponse> getAllTransactions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String order,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String search) {

    Sort.Direction direction = order.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    List<TransactionStatus> statuses = resolveStatuses(status);
    return ApiResponse.<AdminTransactionPageResponse>builder()
        .result(adminDashboardService.getAllTransactions(statuses, search, pageable))
        .build();
  }

  @GetMapping("/transactions/stats")
  @Operation(
      summary = "Get transaction statistics",
      description = "Returns aggregate counts by status and total revenue from completed transactions.")
  public ApiResponse<AdminTransactionStatsResponse> getTransactionStats() {
    return ApiResponse.<AdminTransactionStatsResponse>builder()
        .result(adminDashboardService.getTransactionStats())
        .build();
  }

  @GetMapping("/transactions/{transactionId}")
  @Operation(
      summary = "Get transaction detail",
      description = "Returns full detail of a single transaction including user info and PayOS order code.")
  public ApiResponse<AdminTransactionResponse> getTransactionById(
      @PathVariable UUID transactionId) {
    return ApiResponse.<AdminTransactionResponse>builder()
        .result(adminDashboardService.getTransactionById(transactionId))
        .build();
  }

  @GetMapping(value = "/transactions/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation(
      summary = "Export transactions to CSV",
      description = "Downloads a CSV file of all matching transactions. Accepts same status and search filters as the list endpoint.")
  public void exportTransactions(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String search,
      HttpServletResponse response) {
    List<TransactionStatus> statuses = resolveStatuses(status);
    adminDashboardService.exportTransactionsCsv(statuses, search, response);
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  /**
   * Maps FE status string to TransactionStatus enum list.
   * null / blank → all statuses
   * "completed"  → [SUCCESS]
   * "pending"    → [PENDING, PROCESSING]
   * "failed"     → [FAILED, CANCELLED]
   */
  private List<TransactionStatus> resolveStatuses(String status) {
    if (status == null || status.isBlank()) {
      return List.of(TransactionStatus.values());
    }
    return switch (status.toLowerCase()) {
      case "completed" -> List.of(TransactionStatus.SUCCESS);
      case "pending" -> List.of(TransactionStatus.PENDING, TransactionStatus.PROCESSING);
      case "failed" -> List.of(TransactionStatus.FAILED, TransactionStatus.CANCELLED);
      default -> List.of(TransactionStatus.values());
    };
  }
}
