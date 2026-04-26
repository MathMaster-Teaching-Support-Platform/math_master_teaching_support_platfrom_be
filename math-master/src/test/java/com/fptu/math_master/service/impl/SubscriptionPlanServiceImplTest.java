package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.fptu.math_master.dto.request.CreateSubscriptionPlanRequest;
import com.fptu.math_master.dto.request.UpdateSubscriptionPlanRequest;
import com.fptu.math_master.dto.response.SubscriptionOverallStatsResponse;
import com.fptu.math_master.dto.response.SubscriptionPlanResponse;
import com.fptu.math_master.dto.response.UserSubscriptionResponse;
import com.fptu.math_master.entity.SubscriptionPlan;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.UserSubscription;
import com.fptu.math_master.enums.BillingCycle;
import com.fptu.math_master.enums.SubscriptionPlanStatus;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.SubscriptionPlanRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.repository.UserSubscriptionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("SubscriptionPlanServiceImpl - Tests")
class SubscriptionPlanServiceImplTest extends BaseUnitTest {

  @InjectMocks private SubscriptionPlanServiceImpl subscriptionPlanService;

  @Mock private SubscriptionPlanRepository planRepository;
  @Mock private UserSubscriptionRepository subscriptionRepository;
  @Mock private UserRepository userRepository;

  private UUID planId;
  private SubscriptionPlan basePlan;

  @BeforeEach
  void setUp() {
    planId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    basePlan = buildPlan(planId, "Gói Giáo viên Pro", "goi-giao-vien-pro", true, true);
  }

  private SubscriptionPlan buildPlan(
      UUID id, String name, String slug, boolean featured, boolean publicVisible) {
    SubscriptionPlan plan =
        SubscriptionPlan.builder()
            .name(name)
            .slug(slug)
            .price(new BigDecimal("299000"))
            .billingCycle(BillingCycle.MONTH)
            .description("Phù hợp cho giảng viên luyện đề và phân tích tiến độ")
            .featured(featured)
            .publicVisible(publicVisible)
            .status(SubscriptionPlanStatus.ACTIVE)
            .features(List.of("Roadmap AI", "Unlimited quiz exports"))
            .tokenQuota(120000)
            .build();
    plan.setId(id);
    plan.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
    plan.setUpdatedAt(Instant.parse("2026-04-01T00:00:00Z"));
    return plan;
  }

  private UserSubscription buildSubscription(SubscriptionPlan plan, UUID userId, String fullName) {
    UserSubscription sub =
        UserSubscription.builder()
            .userId(userId)
            .plan(plan)
            .startDate(Instant.parse("2026-04-01T00:00:00Z"))
            .endDate(Instant.parse("2026-05-01T00:00:00Z"))
            .amount(new BigDecimal("299000"))
            .currency("VND")
            .status(UserSubscriptionStatus.ACTIVE)
            .paymentMethod("payos")
            .tokenQuota(120000)
            .tokenRemaining(95000)
            .build();
    sub.setId(UUID.fromString("aaaa1111-bbbb-2222-cccc-333333333333"));
    sub.setCreatedAt(Instant.parse("2026-04-01T00:00:00Z"));
    User user = User.builder().fullName(fullName).email(fullName.toLowerCase().replace(" ", ".") + "@fptu.edu.vn").build();
    user.setId(userId);
    return sub;
  }

  @Nested
  @DisplayName("getPublicPurchasablePlans()")
  class GetPublicPurchasablePlansTests {

    @Test
    void it_should_return_public_plans_with_default_stats_when_plans_exist() {
      // ===== ARRANGE =====
      when(planRepository.findPublicPurchasablePlans()).thenReturn(List.of(basePlan));

      // ===== ACT =====
      List<SubscriptionPlanResponse> result = subscriptionPlanService.getPublicPurchasablePlans();

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals(planId, result.getFirst().getId()),
          () -> assertEquals(0L, result.getFirst().getStats().getActiveUsers()),
          () -> assertEquals(0L, result.getFirst().getStats().getRevenueThisMonth()),
          () -> assertEquals(0.0, result.getFirst().getStats().getGrowthPercent()));

