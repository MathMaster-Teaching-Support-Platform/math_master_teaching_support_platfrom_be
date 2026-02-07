package com.fptu.math_master.entity;

import com.fptu.math_master.enums.ProfileStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @OneToOne
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  User user;

  @ManyToOne
  @JoinColumn(name = "school_id", nullable = false)
  School school;

  @Column(name = "position", nullable = false)
  String position;

  @Column(name = "certificate_url")
  String certificateUrl; // URL của file minh chứng (upload lên cloud storage)

  @Column(name = "identification_document_url")
  String identificationDocumentUrl; // URL của CMND/CCCD

  @Column(name = "description", columnDefinition = "TEXT")
  String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  ProfileStatus status;

  @Column(name = "admin_comment", columnDefinition = "TEXT")
  String adminComment; // Lý do từ chối hoặc ghi chú của admin

  @Column(name = "reviewed_by")
  Integer reviewedBy; // ID của admin đã duyệt

  @Column(name = "reviewed_at")
  LocalDateTime reviewedAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;
}
