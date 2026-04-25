package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.response.AdminDashboardStatsResponse;
import com.fptu.math_master.dto.response.AdminQuickStatsResponse;
import com.fptu.math_master.dto.response.AdminRevenueByMonthResponse;
import com.fptu.math_master.dto.response.AdminSystemStatusResponse;
import com.fptu.math_master.dto.response.AdminTransactionPageResponse;
import com.fptu.math_master.dto.response.AdminTransactionStatsResponse;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonPlanRepository;
import com.fptu.math_master.repository.MindmapRepository;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("AdminDashboardServiceImpl")
class AdminDashboardServiceImplTest extends BaseUnitTest {

  @InjectMocks private AdminDashboardServiceImpl adminDashboardService;

  @Mock private UserRepository userRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private LessonPlanRepository lessonPlanRepository;
  @Mock private MindmapRepository mindmapRepository;

  private User buildAdminUser(UUID userId, String fullName, String email) {
    User user = User.builder().fullName(fullName).email(email).build();
    user.setId(userId);
    return user;
  }

  private Transaction buildTransaction(
      UUID transactionId,
      Wallet wallet,
      String description,
      BigDecimal amount,
      TransactionStatus status,
      Long orderCode,
      Instant createdAt) {
    Transaction tx =
        Transaction.builder()
            .wallet(wallet)
            .description(description)
            .amount(amount)
            .type(TransactionType.PAYMENT)
            .status(status)
            .orderCode(orderCode)
            .build();
    tx.setId(transactionId);
    tx.setCreatedAt(createdAt);
    return tx;
  }

  @Test
  @DisplayName("getDashboardStats: map metrics và tính growth khi month hợp lệ")
  void it_should_map_stats_and_calculate_growth_when_month_is_valid() {
    // ===== ARRANGE =====
    when(userRepository.count()).thenReturn(100L);
    when(userRepository.countByCreatedAtBetween(any(), any())).thenReturn(10L, 20L);
    when(transactionRepository.sumSuccessfulRevenue(any(), any()))
        .thenReturn(new BigDecimal("1000"), new BigDecimal("1500"));
    when(enrollmentRepository.countByStatusAndDeletedAtIsNull(EnrollmentStatus.ACTIVE)).thenReturn(30L);
    when(enrollmentRepository.countByStatusAndDeletedAtIsNullAndCreatedAtBetween(
            eq(EnrollmentStatus.ACTIVE), any(), any()))
        .thenReturn(5L, 8L);
    when(transactionRepository.count()).thenReturn(200L);
    when(transactionRepository.countByCreatedAtBetween(any(), any())).thenReturn(12L, 18L);

    // ===== ACT =====
    AdminDashboardStatsResponse result = adminDashboardService.getDashboardStats("2026-03");

    // ===== ASSERT =====
    assertAll(
        () -> assertEquals(100L, result.getTotalUsers()),
        () -> assertEquals(-50.0, result.getTotalUsersGrowthPercent()),
        () -> assertEquals(1000L, result.getMonthlyRevenue()),
        () -> assertEquals(-33.3, result.getMonthlyRevenueGrowthPercent()),
        () -> assertEquals(30L, result.getActiveEnrollments()),
        () -> assertEquals(60.0, result.getActiveEnrollmentsGrowthPercent()),
        () -> assertEquals(200L, result.getTotalTransactions()),
        () -> assertEquals(-33.3, result.getTotalTransactionsGrowthPercent()),
        () -> assertEquals("2026-03", result.getMonth()));

    // ===== VERIFY =====
    verify(userRepository, times(1)).count();
    verify(userRepository, times(2)).countByCreatedAtBetween(any(), any());
    verify(transactionRepository, times(2)).sumSuccessfulRevenue(any(), any());
    verify(enrollmentRepository, times(1)).countByStatusAndDeletedAtIsNull(EnrollmentStatus.ACTIVE);
    verify(enrollmentRepository, times(2))
        .countByStatusAndDeletedAtIsNullAndCreatedAtBetween(eq(EnrollmentStatus.ACTIVE), any(), any());
    verify(transactionRepository, times(1)).count();
    verify(transactionRepository, times(2)).countByCreatedAtBetween(any(), any());
    verify(lessonPlanRepository, never()).countAllNotDeleted();
    verify(mindmapRepository, never()).countAllNotDeleted();
    verifyNoMoreInteractions(
        userRepository, transactionRepository, enrollmentRepository, lessonPlanRepository, mindmapRepository);
  }

