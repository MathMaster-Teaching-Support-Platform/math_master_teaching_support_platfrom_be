package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.RefundStatus;
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
public class RefundRequestResponse {
    private UUID id;
    private UUID orderId;
    private String orderNumber;
    private UUID enrollmentId;
    private UUID studentId;
    private String studentName;
    private UUID courseId;
    private String courseTitle;
    private RefundStatus status;
    private String reason;
    private BigDecimal refundAmount;
    private BigDecimal instructorDeduction;
    private BigDecimal platformDeduction;
    private Instant requestedAt;
    private Instant processedAt;
    private UUID processedBy;
    private String processorName;
    private String adminNotes;
    private boolean isAutoApproved;
    private Instant createdAt;
    private Instant updatedAt;
}