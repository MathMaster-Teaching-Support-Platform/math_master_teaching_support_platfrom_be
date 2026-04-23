package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    
    /**
     * Create a new order for a course
     */
    OrderResponse createOrder(UUID courseId);
    
    /**
     * Get order by ID (student can only see their own orders)
     */
    OrderResponse getOrder(UUID orderId);
    
    /**
     * Get order by order number
     */
    OrderResponse getOrderByNumber(String orderNumber);
    
    /**
     * Confirm and process an order (payment)
     */
    OrderResponse confirmOrder(UUID orderId);
    
    /**
     * Cancel an order
     */
    OrderResponse cancelOrder(UUID orderId, String reason);
    
    /**
     * Get my orders (paginated)
     */
    Page<OrderResponse> getMyOrders(Pageable pageable);
    
    /**
     * Cancel expired orders (scheduled job)
     */
    void cancelExpiredOrders();
    
    /**
     * Check if user has pending order for course
     */
    boolean hasPendingOrder(UUID studentId, UUID courseId);
}