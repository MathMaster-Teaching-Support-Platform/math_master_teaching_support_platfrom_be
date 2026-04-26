package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.OrderResponse;
import com.fptu.math_master.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Order Management", description = "APIs for managing course orders")
public class OrderController {

  OrderService orderService;

  @PostMapping("/courses/{courseId}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Create order", description = "Create a new order for a course")
  public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@PathVariable UUID courseId) {
    log.info("Creating order for course: {}", courseId);
    OrderResponse response = orderService.createOrder(courseId);
    return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
        .code(200)
        .message("Order created successfully")
        .result(response)
        .build());
  }

  @GetMapping("/{orderId}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get order by ID", description = "Get order details by order ID")
  public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID orderId) {
    log.info("Getting order: {}", orderId);
    OrderResponse response = orderService.getOrder(orderId);
    return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
        .code(200)
        .message("Order retrieved successfully")
        .result(response)
        .build());
  }

  @GetMapping("/number/{orderNumber}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get order by number", description = "Get order details by order number")
  public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(@PathVariable String orderNumber) {
    log.info("Getting order by number: {}", orderNumber);
    OrderResponse response = orderService.getOrderByNumber(orderNumber);
    return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
        .code(200)
        .message("Order retrieved successfully")
        .result(response)
        .build());
  }

  @PostMapping("/{orderId}/confirm")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Confirm order", description = "Confirm and process payment for an order")
  public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(@PathVariable UUID orderId) {
    log.info("Confirming order: {}", orderId);
    OrderResponse response = orderService.confirmOrder(orderId);
    return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
        .code(200)
        .message("Order confirmed successfully")
        .result(response)
        .build());
  }

  @PostMapping("/{orderId}/cancel")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Cancel order", description = "Cancel a pending order")
  public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
      @PathVariable UUID orderId,
      @RequestParam(required = false) String reason) {
    log.info("Cancelling order: {}", orderId);
    OrderResponse response = orderService.cancelOrder(orderId, reason);
    return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
        .code(200)
        .message("Order cancelled successfully")
        .result(response)
        .build());
  }

  @GetMapping("/my-orders")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get my orders", description = "Get all orders for the current student")
  public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    log.info("Getting my orders - page: {}, size: {}", page, size);
    Pageable pageable = PageRequest.of(page, size);
    Page<OrderResponse> response = orderService.getMyOrders(pageable);
    return ResponseEntity.ok(ApiResponse.<Page<OrderResponse>>builder()
        .code(200)
        .message("Orders retrieved successfully")
        .result(response)
        .build());
  }

  @GetMapping("/courses/{courseId}/pending")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Check pending order", description = "Check if student has a pending order for a course")
  public ResponseEntity<ApiResponse<Boolean>> hasPendingOrder(
      @PathVariable UUID courseId) {
    log.info("Checking pending order for course: {}", courseId);
    UUID studentId = com.fptu.math_master.util.SecurityUtils.getCurrentUserId();
    boolean hasPending = orderService.hasPendingOrder(studentId, courseId);
    return ResponseEntity.ok(ApiResponse.<Boolean>builder()
        .code(200)
        .message("Pending order check completed")
        .result(hasPending)
        .build());
  }
}
