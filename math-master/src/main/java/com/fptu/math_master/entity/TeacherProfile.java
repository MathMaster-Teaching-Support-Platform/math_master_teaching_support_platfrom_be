package com.fptu.math_master.entity;

import com.fptu.math_master.enums.ProfileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
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
  User user;

  /** school_name */
  @Column(name = "school_name", nullable = false)
  String schoolName;

  /** school_address */
  @Column(name = "school_address")
  String schoolAddress;

  /** school_website */
  @Column(name = "school_website")
  String schoolWebsite;

  /** position */
  @Column(name = "position", nullable = false)
  String position;

  /** document_url */
  @Column(name = "document_url", nullable = false)
  String documentUrl;

  /** document_type */
  @Column(name = "document_type", nullable = false)
  String documentType;

  /** description */
  @Column(name = "description", columnDefinition = "TEXT")
  String description;

  /**
   * status
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  ProfileStatus status;

  /**
   * admin_comment
   */
  @Column(name = "admin_comment", columnDefinition = "TEXT")
  String adminComment;

  /**
   * reviewed_by
   */
  @Column(name = "reviewed_by")
  UUID reviewedBy;

  /**
   * reviewed_at
   */
  @Column(name = "reviewed_at")
  LocalDateTime reviewedAt;
}
