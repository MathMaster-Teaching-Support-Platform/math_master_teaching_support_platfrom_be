package com.fptu.math_master.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.request.CreateSubscriptionPlanRequest;
import com.fptu.math_master.dto.request.UpdateSubscriptionPlanRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.SubscriptionOverallStatsResponse;
import com.fptu.math_master.dto.response.SubscriptionPlanResponse;
import com.fptu.math_master.dto.response.UserSubscriptionResponse;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import com.fptu.math_master.service.SubscriptionPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/subscription-plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Subscription Plans", description = "CRUD and statistics for subscription plans")
public class AdminSubscriptionController {

  private final SubscriptionPlanService subscriptionPlanService;

  @GetMapping
  @Operation(
      summary = "List all subscription plans",
      description = "Returns all non-deleted plans with live stats (activeUsers, revenueThisMonth, growthPercent).")
  public ApiResponse<List<SubscriptionPlanResponse>> getAllPlans() {
    return ApiResponse.<List<SubscriptionPlanResponse>>builder()
        .result(subscriptionPlanService.getAllPlans())
        .build();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a new subscription plan",
      description = "Slug is auto-generated from the plan name.")
  public ApiResponse<SubscriptionPlanResponse> createPlan(
      @Valid @RequestBody CreateSubscriptionPlanRequest request) {
    return ApiResponse.<SubscriptionPlanResponse>builder()
        .result(subscriptionPlanService.createPlan(request))
        .build();
  }

  @PutMapping("/{planId}")
  @Operation(
      summary = "Update a subscription plan",
      description = "Partial update — only provided fields are changed. If name changes, slug is regenerated.")
  public ApiResponse<SubscriptionPlanResponse> updatePlan(
      @PathVariable UUID planId,
      @Valid @RequestBody UpdateSubscriptionPlanRequest request) {
    return ApiResponse.<SubscriptionPlanResponse>builder()
        .result(subscriptionPlanService.updatePlan(planId, request))
        .build();
  }

  @DeleteMapping("/{planId}")
  @Operation(
      summary = "Delete a subscription plan",
      description = "Soft-deletes the plan. Returns 400 if the plan has active subscribers — deactivate it first via PUT status=INACTIVE.")
  public ApiResponse<Void> deletePlan(@PathVariable UUID planId) {
    subscriptionPlanService.deletePlan(planId);
    return ApiResponse.<Void>builder()
        .message("Plan deleted successfully")
        .build();
  }

  @GetMapping("/stats")
  @Operation(
      summary = "Overall subscription revenue statistics",
      description = "Returns total revenue, paid users, avg revenue/user, and conversion rate for the given month vs previous month.")
  public ApiResponse<SubscriptionOverallStatsResponse> getOverallStats(
      @RequestParam(required = false) String month) {
    return ApiResponse.<SubscriptionOverallStatsResponse>builder()
        .result(subscriptionPlanService.getOverallStats(month))
        .build();
  }

  @GetMapping("/subscriptions")
  @Operation(
      summary = "List recent user subscriptions",
      description = "Paginated list filterable by status and planId. Default sorted by createdAt DESC.")
  public ApiResponse<Page<UserSubscriptionResponse>> getRecentSubscriptions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID planId,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String order) {

    UserSubscriptionStatus statusEnum = null;
    if (status != null && !status.equalsIgnoreCase("all")) {
      statusEnum = UserSubscriptionStatus.valueOf(status.toUpperCase());
    }

    Sort.Direction direction = order.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    return ApiResponse.<Page<UserSubscriptionResponse>>builder()
        .result(subscriptionPlanService.getRecentSubscriptions(statusEnum, planId, pageable))
        .build();
  }
}
