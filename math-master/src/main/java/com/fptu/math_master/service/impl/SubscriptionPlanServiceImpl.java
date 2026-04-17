package com.fptu.math_master.service.impl;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.dto.request.CreateSubscriptionPlanRequest;
import com.fptu.math_master.dto.request.UpdateSubscriptionPlanRequest;
import com.fptu.math_master.dto.response.SubscriptionOverallStatsResponse;
import com.fptu.math_master.dto.response.SubscriptionPlanResponse;
import com.fptu.math_master.dto.response.UserSubscriptionResponse;
import com.fptu.math_master.entity.SubscriptionPlan;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.UserSubscription;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.SubscriptionPlanRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.repository.UserSubscriptionRepository;
import com.fptu.math_master.service.SubscriptionPlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

  private final SubscriptionPlanRepository planRepository;
  private final UserSubscriptionRepository subscriptionRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public List<SubscriptionPlanResponse> getPublicPurchasablePlans() {
    return planRepository.findPublicPurchasablePlans().stream()
        .map(plan -> toResponse(plan, 0L, 0L, 0.0))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubscriptionPlanResponse> getAllPlans() {
    List<SubscriptionPlan> plans = planRepository.findAllActive();
    YearMonth current = YearMonth.now(ZoneOffset.UTC);
    YearMonth previous = current.minusMonths(1);

    Instant curStart = current.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant curEnd = current.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant prevStart = previous.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    return plans.stream()
        .map(plan -> {
          long activeUsers = subscriptionRepository.countByPlanIdAndStatus(
              plan.getId(), UserSubscriptionStatus.ACTIVE);
          BigDecimal revThis = subscriptionRepository.sumRevenueByPlan(plan.getId(), curStart, curEnd);
          BigDecimal revPrev = subscriptionRepository.sumRevenueByPlan(plan.getId(), prevStart, curStart);
          long revenueThisMonth = revThis != null ? revThis.longValue() : 0L;
          long revPrevLong = revPrev != null ? revPrev.longValue() : 0L;
          double growth = calcGrowth(revPrevLong, revenueThisMonth);
          return toResponse(plan, activeUsers, revenueThisMonth, growth);
        })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request) {
    String slug = generateSlug(request.getName());
    if (planRepository.existsBySlug(slug)) {
      throw new AppException(ErrorCode.SUBSCRIPTION_PLAN_SLUG_EXISTS);
    }

    SubscriptionPlan plan = SubscriptionPlan.builder()
        .name(request.getName())
        .slug(slug)
        .price(request.getPrice())
        .billingCycle(request.getBillingCycle())
        .description(request.getDescription())
        .featured(request.isFeatured())
        .publicVisible(request.isPublic())
        .features(request.getFeatures())
      .tokenQuota(request.getTokenQuota() == null ? 0 : request.getTokenQuota())
        .build();

    plan.setCreatedAt(Instant.now());
    plan.setUpdatedAt(Instant.now());

    SubscriptionPlan saved = planRepository.save(plan);
    return toResponse(saved, 0L, 0L, 0.0);
  }

  @Override
  @Transactional
  public SubscriptionPlanResponse updatePlan(UUID planId, UpdateSubscriptionPlanRequest request) {
    SubscriptionPlan plan = planRepository.findById(planId)
        .filter(p -> p.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

    if (request.getName() != null) {
      String newSlug = generateSlug(request.getName());
      if (planRepository.existsBySlugAndIdNot(newSlug, planId)) {
        throw new AppException(ErrorCode.SUBSCRIPTION_PLAN_SLUG_EXISTS);
      }
      plan.setName(request.getName());
      plan.setSlug(newSlug);
    }
    if (request.getDescription() != null) {
      plan.setDescription(request.getDescription());
    }
    if (request.getPrice() != null) {
      plan.setPrice(request.getPrice());
    }
    if (request.getBillingCycle() != null) {
      plan.setBillingCycle(request.getBillingCycle());
    }
    if (request.getFeatures() != null && !request.getFeatures().isEmpty()) {
      plan.setFeatures(request.getFeatures());
    }
    if (request.getTokenQuota() != null) {
      plan.setTokenQuota(request.getTokenQuota());
    }
    if (request.getFeatured() != null) {
      plan.setFeatured(request.getFeatured());
    }
    if (request.getIsPublic() != null) {
      plan.setPublicVisible(request.getIsPublic());
    }
    if (request.getStatus() != null) {
      plan.setStatus(request.getStatus());
    }
    plan.setUpdatedAt(Instant.now());

    SubscriptionPlan saved = planRepository.save(plan);

    YearMonth current = YearMonth.now(ZoneOffset.UTC);
    YearMonth previous = current.minusMonths(1);
    Instant curStart = current.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant curEnd = current.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant prevStart = previous.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    long activeUsers = subscriptionRepository.countByPlanIdAndStatus(planId, UserSubscriptionStatus.ACTIVE);
    BigDecimal revThis = subscriptionRepository.sumRevenueByPlan(planId, curStart, curEnd);
    BigDecimal revPrev = subscriptionRepository.sumRevenueByPlan(planId, prevStart, curStart);
    long revenueThisMonth = revThis != null ? revThis.longValue() : 0L;
    long revPrevLong = revPrev != null ? revPrev.longValue() : 0L;
    double growth = calcGrowth(revPrevLong, revenueThisMonth);

    return toResponse(saved, activeUsers, revenueThisMonth, growth);
  }

  @Override
  @Transactional
  public void deletePlan(UUID planId) {
    SubscriptionPlan plan = planRepository.findById(planId)
        .filter(p -> p.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

    boolean hasActiveSubscribers = subscriptionRepository.existsByPlanIdAndStatus(
        planId, UserSubscriptionStatus.ACTIVE);
    if (hasActiveSubscribers) {
      throw new AppException(ErrorCode.SUBSCRIPTION_PLAN_HAS_ACTIVE_USERS);
    }

    plan.setDeletedAt(Instant.now());
    planRepository.save(plan);
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionOverallStatsResponse getOverallStats(String month) {
    YearMonth targetMonth = (month != null && !month.isBlank())
        ? YearMonth.parse(month)
        : YearMonth.now(ZoneOffset.UTC);
    YearMonth prevMonth = targetMonth.minusMonths(1);

    Instant curStart = targetMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant curEnd = targetMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant prevStart = prevMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    BigDecimal revThis = subscriptionRepository.sumTotalRevenue(curStart, curEnd);
    BigDecimal revPrev = subscriptionRepository.sumTotalRevenue(prevStart, curStart);
    long totalRevenue = revThis != null ? revThis.longValue() : 0L;
    long prevRevenue = revPrev != null ? revPrev.longValue() : 0L;
    double revenueTrend = calcGrowth(prevRevenue, totalRevenue);

    long paidThis = subscriptionRepository.countPaidSubscriptions(curStart, curEnd);
    long paidPrev = subscriptionRepository.countPaidSubscriptions(prevStart, curStart);
    double paidTrend = calcGrowth(paidPrev, paidThis);

    long avgRevenue = paidThis > 0 ? totalRevenue / paidThis : 0L;
    long prevAvgRevenue = paidPrev > 0 ? prevRevenue / paidPrev : 0L;
    double avgTrend = calcGrowth(prevAvgRevenue, avgRevenue);

    long totalUsers = userRepository.count();
    long allActivePaid = subscriptionRepository.countAllActivePaidSubscriptions();
    double conversionRate = totalUsers > 0
        ? Math.min(100.0, (allActivePaid * 100.0) / totalUsers)
        : 0.0;
    double prevAllActivePaidEstimate = paidPrev;
    double prevConversionRate = totalUsers > 0
        ? Math.min(100.0, (prevAllActivePaidEstimate * 100.0) / totalUsers)
        : 0.0;
    double conversionTrend = prevConversionRate > 0
        ? Math.round((conversionRate - prevConversionRate) * 10.0) / 10.0
        : 0.0;

    return SubscriptionOverallStatsResponse.builder()
        .totalRevenue(totalRevenue)
        .totalRevenueTrend(revenueTrend)
        .totalPaidUsers(paidThis)
        .totalPaidUsersTrend(paidTrend)
        .avgRevenuePerUser(avgRevenue)
        .avgRevenuePerUserTrend(avgTrend)
        .conversionRate(Math.round(conversionRate * 10.0) / 10.0)
        .conversionRateTrend(conversionTrend)
        .period(targetMonth.toString())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserSubscriptionResponse> getRecentSubscriptions(
      UserSubscriptionStatus status, UUID planId, Pageable pageable) {

    Page<UserSubscription> page = subscriptionRepository.findAllForAdmin(status, planId, pageable);

    // Batch-load users to avoid N+1
    List<UUID> userIds = page.getContent().stream()
        .map(UserSubscription::getUserId)
        .distinct()
        .collect(Collectors.toList());

    Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));

    return page.map(sub -> {
      User user = userMap.get(sub.getUserId());
      String name = user != null && user.getFullName() != null ? user.getFullName() : "Unknown";
      String email = user != null ? user.getEmail() : "";
      String initial = name.isBlank() ? "?" : String.valueOf(name.charAt(0)).toUpperCase();

      return UserSubscriptionResponse.builder()
          .id(sub.getId())
          .user(UserSubscriptionResponse.UserInfo.builder()
              .id(sub.getUserId())
              .name(name)
              .email(email)
              .avatarInitial(initial)
              .build())
          .plan(UserSubscriptionResponse.PlanInfo.builder()
              .id(sub.getPlan().getId())
              .name(sub.getPlan().getName())
              .slug(sub.getPlan().getSlug())
              .build())
          .startDate(sub.getStartDate())
          .endDate(sub.getEndDate())
          .amount(sub.getAmount())
          .currency(sub.getCurrency())
          .tokenQuota(sub.getTokenQuota())
          .tokenRemaining(sub.getTokenRemaining())
          .status(sub.getStatus())
          .paymentMethod(sub.getPaymentMethod())
          .createdAt(sub.getCreatedAt())
          .build();
    });
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private SubscriptionPlanResponse toResponse(
      SubscriptionPlan plan, long activeUsers, long revenueThisMonth, double growth) {
    return SubscriptionPlanResponse.builder()
        .id(plan.getId())
        .name(plan.getName())
        .slug(plan.getSlug())
        .price(plan.getPrice())
        .currency(plan.getCurrency())
        .billingCycle(plan.getBillingCycle())
        .description(plan.getDescription())
        .featured(plan.isFeatured())
        .isPublic(plan.isPublicVisible())
        .status(plan.getStatus())
        .features(plan.getFeatures())
        .tokenQuota(plan.getTokenQuota())
        .stats(SubscriptionPlanResponse.PlanStats.builder()
            .activeUsers(activeUsers)
            .revenueThisMonth(revenueThisMonth)
            .growthPercent(growth)
            .build())
        .createdAt(plan.getCreatedAt())
        .updatedAt(plan.getUpdatedAt())
        .build();
  }

  private double calcGrowth(long previous, long current) {
    if (previous == 0) return current > 0 ? 100.0 : 0.0;
    return Math.round(((current - previous) * 100.0 / previous) * 10.0) / 10.0;
  }

  /**
   * Generate a URL-safe slug from a Vietnamese/English name.
   * Example: "Giáo viên" → "giao-vien"
   */
  private String generateSlug(String name) {
    String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
    String ascii = normalized.replaceAll("\\p{M}", "")       // remove diacritical marks
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s-]", "")                      // remove non-alphanumeric
        .trim()
        .replaceAll("\\s+", "-");                              // spaces → hyphens
    return ascii.isEmpty() ? "plan-" + System.currentTimeMillis() : ascii;
  }
}
