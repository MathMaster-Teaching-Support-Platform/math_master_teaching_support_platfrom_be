package com.fptu.math_master.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.dto.response.MySubscriptionResponse;
import com.fptu.math_master.entity.SubscriptionPlan;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.UserSubscription;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.enums.SubscriptionPlanStatus;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.SubscriptionPlanRepository;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserSubscriptionRepository;
import com.fptu.math_master.repository.WalletRepository;
import com.fptu.math_master.service.UserSubscriptionService;
import com.fptu.math_master.service.WalletService;
import com.fptu.math_master.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionServiceImpl implements UserSubscriptionService {

  private final UserSubscriptionRepository userSubscriptionRepository;
  private final SubscriptionPlanRepository subscriptionPlanRepository;
  private final WalletRepository walletRepository;
  private final WalletService walletService;
  private final TransactionRepository transactionRepository;

  @Override
  @Transactional(readOnly = true)
  public MySubscriptionResponse getMyActiveSubscription() {
    UUID userId = SecurityUtils.getCurrentUserId();
    UserSubscription subscription = findLatestActiveSubscription(userId, false);
    if (subscription == null) {
      return null;
    }
    return toResponse(subscription);
  }

  @Override
  @Transactional
  public MySubscriptionResponse purchasePlan(UUID planId) {
    UUID userId = SecurityUtils.getCurrentUserId();

    SubscriptionPlan plan =
        subscriptionPlanRepository
            .findById(planId)
            .filter(p -> p.getDeletedAt() == null)
            .filter(p -> p.getStatus() == SubscriptionPlanStatus.ACTIVE)
            .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

    if (!plan.isPublicVisible() || plan.getPrice() == null) {
      throw new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_PURCHASABLE);
    }

    Wallet wallet =
        walletRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

    BigDecimal price = plan.getPrice();
    if (price.compareTo(BigDecimal.ZERO) < 0) {
      throw new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_PURCHASABLE);
    }

    if (price.compareTo(BigDecimal.ZERO) > 0) {
      walletService.deductBalance(wallet.getId(), price);
    }

    Instant now = Instant.now();
    List<UserSubscription> activeSubscriptions =
        userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(userId, now);

    int carryOverTokens =
        activeSubscriptions.stream()
            .mapToInt(
                sub -> {
                  Integer remaining = sub.getTokenRemaining();
                  return remaining == null ? 0 : Math.max(remaining, 0);
                })
            .sum();

    if (!activeSubscriptions.isEmpty()) {
      activeSubscriptions.forEach(
          sub -> {
            sub.setStatus(UserSubscriptionStatus.EXPIRED);
            sub.setEndDate(now);
          });
      userSubscriptionRepository.saveAll(activeSubscriptions);
    }

    Integer tokenQuota = plan.getTokenQuota() == null ? 0 : Math.max(plan.getTokenQuota(), 0);
    int tokenRemaining = tokenQuota + carryOverTokens;

    UserSubscription subscription =
        UserSubscription.builder()
            .userId(userId)
            .plan(plan)
            .startDate(now)
            .endDate(calculateEndDate(now, plan))
            .amount(price)
            .currency(plan.getCurrency())
            .status(UserSubscriptionStatus.ACTIVE)
            .paymentMethod("wallet")
            .tokenQuota(tokenQuota)
            .tokenRemaining(tokenRemaining)
            .build();

    UserSubscription saved = userSubscriptionRepository.save(subscription);

    Transaction transaction =
        Transaction.builder()
            .wallet(wallet)
            .amount(price)
            .type(TransactionType.PAYMENT)
            .status(TransactionStatus.SUCCESS)
            .description("Subscription purchase: " + plan.getName())
            .transactionDate(now)
            .referenceCode("SUB_" + saved.getId())
            .build();
    transactionRepository.save(transaction);

    log.info(
        "Subscription purchased. userId={}, planId={}, subscriptionId={}, tokenQuota={}, carryOverTokens={}, tokenRemaining={}",
        userId,
        planId,
        saved.getId(),
        tokenQuota,
        carryOverTokens,
        tokenRemaining);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public void consumeMyTokens(int tokens, String feature) {
    if (tokens <= 0) {
      return;
    }

    UUID userId = SecurityUtils.getCurrentUserId();
    UserSubscription subscription = findLatestActiveSubscription(userId, true);
    if (subscription == null) {
      throw new AppException(ErrorCode.NO_ACTIVE_SUBSCRIPTION);
    }

    int remaining = subscription.getTokenRemaining() == null ? 0 : subscription.getTokenRemaining();
    if (remaining < tokens) {
      throw new AppException(ErrorCode.INSUFFICIENT_SUBSCRIPTION_TOKENS);
    }

    subscription.setTokenRemaining(remaining - tokens);
    userSubscriptionRepository.save(subscription);

    log.info(
        "Consumed subscription tokens. userId={}, subscriptionId={}, feature={}, used={}, remaining={}",
        userId,
        subscription.getId(),
        feature,
        tokens,
        subscription.getTokenRemaining());
  }

  private UserSubscription findLatestActiveSubscription(UUID userId, boolean forUpdate) {
    Instant now = Instant.now();
    List<UserSubscription> subscriptions =
        forUpdate
            ? userSubscriptionRepository.findActiveSubscriptionsByUserIdForUpdate(userId, now)
            : userSubscriptionRepository.findActiveSubscriptionsByUserId(userId, now);

    return subscriptions.isEmpty() ? null : subscriptions.get(0);
  }

  private Instant calculateEndDate(Instant startDate, SubscriptionPlan plan) {
    return switch (plan.getBillingCycle()) {
      case FOREVER -> null;
      case MONTH -> startDate.plus(30, ChronoUnit.DAYS);
      case THREE_MONTHS -> startDate.plus(90, ChronoUnit.DAYS);
      case SIX_MONTHS -> startDate.plus(180, ChronoUnit.DAYS);
      case YEAR -> startDate.plus(365, ChronoUnit.DAYS);
      case CUSTOM -> startDate.plus(30, ChronoUnit.DAYS);
    };
  }

  private MySubscriptionResponse toResponse(UserSubscription subscription) {
    return MySubscriptionResponse.builder()
        .subscriptionId(subscription.getId())
        .planId(subscription.getPlan().getId())
        .planName(subscription.getPlan().getName())
        .planSlug(subscription.getPlan().getSlug())
        .billingCycle(subscription.getPlan().getBillingCycle())
        .status(subscription.getStatus())
        .startDate(subscription.getStartDate())
        .endDate(subscription.getEndDate())
        .amount(subscription.getAmount())
        .currency(subscription.getCurrency())
        .tokenQuota(subscription.getTokenQuota())
        .tokenRemaining(subscription.getTokenRemaining())
        .paymentMethod(subscription.getPaymentMethod())
        .build();
  }
}
