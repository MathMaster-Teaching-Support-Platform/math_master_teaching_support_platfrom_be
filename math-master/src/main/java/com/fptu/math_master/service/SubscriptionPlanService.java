package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateSubscriptionPlanRequest;
import com.fptu.math_master.dto.request.UpdateSubscriptionPlanRequest;
import com.fptu.math_master.dto.response.SubscriptionOverallStatsResponse;
import com.fptu.math_master.dto.response.SubscriptionPlanResponse;
import com.fptu.math_master.dto.response.UserSubscriptionResponse;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscriptionPlanService {

  List<SubscriptionPlanResponse> getAllPlans();

  SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request);

  SubscriptionPlanResponse updatePlan(UUID planId, UpdateSubscriptionPlanRequest request);

  void deletePlan(UUID planId);

  SubscriptionOverallStatsResponse getOverallStats(String month);

  Page<UserSubscriptionResponse> getRecentSubscriptions(
      UserSubscriptionStatus status, UUID planId, Pageable pageable);
}
