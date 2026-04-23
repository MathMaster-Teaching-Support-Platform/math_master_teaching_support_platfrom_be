package com.fptu.math_master.entity;

import com.fptu.math_master.enums.OrderStatus;
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
    name = "orders",
    indexes = {
      @Index(name = "idx_orders_student", columnList = "student_id"),
      @Index(name = "idx_orders_course", columnList = "course_id"),
      @Index(name = "idx_orders_status", columnList = "status"),
      @Index(name = "idx_orders_number", columnList = "order_number"),
      @Index(name = "idx_orders_expires_at", columnList = "expires_at")
    })
public class Order extends BaseEntity {

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "enrollment_id")
  private UUID enrollmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private OrderStatus status = OrderStatus.PENDING;

  @Column(name = "order_number", unique = true, nullable = false, length = 50)
  private String orderNumber;

  @Column(name = "original_price", precision = 15, scale = 2)
  private BigDecimal originalPrice;

  @Column(name = "discount_amount", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "final_price", nullable = false, precision = 15, scale = 2)
  private BigDecimal finalPrice;

  @Column(name = "instructor_earnings", precision = 15, scale = 2)
  private BigDecimal instructorEarnings;

  @Column(name = "platform_commission", precision = 15, scale = 2)
  private BigDecimal platformCommission;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancellation_reason", columnDefinition = "TEXT")
  private String cancellationReason;

  // ─── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", insertable = false, updatable = false)
  private Course course;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", insertable = false, updatable = false)
  private Enrollment enrollment;

  @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
  private Transaction studentTransaction;

  @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
  private Transaction instructorTransaction;
}