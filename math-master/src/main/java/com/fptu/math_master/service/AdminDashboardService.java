package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.AdminDashboardStatsResponse;
import com.fptu.math_master.dto.response.AdminQuickStatsResponse;
import com.fptu.math_master.dto.response.AdminRevenueByMonthResponse;
import com.fptu.math_master.dto.response.AdminSystemStatusResponse;
import com.fptu.math_master.dto.response.AdminTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminDashboardService {

  AdminDashboardStatsResponse getDashboardStats(String month);

  AdminRevenueByMonthResponse getRevenueByMonth(int year);

  AdminQuickStatsResponse getQuickStats();

  AdminSystemStatusResponse getSystemStatus();

  Page<AdminTransactionResponse> getAllTransactions(Pageable pageable);
}
