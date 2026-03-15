package com.fptu.math_master.entity;

import com.fptu.math_master.enums.Status;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.*;

/**
 * The entity of 'Wallet'.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@AttributeOverride(
    name = "id",
    column = @Column(name = "wallet_id", updatable = false, nullable = false))
@Table(name = "wallets")
public class Wallet extends BaseEntity {

  /**
   * user
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false, unique = true)
  private User user;

  /**
   * balance
   */
  @Builder.Default
  @Column(name = "balance", nullable = false, precision = 15, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  /**
   * status
   */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.ACTIVE;
}
