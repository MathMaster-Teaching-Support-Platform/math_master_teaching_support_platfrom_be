package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.response.MySubscriptionResponse;
import com.fptu.math_master.entity.SubscriptionPlan;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.UserSubscription;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.enums.BillingCycle;
import com.fptu.math_master.enums.SubscriptionPlanStatus;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.SubscriptionPlanRepository;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserSubscriptionRepository;
import com.fptu.math_master.repository.WalletRepository;
import com.fptu.math_master.service.WalletService;
import com.fptu.math_master.util.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@DisplayName("UserSubscriptionServiceImpl - Tests")
class UserSubscriptionServiceImplTest extends BaseUnitTest {

  @InjectMocks private UserSubscriptionServiceImpl service;
  @Mock private UserSubscriptionRepository userSubscriptionRepository;
  @Mock private SubscriptionPlanRepository subscriptionPlanRepository;
  @Mock private WalletRepository walletRepository;
  @Mock private WalletService walletService;
  @Mock private TransactionRepository transactionRepository;

  private UUID userId;
  private UUID planId;
  private SubscriptionPlan monthlyPlan;
  private Wallet wallet;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    planId = UUID.randomUUID();
    monthlyPlan =
        SubscriptionPlan.builder()
            .name("Teacher Premium Monthly")
            .slug("teacher-premium-month")
            .price(new BigDecimal("199000"))
            .currency("VND")
            .publicVisible(true)
            .status(SubscriptionPlanStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTH)
            .tokenQuota(5000)
            .build();
    monthlyPlan.setId(planId);
    wallet = Wallet.builder().build();
    wallet.setId(UUID.randomUUID());
  }

  /** Normal case: Returns null when no active subscription exists. */
  @Test
  void it_should_return_null_when_get_my_active_subscription_has_no_data() {
    // ===== ARRANGE =====
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(userSubscriptionRepository.findActiveSubscriptionsByUserId(any(), any())).thenReturn(List.of());

      // ===== ACT =====
      MySubscriptionResponse result = service.getMyActiveSubscription();

      // ===== ASSERT =====
      assertNull(result);

      // ===== VERIFY =====
      verify(userSubscriptionRepository).findActiveSubscriptionsByUserId(any(), any());
    }
  }

  /** Normal case: Returns mapped response when active subscription exists. */
  @Test
  void it_should_return_active_subscription_when_get_my_active_subscription_has_data() {
    // ===== ARRANGE =====
    UserSubscription active =
        UserSubscription.builder()
            .userId(userId)
            .plan(monthlyPlan)
            .status(UserSubscriptionStatus.ACTIVE)
            .tokenQuota(5000)
            .tokenRemaining(4200)
            .amount(new BigDecimal("199000"))
            .currency("VND")
            .paymentMethod("wallet")
            .startDate(Instant.now())
            .endDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .build();
    active.setId(UUID.randomUUID());

    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(userSubscriptionRepository.findActiveSubscriptionsByUserId(any(), any())).thenReturn(List.of(active));

      // ===== ACT =====
      MySubscriptionResponse result = service.getMyActiveSubscription();

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(active.getId(), result.getSubscriptionId());
      assertEquals(4200, result.getTokenRemaining());
    }
  }

  /** Normal case: Purchases paid plan, expires old subscription, carries over tokens. */
  @Test
  void it_should_purchase_plan_and_carry_over_tokens_when_active_subscription_exists() {
    // ===== ARRANGE =====
    UserSubscription oldSubscription =
        UserSubscription.builder()
            .userId(userId)
            .plan(monthlyPlan)
            .status(UserSubscriptionStatus.ACTIVE)
            .tokenRemaining(1200)
            .build();
    oldSubscription.setId(UUID.randomUUID());
    UserSubscription savedSubscription =
        UserSubscription.builder()
            .userId(userId)
            .plan(monthlyPlan)
            .status(UserSubscriptionStatus.ACTIVE)
            .tokenQuota(5000)
            .tokenRemaining(6200)
            .amount(monthlyPlan.getPrice())
            .currency("VND")
            .paymentMethod("wallet")
            .startDate(Instant.now())
            .endDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .build();
    savedSubscription.setId(UUID.randomUUID());

    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));
      when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
      when(userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(any(), any()))
          .thenReturn(List.of(oldSubscription));
      when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(savedSubscription);

      // ===== ACT =====
      MySubscriptionResponse result = service.purchasePlan(planId);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(planId, result.getPlanId());
      assertEquals(6200, result.getTokenRemaining());

      // ===== VERIFY =====
      verify(walletService, times(1)).deductBalance(wallet.getId(), new BigDecimal("199000"));
      verify(userSubscriptionRepository, times(1)).saveAll(any());
      verify(userSubscriptionRepository, times(1)).save(any(UserSubscription.class));
      verify(transactionRepository, times(1)).save(any(Transaction.class));
    }
  }

  /** Abnormal case: Throws when plan is not purchasable due to non-public visibility. */
  @Test
  void it_should_throw_exception_when_plan_is_not_public() {
    // ===== ARRANGE =====
    monthlyPlan.setPublicVisible(false);
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> service.purchasePlan(planId));
      assertEquals(ErrorCode.SUBSCRIPTION_PLAN_NOT_PURCHASABLE, exception.getErrorCode());

      // ===== VERIFY =====
      verify(walletRepository, never()).findByUserId(any());
    }
  }

  /** Abnormal case: Throws when plan is missing or inactive/deleted after filters. */
  @Test
  void it_should_throw_exception_when_plan_not_found_by_purchase_filters() {
    // ===== ARRANGE =====
    monthlyPlan.setStatus(SubscriptionPlanStatus.INACTIVE);
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> service.purchasePlan(planId));
      assertEquals(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND, exception.getErrorCode());
    }
  }

  /** Abnormal case: Throws when purchasable plan has negative price. */
  @Test
  void it_should_throw_exception_when_plan_price_is_negative() {
    // ===== ARRANGE =====
    monthlyPlan.setPrice(new BigDecimal("-1000"));
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));
      when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> service.purchasePlan(planId));
      assertEquals(ErrorCode.SUBSCRIPTION_PLAN_NOT_PURCHASABLE, exception.getErrorCode());
      verify(walletService, never()).deductBalance(any(), any());
    }
  }

  /** Abnormal case: Throws when plan has null price. */
  @Test
  void it_should_throw_exception_when_plan_price_is_null() {
    // ===== ARRANGE =====
    monthlyPlan.setPrice(null);
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> service.purchasePlan(planId));
      assertEquals(ErrorCode.SUBSCRIPTION_PLAN_NOT_PURCHASABLE, exception.getErrorCode());
    }
  }

  /** Abnormal case: Throws when wallet is missing for purchasing user. */
  @Test
  void it_should_throw_exception_when_wallet_not_found() {
    // ===== ARRANGE =====
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));
      when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> service.purchasePlan(planId));
      assertEquals(ErrorCode.WALLET_NOT_FOUND, exception.getErrorCode());
    }
  }

  /** Normal case: Free plan purchase skips wallet deduction and sets FOREVER end date null. */
  @Test
  void it_should_skip_wallet_deduction_when_plan_price_is_zero() {
    // ===== ARRANGE =====
    monthlyPlan.setPrice(BigDecimal.ZERO);
    monthlyPlan.setBillingCycle(BillingCycle.FOREVER);
    UserSubscription saved =
        UserSubscription.builder()
            .userId(userId)
            .plan(monthlyPlan)
            .status(UserSubscriptionStatus.ACTIVE)
            .tokenQuota(5000)
            .tokenRemaining(5000)
            .amount(BigDecimal.ZERO)
            .currency("VND")
            .paymentMethod("wallet")
            .build();
    saved.setId(UUID.randomUUID());
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));
      when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
      when(userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(any(), any()))
          .thenReturn(List.of());
      when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(saved);

      // ===== ACT =====
      MySubscriptionResponse result = service.purchasePlan(planId);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(5000, result.getTokenRemaining());
      assertNull(result.getEndDate());

      // ===== VERIFY =====
      verify(walletService, never()).deductBalance(any(), any());
    }
  }

  /** Normal case: Calculates end date for three-month billing cycle. */
  @Test
  void it_should_set_end_date_plus_90_days_when_billing_cycle_is_three_months() {
    // ===== ARRANGE =====
    monthlyPlan.setBillingCycle(BillingCycle.THREE_MONTHS);
    UserSubscription saved =
        UserSubscription.builder()
            .userId(userId)
            .plan(monthlyPlan)
            .status(UserSubscriptionStatus.ACTIVE)
            .tokenQuota(5000)
            .tokenRemaining(5000)
            .amount(monthlyPlan.getPrice())
            .currency("VND")
            .paymentMethod("wallet")
            .startDate(Instant.now())
            .endDate(Instant.now().plus(90, ChronoUnit.DAYS))
            .build();
    saved.setId(UUID.randomUUID());
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));
      when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
      when(userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(any(), any()))
          .thenReturn(List.of());
      when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(saved);

      // ===== ACT =====
      MySubscriptionResponse result = service.purchasePlan(planId);

      // ===== ASSERT =====
      assertNotNull(result.getEndDate());
    }
  }

  /** Normal case: Calculates end date for yearly billing cycle. */
  @Test
  void it_should_set_end_date_plus_365_days_when_billing_cycle_is_year() {
    // ===== ARRANGE =====
    monthlyPlan.setBillingCycle(BillingCycle.YEAR);
    UserSubscription saved =
        UserSubscription.builder()
            .userId(userId)
            .plan(monthlyPlan)
            .status(UserSubscriptionStatus.ACTIVE)
            .tokenQuota(5000)
            .tokenRemaining(5000)
            .amount(monthlyPlan.getPrice())
            .currency("VND")
            .paymentMethod("wallet")
            .startDate(Instant.now())
            .endDate(Instant.now().plus(365, ChronoUnit.DAYS))
            .build();
    saved.setId(UUID.randomUUID());
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(monthlyPlan));
      when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
      when(userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(any(), any()))
          .thenReturn(List.of());
      when(userSubscriptionRepository.save(any(UserSubscription.class))).thenReturn(saved);

      // ===== ACT =====
      MySubscriptionResponse result = service.purchasePlan(planId);

      // ===== ASSERT =====
      assertNotNull(result.getEndDate());
    }
  }

  /** Abnormal case: Throws when consuming tokens with no active subscription. */
  @Test
  void it_should_throw_exception_when_consuming_tokens_without_active_subscription() {
    // ===== ARRANGE =====
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(any(), any()))
          .thenReturn(List.of());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> service.consumeMyTokens(100, "LATEX_RENDER"));
      assertEquals(ErrorCode.NO_ACTIVE_SUBSCRIPTION, exception.getErrorCode());
    }
  }

  /** Normal case: Ignores consume call when tokens requested are non-positive. */
  @Test
  void it_should_skip_consume_when_requested_tokens_are_non_positive() {
    // ===== ARRANGE =====

    // ===== ACT =====
    service.consumeMyTokens(0, "LATEX_RENDER");

    // ===== ASSERT & VERIFY =====
    verify(userSubscriptionRepository, never()).findActiveSubscriptionsByUserIdForUpdate(any(), any());
    verify(userSubscriptionRepository, never()).save(any(UserSubscription.class));
  }

  /** Abnormal case: Throws when token balance is insufficient. */
  @Test
  void it_should_throw_exception_when_token_balance_is_insufficient() {
    // ===== ARRANGE =====
    UserSubscription subscription =
        UserSubscription.builder()
            .userId(userId)
            .plan(monthlyPlan)
            .status(UserSubscriptionStatus.ACTIVE)
            .tokenRemaining(99)
            .build();
    subscription.setId(UUID.randomUUID());
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(any(), any()))
          .thenReturn(List.of(subscription));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> service.consumeMyTokens(100, "LATEX_RENDER"));
      assertEquals(ErrorCode.INSUFFICIENT_SUBSCRIPTION_TOKENS, exception.getErrorCode());
      verify(userSubscriptionRepository, never()).save(any(UserSubscription.class));
    }
  }

  /** Normal case: Deducts token amount from active subscription. */
  @Test
  void it_should_consume_tokens_when_balance_is_sufficient() {
    // ===== ARRANGE =====
    UserSubscription subscription =
        UserSubscription.builder()
            .userId(userId)
            .plan(monthlyPlan)
            .status(UserSubscriptionStatus.ACTIVE)
            .tokenRemaining(200)
            .build();
    subscription.setId(UUID.randomUUID());
    try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
      mocked.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
      when(userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(any(), any()))
          .thenReturn(List.of(subscription));
      when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(inv -> inv.getArgument(0));

      // ===== ACT =====
      service.consumeMyTokens(50, "LATEX_RENDER");

      // ===== ASSERT =====
      assertEquals(150, subscription.getTokenRemaining());

      // ===== VERIFY =====
      verify(userSubscriptionRepository, times(1)).save(subscription);
    }
  }
}
