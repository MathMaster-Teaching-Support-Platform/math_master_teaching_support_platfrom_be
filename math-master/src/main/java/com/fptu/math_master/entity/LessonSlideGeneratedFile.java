package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
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
    name = "lesson_slide_generated_files",
    indexes = {
      @Index(name = "idx_slide_generated_lesson", columnList = "lesson_id"),
      @Index(name = "idx_slide_generated_created_by", columnList = "created_by"),
      @Index(name = "idx_slide_generated_public", columnList = "is_public")
    })
public class LessonSlideGeneratedFile extends BaseEntity {

  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  @Column(name = "template_id")
  private UUID templateId;

  @Column(name = "bucket_name", nullable = false, length = 255)
  private String bucketName;

  @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
  private String objectKey;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "slide_name", length = 255)
  private String name;

  @Column(name = "thumbnail_url", columnDefinition = "TEXT")
  private String thumbnail;

  @Column(name = "content_type", nullable = false, length = 255)
  private String contentType;

  @Column(name = "file_size_bytes", nullable = false)
  private Long fileSizeBytes;

  @Column(name = "is_public", nullable = false)
  private Boolean isPublic;

  @Column(name = "published_at")
  private Instant publishedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (isPublic == null) {
      isPublic = Boolean.FALSE;
    }
  }
}
