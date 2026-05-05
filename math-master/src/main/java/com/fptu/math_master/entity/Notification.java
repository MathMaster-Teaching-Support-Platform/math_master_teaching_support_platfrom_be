package com.fptu.math_master.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "notifications",
    indexes = {
      @Index(name = "idx_notifications_recipient", columnList = "recipient_id"),
      @Index(name = "idx_notifications_unread", columnList = "recipient_id, is_read")
    })
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "type", nullable = false, length = 50)
  private String type;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Type(JsonBinaryType.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "is_read")
  private boolean isRead = false;

  @Column(name = "action_url")
  private String actionUrl;
}
