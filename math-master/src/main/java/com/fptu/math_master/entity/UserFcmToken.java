package com.fptu.math_master.entity;

import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "user_fcm_tokens",
    indexes = {
      @Index(name = "idx_user_fcm_tokens_user_active", columnList = "user_id, is_active"),
      @Index(name = "idx_user_fcm_tokens_last_seen", columnList = "last_seen_at")
    },
    uniqueConstraints = {@UniqueConstraint(name = "uk_user_fcm_tokens_token", columnNames = "token")})
public class UserFcmToken extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token", nullable = false, columnDefinition = "TEXT")
  private String token;

  @Column(name = "device_info", length = 255)
  private String deviceInfo;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "last_seen_at")
  private java.time.Instant lastSeenAt;
}
