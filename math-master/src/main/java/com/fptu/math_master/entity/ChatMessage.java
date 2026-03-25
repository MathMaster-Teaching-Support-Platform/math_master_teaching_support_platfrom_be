package com.fptu.math_master.entity;

import com.fptu.math_master.enums.ChatMessageRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    name = "chat_messages",
    indexes = {
      @Index(name = "idx_chat_messages_session", columnList = "session_id"),
      @Index(name = "idx_chat_messages_session_created", columnList = "session_id, created_at"),
      @Index(name = "idx_chat_messages_session_seq", columnList = "session_id, sequence_no"),
      @Index(name = "idx_chat_messages_deleted", columnList = "deleted_at")
    })
/** The entity of 'ChatMessage'. */
public class ChatMessage extends BaseEntity {

  /** session_id */
  @Column(name = "session_id", nullable = false)
  private UUID sessionId;

  /** user_id */
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  /** role */
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private ChatMessageRole role;

  /** content */
  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  /** word_count */
  @Column(name = "word_count", nullable = false)
  private Integer wordCount;

  /** model */
  @Column(name = "model", length = 100)
  private String model;

  /** latency_ms */
  @Column(name = "latency_ms")
  private Integer latencyMs;

  /** sequence_no */
  @Column(name = "sequence_no", nullable = false)
  private Long sequenceNo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private ChatSession session;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User user;
}
