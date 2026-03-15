package com.fptu.math_master.entity;

import com.fptu.math_master.enums.ProfileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  @ManyToOne
  @JoinColumn(name = "school_id", nullable = false)
  School school;

  /**
   * position
   */
  @Column(name = "position", nullable = false)
  String position;

  /**
   * certificate_url
   */
  @Column(name = "certificate_url")
  String certificateUrl;

  /**
   * identification_document_url
   */
  @Column(name = "identification_document_url")
  String identificationDocumentUrl;

  /**
   * description
   */
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