  @Test
  @DisplayName("getRevenueByMonth: đủ 12 tháng, tháng không có dữ liệu = 0")
  void it_should_fill_twelve_months_with_zero_when_repository_returns_sparse_rows() {
    // ===== ARRANGE =====
    when(transactionRepository.sumRevenueByMonth(2026))
        .thenReturn(List.of(new Object[] {1, 1200L}, new Object[] {3, 3450L}));

    // ===== ACT =====
    AdminRevenueByMonthResponse result = adminDashboardService.getRevenueByMonth(2026);

    // ===== ASSERT =====
    assertAll(
        () -> assertEquals(2026, result.getYear()),
        () -> assertEquals(12, result.getMonthly().size()),
        () -> assertEquals(1200L, result.getMonthly().get(0).getRevenue()),
        () -> assertEquals(0L, result.getMonthly().get(1).getRevenue()),
        () -> assertEquals(3450L, result.getMonthly().get(2).getRevenue()),
        () -> assertEquals(0L, result.getMonthly().get(11).getRevenue()));

    // ===== VERIFY =====
    verify(transactionRepository, times(1)).sumRevenueByMonth(2026);
    verifyNoMoreInteractions(transactionRepository);
    verifyNoMoreInteractions(userRepository, enrollmentRepository, lessonPlanRepository, mindmapRepository);
  }

  @Test
  @DisplayName("getQuickStats: conversion và documentsCreated")
  void it_should_compute_conversion_and_documents_when_repositories_return_counts() {
    // ===== ARRANGE =====
    when(userRepository.count()).thenReturn(50L);
    when(userRepository.countByStatus(Status.ACTIVE)).thenReturn(20L);
    when(transactionRepository.countByCreatedAtBetween(eq(Instant.EPOCH), any())).thenReturn(70L);
    when(lessonPlanRepository.countAllNotDeleted()).thenReturn(11L);
    when(mindmapRepository.countAllNotDeleted()).thenReturn(9L);

    // ===== ACT =====
    AdminQuickStatsResponse result = adminDashboardService.getQuickStats();

    // ===== ASSERT =====
    assertAll(
        () -> assertEquals(100.0, result.getConversionRate()),
        () -> assertEquals(20L, result.getActiveUsers()),
        () -> assertEquals(20L, result.getDocumentsCreated()),
        () -> assertEquals(-1.0, result.getSatisfactionRate()));

    // ===== VERIFY =====
    verify(userRepository, times(1)).count();
    verify(userRepository, times(1)).countByStatus(Status.ACTIVE);
    verify(transactionRepository, times(1)).countByCreatedAtBetween(eq(Instant.EPOCH), any());
    verify(lessonPlanRepository, times(1)).countAllNotDeleted();
    verify(mindmapRepository, times(1)).countAllNotDeleted();
    verifyNoMoreInteractions(
        userRepository, transactionRepository, enrollmentRepository, lessonPlanRepository, mindmapRepository);
  }

  /**
   * Nhánh catch trong {@link AdminDashboardServiceImpl#getSystemStatus}: repository ném lỗi → status
   * "error" và log ERROR — không phải DB thật down; mock cố tình throw để cover nhánh bất thường.
   */
  @Test
  @DisplayName("getSystemStatus: Database = error khi health check (count) throw")
  void it_should_mark_database_error_when_user_count_throws() {
    // ===== ARRANGE =====
    when(userRepository.count()).thenThrow(new RuntimeException("simulated-db-failure"));

    // ===== ACT =====
    AdminSystemStatusResponse result = adminDashboardService.getSystemStatus();

    // ===== ASSERT =====
    assertAll(
        () -> assertEquals(4, result.getServices().size()),
        () -> assertEquals("Database", result.getServices().get(1).getName()),
        () -> assertEquals("error", result.getServices().get(1).getStatus()));

    // ===== VERIFY =====
    verify(userRepository, times(1)).count();
    verifyNoMoreInteractions(userRepository);
    verifyNoMoreInteractions(transactionRepository, enrollmentRepository, lessonPlanRepository, mindmapRepository);
  }

  @Test
  @DisplayName("getAllTransactions: trim search và map DTO")
  void it_should_trim_search_and_map_items_when_page_requested() {
    // ===== ARRANGE =====
    Pageable pageable = PageRequest.of(0, 10);
    UUID txId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = buildAdminUser(userId, "Nguyễn Văn Admin", "admin.dashboard@fptu.edu.vn");
    Wallet wallet = Wallet.builder().user(user).build();
    Transaction tx =
        buildTransaction(
            txId,
            wallet,
            "Gói Premium Toán 12",
            new BigDecimal("299000"),
            TransactionStatus.SUCCESS,
            9001L,
            Instant.parse("2026-03-10T10:15:30Z"));
    Page<Transaction> page = new PageImpl<>(List.of(tx), pageable, 1);
    when(transactionRepository.findAllWithUserFiltered(any(), eq("search-key"), eq(pageable)))
        .thenReturn(page);

    // ===== ACT =====
    AdminTransactionPageResponse result =
        adminDashboardService.getAllTransactions(
            List.of(TransactionStatus.SUCCESS), "  search-key  ", pageable);

    // ===== ASSERT =====
    assertAll(
        () -> assertEquals(1, result.getItems().size()),
        () -> assertEquals(1L, result.getTotalItems()),
        () -> assertEquals(1, result.getTotalPages()),
        () -> assertEquals("Nguyễn Văn Admin", result.getItems().getFirst().getUserName()),
        () -> assertEquals("completed", result.getItems().getFirst().getStatus()));

    // ===== VERIFY =====
    verify(transactionRepository, times(1))
        .findAllWithUserFiltered(any(), eq("search-key"), eq(pageable));
    verifyNoMoreInteractions(transactionRepository);
    verifyNoMoreInteractions(userRepository, enrollmentRepository, lessonPlanRepository, mindmapRepository);
  }

