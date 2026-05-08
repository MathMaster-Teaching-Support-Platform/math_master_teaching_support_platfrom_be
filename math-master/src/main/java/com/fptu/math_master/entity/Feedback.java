package com.fptu.math_master.entity;

import com.fptu.math_master.enums.FeedbackStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "feedbacks",
    indexes = {
      @Index(name = "idx_feedbacks_sender", columnList = "sender_id"),
      @Index(name = "idx_feedbacks_status", columnList = "status")
    })
public class Feedback extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "related_url")
  private String relatedUrl;

  @Column(name = "category", length = 100)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  @Builder.Default
  private FeedbackStatus status = FeedbackStatus.OPEN;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "responded_by")
  private User respondedBy;

  @Column(name = "response_message", columnDefinition = "TEXT")
  private String responseMessage;

  @Column(name = "sender_read_at")
  private Instant senderReadAt;

  @Column(name = "admin_read_at")
  private Instant adminReadAt;
}
