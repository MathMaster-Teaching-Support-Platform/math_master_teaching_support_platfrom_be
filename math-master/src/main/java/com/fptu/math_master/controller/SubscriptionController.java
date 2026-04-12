package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.MySubscriptionResponse;
import com.fptu.math_master.service.UserSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subscriptions", description = "User subscription purchase and token usage")
public class SubscriptionController {

  private final UserSubscriptionService userSubscriptionService;

  @GetMapping("/me")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Get my active subscription")
  public ApiResponse<MySubscriptionResponse> getMySubscription() {
    return ApiResponse.<MySubscriptionResponse>builder()
        .result(userSubscriptionService.getMyActiveSubscription())
        .build();
  }

  @PostMapping("/purchase/{planId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Purchase plan with wallet balance")
  public ApiResponse<MySubscriptionResponse> purchasePlan(@PathVariable UUID planId) {
    return ApiResponse.<MySubscriptionResponse>builder()
        .message("Subscription purchased successfully")
        .result(userSubscriptionService.purchasePlan(planId))
        .build();
  }
}
