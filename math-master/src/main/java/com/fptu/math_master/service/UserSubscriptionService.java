package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.MySubscriptionResponse;
import java.util.UUID;

public interface UserSubscriptionService {

  MySubscriptionResponse getMyActiveSubscription();

  MySubscriptionResponse purchasePlan(UUID planId);

  void consumeMyTokens(int tokens, String feature);
}
