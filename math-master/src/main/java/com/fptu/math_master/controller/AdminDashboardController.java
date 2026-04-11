package com.fptu.math_master.controller;

import java.time.Year;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.response.AdminDashboardStatsResponse;
import com.fptu.math_master.dto.response.AdminQuickStatsResponse;
import com.fptu.math_master.dto.response.AdminRevenueByMonthResponse;
import com.fptu.math_master.dto.response.AdminSystemStatusResponse;
import com.fptu.math_master.dto.response.AdminTransactionResponse;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.service.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
  public ApiResponse<AdminDashboardStatsResponse> getDashboardStats(
      @RequestParam(required = false) String month) {
    return ApiResponse.<AdminDashboardStatsResponse>builder()
        .result(adminDashboardService.getDashboardStats(month))
        .build();
  }

  @GetMapping("/dashboard/revenue-by-month")
  @Operation(
      summary = "Get monthly revenue for a year",
      description = "Returns 12 monthly revenue totals (VNĐ) for the specified year.")
  public ApiResponse<AdminRevenueByMonthResponse> getRevenueByMonth(
      @RequestParam(required = false) Integer year) {
    int targetYear = (year != null) ? year : Year.now().getValue();
    return ApiResponse.<AdminRevenueByMonthResponse>builder()
        .result(adminDashboardService.getRevenueByMonth(targetYear))
        .build();
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
  public ApiResponse<AdminSystemStatusResponse> getSystemStatus() {
    return ApiResponse.<AdminSystemStatusResponse>builder()
        .result(adminDashboardService.getSystemStatus())
        .build();
  }

  @GetMapping("/transactions")
  @Operation(
      summary = "Get all transactions (admin)",
      description = "Returns paginated list of all wallet top-up transactions across all users.")
  public ApiResponse<Page<AdminTransactionResponse>> getAllTransactions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String order) {
    Sort.Direction direction = order.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    return ApiResponse.<Page<AdminTransactionResponse>>builder()
        .result(adminDashboardService.getAllTransactions(pageable))
        .build();
  }
}
