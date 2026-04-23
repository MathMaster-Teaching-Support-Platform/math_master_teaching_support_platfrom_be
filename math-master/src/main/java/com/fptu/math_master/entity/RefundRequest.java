package com.fptu.math_master.entity;

import com.fptu.math_master.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "refund_requests",
    indexes = {
      @Index(name = "idx_refund_requests_order", columnList = "order_id"),
      @Index(name = "idx_refund_requests_enrollment", columnList = "enrollment_id"),
      @Index(name = "idx_refund_requests_student", columnList = "student_id"),
      @Index(name = "idx_refund_requests_status", columnList = "status"),
      @Index(name = "idx_refund_requests_requested_at", columnList = "requested_at")
    })
public class RefundRequest extends BaseEntity {

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private RefundStatus status = RefundStatus.PENDING;

  @Column(name = "reason", columnDefinition = "TEXT")
  private String reason;

  @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal refundAmount;

  @Column(name = "instructor_deduction", precision = 15, scale = 2)
  private BigDecimal instructorDeduction;

  @Column(name = "platform_deduction", precision = 15, scale = 2)
  private BigDecimal platformDeduction;

  @Column(name = "requested_at", nullable = false)
  @Builder.Default
  private Instant requestedAt = Instant.now();

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "processed_by")
  private UUID processedBy;

  @Column(name = "admin_notes", columnDefinition = "TEXT")
  private String adminNotes;

  @Column(name = "is_auto_approved")
  @Builder.Default
  private boolean isAutoApproved = false;

  // ─── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", insertable = false, updatable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", insertable = false, updatable = false)
  private Enrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_by", insertable = false, updatable = false)
  private User processor;
}