  @Test
  @DisplayName("getTransactionStats: aggregate theo status và revenue")
  void it_should_aggregate_counts_and_revenue_when_repository_returns_values() {
    // ===== ARRANGE =====
    when(transactionRepository.count()).thenReturn(99L);
    when(transactionRepository.countByStatusIn(List.of(TransactionStatus.SUCCESS))).thenReturn(70L);
    when(transactionRepository.countByStatusIn(List.of(TransactionStatus.PENDING, TransactionStatus.PROCESSING)))
        .thenReturn(20L);
    when(transactionRepository.countByStatusIn(List.of(TransactionStatus.FAILED, TransactionStatus.CANCELLED)))
        .thenReturn(9L);
    when(transactionRepository.sumAllSuccessfulRevenue()).thenReturn(new BigDecimal("1000000"));

    // ===== ACT =====
    AdminTransactionStatsResponse result = adminDashboardService.getTransactionStats();

    // ===== ASSERT =====
    assertAll(
        () -> assertEquals(99L, result.getTotal()),
        () -> assertEquals(70L, result.getCompleted()),
        () -> assertEquals(20L, result.getPending()),
        () -> assertEquals(9L, result.getFailed()),
        () -> assertEquals(1000000L, result.getTotalRevenue()));

    // ===== VERIFY =====
    verify(transactionRepository, times(1)).count();
    verify(transactionRepository, times(1)).countByStatusIn(List.of(TransactionStatus.SUCCESS));
    verify(transactionRepository, times(1))
        .countByStatusIn(List.of(TransactionStatus.PENDING, TransactionStatus.PROCESSING));
    verify(transactionRepository, times(1))
        .countByStatusIn(List.of(TransactionStatus.FAILED, TransactionStatus.CANCELLED));
    verify(transactionRepository, times(1)).sumAllSuccessfulRevenue();
    verifyNoMoreInteractions(transactionRepository);
    verifyNoMoreInteractions(userRepository, enrollmentRepository, lessonPlanRepository, mindmapRepository);
  }

  @Test
  @DisplayName("getTransactionById: TRANSACTION_NOT_FOUND khi không có bản ghi")
  void it_should_throw_when_transaction_not_found() {
    // ===== ARRANGE =====
    UUID id = UUID.randomUUID();
    when(transactionRepository.findByIdWithUser(id)).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException ex =
        assertThrows(AppException.class, () -> adminDashboardService.getTransactionById(id));
    assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, ex.getErrorCode());

    // ===== VERIFY =====
    verify(transactionRepository, times(1)).findByIdWithUser(id);
    verifyNoMoreInteractions(transactionRepository);
    verifyNoMoreInteractions(userRepository, enrollmentRepository, lessonPlanRepository, mindmapRepository);
  }

  @Test
  @DisplayName("exportTransactionsCsv: header, trim search, escape CSV")
  void it_should_write_csv_with_escaping_when_transactions_exist() throws Exception {
    // ===== ARRANGE =====
    HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
    StringWriter out = new StringWriter();
    PrintWriter writer = new PrintWriter(out);
    when(response.getWriter()).thenReturn(writer);

    User user =
        buildAdminUser(
            UUID.randomUUID(), "Phạm, \"Minh\" An", "pham.minh.an@student.fptu.edu.vn");
    Wallet wallet = Wallet.builder().user(user).build();
    Transaction tx =
        buildTransaction(
            UUID.randomUUID(),
            wallet,
            "Gói Pro, năm học",
            new BigDecimal("12345"),
            TransactionStatus.FAILED,
            123L,
            Instant.parse("2026-03-10T10:15:30Z"));
    Page<Transaction> page = new PageImpl<>(List.of(tx), Pageable.unpaged(), 1);
    when(transactionRepository.findAllWithUserFiltered(
            List.of(TransactionStatus.FAILED), "mail", Pageable.unpaged()))
        .thenReturn(page);

    // ===== ACT =====
    adminDashboardService.exportTransactionsCsv(List.of(TransactionStatus.FAILED), "  mail  ", response);
    writer.flush();

    // ===== ASSERT =====
    String csv = out.toString();
    assertAll(
        () ->
            assertTrue(
                csv.contains(
                    "id,userId,userName,userEmail,planName,amount,status,paymentMethod,orderCode,createdAt")),
        () -> assertTrue(csv.contains("\"Phạm, \"\"Minh\"\" An\"")),
        () -> assertTrue(csv.contains("\"Gói Pro, năm học\"")),
        () -> assertTrue(csv.contains(",failed,")));

    // ===== VERIFY =====
    verify(response).setContentType("text/csv; charset=UTF-8");
    verify(transactionRepository, times(1))
        .findAllWithUserFiltered(List.of(TransactionStatus.FAILED), "mail", Pageable.unpaged());
    verifyNoMoreInteractions(transactionRepository);
    verifyNoMoreInteractions(userRepository, enrollmentRepository, lessonPlanRepository, mindmapRepository);
  }
}
