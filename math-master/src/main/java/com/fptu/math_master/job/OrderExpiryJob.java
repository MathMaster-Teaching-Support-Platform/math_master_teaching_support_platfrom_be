package com.fptu.math_master.job;

import com.fptu.math_master.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to cancel expired orders.
 * Runs every 5 minutes to check for orders that have exceeded their 15-minute expiry window.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderExpiryJob {

  OrderService orderService;

  /**
   * Cancel expired orders.
   * Runs every 5 minutes (300,000 milliseconds).
   */
  @Scheduled(fixedDelay = 300000, initialDelay = 60000)
  public void cancelExpiredOrders() {
    log.debug("Starting order expiry job");
    try {
      orderService.cancelExpiredOrders();
      log.debug("Order expiry job completed successfully");
    } catch (Exception e) {
      log.error("Error during order expiry job execution", e);
    }
  }
}
