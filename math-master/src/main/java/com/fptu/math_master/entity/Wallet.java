package com.fptu.math_master.entity;

import com.fptu.math_master.enums.Status;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "wallets")
public class Wallet {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "wallet_id", updatable = false, nullable = false)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false, unique = true)
  private User user;

  @Column(name = "balance", nullable = false, precision = 15, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
    if (balance == null) {
      balance = BigDecimal.ZERO;
    }
    if (status == null) {
      status = Status.ACTIVE;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }
}
