package com.fptu.math_master.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.fptu.math_master.dto.response.AdminDashboardStatsResponse;
import com.fptu.math_master.dto.response.AdminQuickStatsResponse;
import com.fptu.math_master.dto.response.AdminRevenueByMonthResponse;
import com.fptu.math_master.dto.response.AdminSystemStatusResponse;
import com.fptu.math_master.dto.response.AdminTransactionPageResponse;
import com.fptu.math_master.dto.response.AdminTransactionResponse;
import com.fptu.math_master.dto.response.AdminTransactionStatsResponse;
import com.fptu.math_master.enums.TransactionStatus;

import jakarta.servlet.http.HttpServletResponse;

public interface AdminDashboardService {

  AdminDashboardStatsResponse getDashboardStats(String month);

  AdminRevenueByMonthResponse getRevenueByMonth(int year);

  AdminQuickStatsResponse getQuickStats();

  AdminSystemStatusResponse getSystemStatus();

  /** Paginated + filtered transaction list for admin */
  AdminTransactionPageResponse getAllTransactions(
      List<TransactionStatus> statuses, String search, Pageable pageable);

  /** Aggregate counts and total revenue for admin transaction stats card */
  AdminTransactionStatsResponse getTransactionStats();

  /** Detail for a single transaction (for admin modal) */
  AdminTransactionResponse getTransactionById(UUID transactionId);

  /** Stream all matching transactions as CSV to the HTTP response */
  void exportTransactionsCsv(List<TransactionStatus> statuses, String search,
      HttpServletResponse response);
}