      // ===== VERIFY =====
      verify(planRepository, times(1)).findPublicPurchasablePlans();
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("getAllPlans()")
  class GetAllPlansTests {

    @Test
    void it_should_map_revenue_and_growth_when_previous_revenue_is_non_zero() {
      // ===== ARRANGE =====
      when(planRepository.findAllActive()).thenReturn(List.of(basePlan));
      when(subscriptionRepository.countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE))
          .thenReturn(9L);
      when(subscriptionRepository.sumRevenueByPlan(eq(planId), any(Instant.class), any(Instant.class)))
          .thenReturn(new BigDecimal("100000"), new BigDecimal("50000"));

      // ===== ACT =====
      List<SubscriptionPlanResponse> result = subscriptionPlanService.getAllPlans();

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals(9L, result.getFirst().getStats().getActiveUsers()),
          () -> assertEquals(100000L, result.getFirst().getStats().getRevenueThisMonth()),
          () -> assertEquals(100.0, result.getFirst().getStats().getGrowthPercent()));

      // ===== VERIFY =====
      verify(planRepository, times(1)).findAllActive();
      verify(subscriptionRepository, times(1))
          .countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE);
      verify(subscriptionRepository, times(2))
          .sumRevenueByPlan(eq(planId), any(Instant.class), any(Instant.class));
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }

    @Test
    void it_should_return_zero_revenue_and_zero_growth_when_revenue_is_null() {
      // ===== ARRANGE =====
      when(planRepository.findAllActive()).thenReturn(List.of(basePlan));
      when(subscriptionRepository.countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE))
          .thenReturn(0L);
      when(subscriptionRepository.sumRevenueByPlan(eq(planId), any(Instant.class), any(Instant.class)))
          .thenReturn((BigDecimal) null, (BigDecimal) null);

      // ===== ACT =====
      List<SubscriptionPlanResponse> result = subscriptionPlanService.getAllPlans();

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(0L, result.getFirst().getStats().getRevenueThisMonth()),
          () -> assertEquals(0.0, result.getFirst().getStats().getGrowthPercent()));

      // ===== VERIFY =====
      verify(planRepository, times(1)).findAllActive();
      verify(subscriptionRepository, times(1))
          .countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE);
      verify(subscriptionRepository, times(2))
          .sumRevenueByPlan(eq(planId), any(Instant.class), any(Instant.class));
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("createPlan()")
  class CreatePlanTests {

    @Test
    void it_should_create_plan_when_slug_is_unique_and_token_quota_is_null() {
      // ===== ARRANGE =====
      CreateSubscriptionPlanRequest request =
          CreateSubscriptionPlanRequest.builder()
              .name("Gói Luyện Thi Cấp Tốc")
              .description("Dành cho học sinh lớp 12 chuẩn bị kỳ thi THPT")
              .price(new BigDecimal("199000"))
              .billingCycle(BillingCycle.MONTH)
              .features(List.of("500 đề mẫu", "Gợi ý lời giải AI"))
              .tokenQuota(null)
              .featured(true)
              .isPublic(true)
              .build();
      SubscriptionPlan saved = buildPlan(planId, request.getName(), "goi-luyen-thi-cap-toc", true, true);
      saved.setTokenQuota(0);
      when(planRepository.existsBySlug("goi-luyen-thi-cap-toc")).thenReturn(false);
      when(planRepository.save(any(SubscriptionPlan.class))).thenReturn(saved);

      // ===== ACT =====
      SubscriptionPlanResponse result = subscriptionPlanService.createPlan(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(planId, result.getId()),
          () -> assertEquals("goi-luyen-thi-cap-toc", result.getSlug()),
          () -> assertEquals(0, result.getTokenQuota()));

      // ===== VERIFY =====
      verify(planRepository, times(1)).existsBySlug("goi-luyen-thi-cap-toc");
      verify(planRepository, times(1)).save(any(SubscriptionPlan.class));
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }

    @Test
    void it_should_throw_exception_when_slug_already_exists() {
      // ===== ARRANGE =====
      CreateSubscriptionPlanRequest request =
          CreateSubscriptionPlanRequest.builder()
              .name("Gói Luyện Thi Cấp Tốc")
              .billingCycle(BillingCycle.MONTH)
              .features(List.of("Gợi ý lời giải AI"))
              .build();
      when(planRepository.existsBySlug("goi-luyen-thi-cap-toc")).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> subscriptionPlanService.createPlan(request));
      assertEquals(ErrorCode.SUBSCRIPTION_PLAN_SLUG_EXISTS, ex.getErrorCode());

      // ===== VERIFY =====
      verify(planRepository, times(1)).existsBySlug("goi-luyen-thi-cap-toc");
      verify(planRepository, never()).save(any());
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("updatePlan()")
  class UpdatePlanTests {

    @Test
    void it_should_update_all_mutable_fields_when_request_contains_values() {
      // ===== ARRANGE =====
      UpdateSubscriptionPlanRequest request =
          UpdateSubscriptionPlanRequest.builder()
              .name("Gói Giáo viên Chuyên sâu")
              .description("Thêm dữ liệu phân tích lớp học")
              .price(new BigDecimal("399000"))
              .billingCycle(BillingCycle.YEAR)
              .features(List.of("Class analytics", "AI tutor"))
              .tokenQuota(300000)
              .featured(false)
              .isPublic(false)
              .status(SubscriptionPlanStatus.INACTIVE)
              .build();
      when(planRepository.findById(planId)).thenReturn(Optional.of(basePlan));
      when(planRepository.existsBySlugAndIdNot("goi-giao-vien-chuyen-sau", planId)).thenReturn(false);
      when(planRepository.save(basePlan)).thenReturn(basePlan);
      when(subscriptionRepository.countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE))
          .thenReturn(4L);
      when(subscriptionRepository.sumRevenueByPlan(eq(planId), any(Instant.class), any(Instant.class)))
          .thenReturn(new BigDecimal("800000"), new BigDecimal("400000"));

      // ===== ACT =====
      SubscriptionPlanResponse result = subscriptionPlanService.updatePlan(planId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Gói Giáo viên Chuyên sâu", result.getName()),
          () -> assertEquals("goi-giao-vien-chuyen-sau", result.getSlug()),
          () -> assertEquals(SubscriptionPlanStatus.INACTIVE, result.getStatus()),
          () -> assertEquals(BillingCycle.YEAR, result.getBillingCycle()),
          () -> assertEquals(300000, result.getTokenQuota()),
          () -> assertEquals(100.0, result.getStats().getGrowthPercent()));

      // ===== VERIFY =====
      verify(planRepository, times(1)).findById(planId);
      verify(planRepository, times(1)).existsBySlugAndIdNot("goi-giao-vien-chuyen-sau", planId);
      verify(planRepository, times(1)).save(basePlan);
      verify(subscriptionRepository, times(1))
          .countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE);
      verify(subscriptionRepository, times(2))
          .sumRevenueByPlan(eq(planId), any(Instant.class), any(Instant.class));
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }

    @Test
    void it_should_keep_features_unchanged_when_request_features_is_empty() {
      // ===== ARRANGE =====
      UpdateSubscriptionPlanRequest request =
          UpdateSubscriptionPlanRequest.builder().features(List.of()).build();
      List<String> originalFeatures = List.copyOf(basePlan.getFeatures());
      when(planRepository.findById(planId)).thenReturn(Optional.of(basePlan));
      when(planRepository.save(basePlan)).thenReturn(basePlan);
      when(subscriptionRepository.countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE))
          .thenReturn(0L);
      when(subscriptionRepository.sumRevenueByPlan(eq(planId), any(Instant.class), any(Instant.class)))
          .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);

      // ===== ACT =====
      subscriptionPlanService.updatePlan(planId, request);

      // ===== ASSERT =====
      assertEquals(originalFeatures, basePlan.getFeatures());

      // ===== VERIFY =====
      verify(planRepository, times(1)).findById(planId);
      verify(planRepository, never()).existsBySlugAndIdNot(any(), any());
      verify(planRepository, times(1)).save(basePlan);
      verify(subscriptionRepository, times(1))
          .countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE);
      verify(subscriptionRepository, times(2))
          .sumRevenueByPlan(eq(planId), any(Instant.class), any(Instant.class));
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }

    @Test
    void it_should_throw_exception_when_plan_not_found_or_deleted() {
      // ===== ARRANGE =====
      SubscriptionPlan deleted = buildPlan(planId, "Deleted Plan", "deleted-plan", false, true);
      deleted.setDeletedAt(Instant.parse("2026-04-01T00:00:00Z"));
      when(planRepository.findById(planId)).thenReturn(Optional.of(deleted));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> subscriptionPlanService.updatePlan(planId, UpdateSubscriptionPlanRequest.builder().build()));
      assertEquals(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(planRepository, times(1)).findById(planId);
      verify(planRepository, never()).save(any());
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("deletePlan()")
  class DeletePlanTests {

    @Test
    void it_should_soft_delete_plan_when_no_active_subscribers() {
      // ===== ARRANGE =====
      when(planRepository.findById(planId)).thenReturn(Optional.of(basePlan));
      when(subscriptionRepository.existsByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE))
          .thenReturn(false);

      // ===== ACT =====
      subscriptionPlanService.deletePlan(planId);

      // ===== ASSERT =====
      assertNotNull(basePlan.getDeletedAt());

      // ===== VERIFY =====
      verify(planRepository, times(1)).findById(planId);
      verify(subscriptionRepository, times(1))
          .existsByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE);
      verify(planRepository, times(1)).save(basePlan);
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }

    @Test
    void it_should_throw_exception_when_plan_has_active_subscribers() {
      // ===== ARRANGE =====
      when(planRepository.findById(planId)).thenReturn(Optional.of(basePlan));
      when(subscriptionRepository.existsByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE))
          .thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> subscriptionPlanService.deletePlan(planId));
      assertEquals(ErrorCode.SUBSCRIPTION_PLAN_HAS_ACTIVE_USERS, ex.getErrorCode());

      // ===== VERIFY =====
      verify(planRepository, times(1)).findById(planId);
      verify(subscriptionRepository, times(1))
          .existsByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE);
      verify(planRepository, never()).save(any());
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("getOverallStats()")
  class GetOverallStatsTests {

    @Test
    void it_should_calculate_trends_when_month_is_blank() {
      // ===== ARRANGE =====
      when(subscriptionRepository.sumTotalRevenue(any(Instant.class), any(Instant.class)))
          .thenReturn(new BigDecimal("1200000"), new BigDecimal("600000"));
      when(subscriptionRepository.countPaidSubscriptions(any(Instant.class), any(Instant.class)))
          .thenReturn(12L, 6L);
      when(userRepository.count()).thenReturn(20L);
      when(subscriptionRepository.countAllActivePaidSubscriptions()).thenReturn(15L);

      // ===== ACT =====
      SubscriptionOverallStatsResponse result = subscriptionPlanService.getOverallStats("   ");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1200000L, result.getTotalRevenue()),
          () -> assertEquals(100.0, result.getTotalRevenueTrend()),
          () -> assertEquals(12L, result.getTotalPaidUsers()),
          () -> assertEquals(100.0, result.getTotalPaidUsersTrend()),
          () -> assertEquals(100000L, result.getAvgRevenuePerUser()),
          () -> assertEquals(0.0, result.getAvgRevenuePerUserTrend()),
          () -> assertEquals(75.0, result.getConversionRate()),
          () -> assertTrue(result.getPeriod().matches("\\d{4}-\\d{2}")));

      // ===== VERIFY =====
      verify(subscriptionRepository, times(2)).sumTotalRevenue(any(Instant.class), any(Instant.class));
      verify(subscriptionRepository, times(2))
          .countPaidSubscriptions(any(Instant.class), any(Instant.class));
      verify(subscriptionRepository, times(1)).countAllActivePaidSubscriptions();
      verify(userRepository, times(1)).count();
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }

    @Test
    void it_should_return_zero_conversion_trend_when_total_users_is_zero_and_prev_conversion_is_zero() {
      // ===== ARRANGE =====
      YearMonth month = YearMonth.of(2026, 5);
      when(subscriptionRepository.sumTotalRevenue(any(Instant.class), any(Instant.class)))
          .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
      when(subscriptionRepository.countPaidSubscriptions(any(Instant.class), any(Instant.class)))
          .thenReturn(0L, 0L);
      when(userRepository.count()).thenReturn(0L);
      when(subscriptionRepository.countAllActivePaidSubscriptions()).thenReturn(0L);

      // ===== ACT =====
      SubscriptionOverallStatsResponse result = subscriptionPlanService.getOverallStats(month.toString());

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(0.0, result.getConversionRate()),
          () -> assertEquals(0.0, result.getConversionRateTrend()),
          () -> assertEquals("2026-05", result.getPeriod()));

      // ===== VERIFY =====
      verify(subscriptionRepository, times(2)).sumTotalRevenue(any(Instant.class), any(Instant.class));
      verify(subscriptionRepository, times(2))
          .countPaidSubscriptions(any(Instant.class), any(Instant.class));
      verify(subscriptionRepository, times(1)).countAllActivePaidSubscriptions();
      verify(userRepository, times(1)).count();
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("getRecentSubscriptions()")
  class GetRecentSubscriptionsTests {

    @Test
    void it_should_map_user_info_with_fallback_name_email_and_initial() {
      // ===== ARRANGE =====
      UUID knownUserId = UUID.fromString("66666666-7777-8888-9999-000000000001");
      UUID unknownUserId = UUID.fromString("66666666-7777-8888-9999-000000000002");
      UserSubscription knownSub = buildSubscription(basePlan, knownUserId, "Nguyen Quang Minh");
      UserSubscription unknownSub = buildSubscription(basePlan, unknownUserId, "placeholder");
      Page<UserSubscription> page = new PageImpl<>(List.of(knownSub, unknownSub), PageRequest.of(0, 10), 2);
      User knownUser = User.builder().fullName("Nguyen Quang Minh").email("nguyen.quang.minh@fptu.edu.vn").build();
      knownUser.setId(knownUserId);

      when(subscriptionRepository.findAllForAdmin(null, null, PageRequest.of(0, 10))).thenReturn(page);
      when(userRepository.findAllById(List.of(knownUserId, unknownUserId))).thenReturn(List.of(knownUser));

      // ===== ACT =====
      Page<UserSubscriptionResponse> result =
          subscriptionPlanService.getRecentSubscriptions(null, null, PageRequest.of(0, 10));

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(2, result.getContent().size()),
          () -> assertEquals("Nguyen Quang Minh", result.getContent().get(0).getUser().getName()),
          () -> assertEquals("N", result.getContent().get(0).getUser().getAvatarInitial()),
          () -> assertEquals("Unknown", result.getContent().get(1).getUser().getName()),
          () -> assertEquals("", result.getContent().get(1).getUser().getEmail()),
          () -> assertEquals("U", result.getContent().get(1).getUser().getAvatarInitial()));

      // ===== VERIFY =====
      verify(subscriptionRepository, times(1))
          .findAllForAdmin(null, null, PageRequest.of(0, 10));
      verify(userRepository, times(1)).findAllById(List.of(knownUserId, unknownUserId));
      verifyNoMoreInteractions(planRepository, subscriptionRepository, userRepository);
    }
  }
}
