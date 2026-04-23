package com.fptu.math_master.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "notification_preferences",
    indexes = {
      @Index(name = "idx_notification_preferences_user", columnList = "user_id"),
      @Index(name = "idx_notification_preferences_user_type", columnList = "user_id, notification_type")
    },
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_notification_preferences_user_type", columnNames = {"user_id", "notification_type"})
    })
public class NotificationPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType; // e.g., "COURSE", "PROFILE_VERIFICATION", "SYSTEM"

    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Column(name = "push_enabled")
    private boolean pushEnabled = true;

    @Column(name = "in_app_enabled")
    private boolean inAppEnabled = true;
}