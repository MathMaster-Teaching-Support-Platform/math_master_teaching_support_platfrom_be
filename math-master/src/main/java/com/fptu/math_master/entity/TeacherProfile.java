package com.fptu.math_master.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fptu.math_master.enums.ProfileStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
@Table(name = "teacher_profiles")
/**
 * The entity of 'TeacherProfile'.
 */
public class TeacherProfile extends BaseEntity {

  /**
   * Relationships
   * - One-to-One with User
   * - Many-to-One with School
   */
  @OneToOne
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  /**
   * school_name
   */
  @Column(name = "school_name", nullable = false)
  private String schoolName;

  /**
   * school_address
   */
  @Column(name = "school_address")
  private String schoolAddress;

  /**
   * school_website
   */
  @Column(name = "school_website")
  private String schoolWebsite;

  /**
   * position
   */
  @Column(name = "position", nullable = false)
  private String position;

  /**
   * verification_document_key
   */
  @Column(name = "verification_document_key")
  private String verificationDocumentKey;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * status
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ProfileStatus status;

  /**
   * admin_comment
   */
  @Column(name = "admin_comment", columnDefinition = "TEXT")
  private String adminComment;

  /**
   * reviewed_by
   */
  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  /**
   * reviewed_at
   */
  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;
}
