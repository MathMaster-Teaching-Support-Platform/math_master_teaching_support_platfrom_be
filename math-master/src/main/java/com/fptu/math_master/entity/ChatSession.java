package com.fptu.math_master.entity;

import com.fptu.math_master.enums.ChatSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "chat_sessions",
    indexes = {
      @Index(name = "idx_chat_sessions_user", columnList = "user_id"),
      @Index(name = "idx_chat_sessions_user_status", columnList = "user_id, status"),
      @Index(name = "idx_chat_sessions_user_last_message", columnList = "user_id, last_message_at"),
      @Index(name = "idx_chat_sessions_deleted", columnList = "deleted_at")
    })
/** The entity of 'ChatSession'. */
public class ChatSession extends BaseEntity {

  /** user_id */
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  /** title */
  @Size(max = 200)
  @Column(name = "title", nullable = false, length = 200)
  private String title;

  /** status */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ChatSessionStatus status;

  /** model */
  @Column(name = "model", length = 100)
  private String model;

  /** last_message_at */
  @Column(name = "last_message_at")
  private Instant lastMessageAt;

  /** total_messages */
  @Column(name = "total_messages")
  private Integer totalMessages;

  /** total_words */
  @Column(name = "total_words")
  private Integer totalWords;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User user;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (status == null) status = ChatSessionStatus.ACTIVE;
    if (totalMessages == null) totalMessages = 0;
    if (totalWords == null) totalWords = 0;
    if (lastMessageAt == null) lastMessageAt = Instant.now();
  }
}
