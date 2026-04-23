package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private UUID studentId;
    private String studentName;
    private UUID courseId;
    private String courseTitle;
    private String courseThumbnailUrl;
    private UUID enrollmentId;
    private OrderStatus status;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private BigDecimal instructorEarnings;
    private BigDecimal platformCommission;
    private Instant expiresAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private Instant createdAt;
    private Instant updatedAt;
}