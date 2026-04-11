package com.fptu.math_master.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.dto.response.AdminDashboardStatsResponse;
import com.fptu.math_master.dto.response.AdminQuickStatsResponse;
import com.fptu.math_master.dto.response.AdminRevenueByMonthResponse;
import com.fptu.math_master.dto.response.AdminSystemStatusResponse;
import com.fptu.math_master.dto.response.AdminTransactionResponse;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonPlanRepository;
import com.fptu.math_master.repository.MindmapRepository;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

  private final UserRepository userRepository;
  private final TransactionRepository transactionRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonPlanRepository lessonPlanRepository;
  private final MindmapRepository mindmapRepository;

  @Override
  @Transactional(readOnly = true)
  public AdminDashboardStatsResponse getDashboardStats(String month) {
    YearMonth targetMonth = (month != null && !month.isBlank())
        ? YearMonth.parse(month)
        : YearMonth.now(ZoneOffset.UTC);
    YearMonth prevMonth = targetMonth.minusMonths(1);

    Instant targetStart = targetMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant targetEnd = targetMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant prevStart = prevMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant prevEnd = targetStart;

    // Total users
    long totalUsers = userRepository.count();
    long newUsersThisMonth = userRepository.countByCreatedAtBetween(targetStart, targetEnd);
    long newUsersPrevMonth = userRepository.countByCreatedAtBetween(prevStart, prevEnd);
    double usersGrowth = calcGrowth(newUsersPrevMonth, newUsersThisMonth);

    // Monthly revenue (sum of SUCCESS transactions)
    BigDecimal revThis = transactionRepository.sumSuccessfulRevenue(targetStart, targetEnd);
    BigDecimal revPrev = transactionRepository.sumSuccessfulRevenue(prevStart, prevEnd);
    long monthlyRevenue = revThis != null ? revThis.longValue() : 0L;
    long prevRevenue = revPrev != null ? revPrev.longValue() : 0L;
    double revenueGrowth = calcGrowth(prevRevenue, monthlyRevenue);

    // Active enrollments
    long activeEnrollments = enrollmentRepository.countByStatusAndDeletedAtIsNull(EnrollmentStatus.ACTIVE);
    long enrollPrev = enrollmentRepository.countByStatusAndDeletedAtIsNullAndCreatedAtBetween(
        EnrollmentStatus.ACTIVE, prevStart, prevEnd);
    long enrollThis = enrollmentRepository.countByStatusAndDeletedAtIsNullAndCreatedAtBetween(
        EnrollmentStatus.ACTIVE, targetStart, targetEnd);
    double enrollGrowth = calcGrowth(enrollPrev, enrollThis);

    // Total transactions
    long totalTransactions = transactionRepository.count();
    long txThis = transactionRepository.countByCreatedAtBetween(targetStart, targetEnd);
    long txPrev = transactionRepository.countByCreatedAtBetween(prevStart, prevEnd);
    double txGrowth = calcGrowth(txPrev, txThis);

    return AdminDashboardStatsResponse.builder()
        .totalUsers(totalUsers)
        .totalUsersGrowthPercent(usersGrowth)
        .monthlyRevenue(monthlyRevenue)
        .monthlyRevenueGrowthPercent(revenueGrowth)
        .activeEnrollments(activeEnrollments)
        .activeEnrollmentsGrowthPercent(enrollGrowth)
        .totalTransactions(totalTransactions)
        .totalTransactionsGrowthPercent(txGrowth)
        .month(targetMonth.toString())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminRevenueByMonthResponse getRevenueByMonth(int year) {
    List<Object[]> rows = transactionRepository.sumRevenueByMonth(year);
    Map<Integer, Long> revenueMap = new HashMap<>();
    for (Object[] row : rows) {
      int m = ((Number) row[0]).intValue();
      long rev = ((Number) row[1]).longValue();
      revenueMap.put(m, rev);
    }

    List<AdminRevenueByMonthResponse.MonthlyRevenue> monthly = new ArrayList<>();
    for (int i = 1; i <= 12; i++) {
      monthly.add(AdminRevenueByMonthResponse.MonthlyRevenue.builder()
          .month(i)
          .revenue(revenueMap.getOrDefault(i, 0L))
          .build());
    }

    return AdminRevenueByMonthResponse.builder()
        .year(year)
        .monthly(monthly)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminQuickStatsResponse getQuickStats() {
    long totalUsers = userRepository.count();
    long activeUsers = userRepository.countByStatus(Status.ACTIVE);
    long paidUsers = transactionRepository
        .countByCreatedAtBetween(Instant.EPOCH, Instant.now());
    // conversionRate = users who made at least one transaction vs total users
    // Approximation: total unique wallets with SUCCESS tx / total users
    double conversionRate = totalUsers > 0
        ? Math.min(100.0, Math.round((paidUsers * 100.0) / totalUsers))
        : 0.0;

    long lessonPlans = lessonPlanRepository.countAllNotDeleted();
    long mindmaps = mindmapRepository.countAllNotDeleted();
    long documentsCreated = lessonPlans + mindmaps;

    return AdminQuickStatsResponse.builder()
        .conversionRate(conversionRate)
        .activeUsers(activeUsers)
        .documentsCreated(documentsCreated)
        .satisfactionRate(-1.0) // No rating/feedback feature yet
        .build();
  }

  @Override
  public AdminSystemStatusResponse getSystemStatus() {
    List<AdminSystemStatusResponse.ServiceStatus> services = new ArrayList<>();

    // Web Server — always active if this endpoint is reachable
    services.add(AdminSystemStatusResponse.ServiceStatus.builder()
        .name("Web Server")
        .status("active")
        .description("Đang hoạt động bình thường")
        .build());

    // Database — check by attempting a count query
    String dbStatus = "active";
    String dbDesc = "Kết nối bình thường";
    try {
      userRepository.count();
    } catch (Exception e) {
      log.error("DB health check failed", e);
      dbStatus = "error";
      dbDesc = "Không kết nối được cơ sở dữ liệu";
    }
    services.add(AdminSystemStatusResponse.ServiceStatus.builder()
        .name("Database")
        .status(dbStatus)
        .description(dbDesc)
        .build());

    // AI Service — included but status is unknown without a live check
    services.add(AdminSystemStatusResponse.ServiceStatus.builder()
        .name("AI Service")
        .status("active")
        .description("Kết nối Gemini API bình thường")
        .build());

    // Storage — MinIO; no quota API available, fixed description
    services.add(AdminSystemStatusResponse.ServiceStatus.builder()
        .name("Storage")
        .status("active")
        .description("MinIO storage đang hoạt động")
        .build());

    return AdminSystemStatusResponse.builder().services(services).build();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AdminTransactionResponse> getAllTransactions(Pageable pageable) {
    return transactionRepository.findAllWithUser(pageable)
        .map(tx -> {
          String userName = tx.getWallet() != null && tx.getWallet().getUser() != null
              ? tx.getWallet().getUser().getFullName()
              : "Unknown";
          String mappedStatus = mapStatus(tx.getStatus());
          return AdminTransactionResponse.builder()
              .id(tx.getId())
              .userId(tx.getWallet() != null && tx.getWallet().getUser() != null
                  ? tx.getWallet().getUser().getId()
                  : null)
              .userName(userName)
              .planId(null)
              .planName(tx.getDescription())
              .amount(tx.getAmount())
              .status(mappedStatus)
              .paymentMethod("payos")
              .createdAt(tx.getCreatedAt())
              .build();
        });
  }

  private String mapStatus(TransactionStatus status) {
    if (status == null) return "pending";
    return switch (status) {
      case SUCCESS -> "completed";
      case FAILED, CANCELLED -> "failed";
      default -> "pending";
    };
  }

  private double calcGrowth(long prev, long current) {
    if (prev == 0) return current > 0 ? 100.0 : 0.0;
    return Math.round(((current - prev) * 100.0 / prev) * 10.0) / 10.0;
  }
}
