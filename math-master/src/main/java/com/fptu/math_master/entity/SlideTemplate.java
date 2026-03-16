package com.fptu.math_master.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "slide_templates",
    indexes = {
      @Index(name = "idx_slide_templates_active", columnList = "is_active"),
      @Index(name = "idx_slide_templates_uploaded_by", columnList = "uploaded_by"),
      @Index(name = "idx_slide_templates_name", columnList = "name")
    })
public class SlideTemplate extends BaseEntity {

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  /**
   * description
   */
  @Lob
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Size(max = 255)
  @Column(name = "original_file_name", length = 255, nullable = false)
  private String originalFileName;

  @Size(max = 100)
  @Column(name = "content_type", length = 100, nullable = false)
  private String contentType;

  @Size(max = 500)
  @Column(name = "object_key", length = 500, nullable = false)
  private String objectKey;

  @Size(max = 100)
  @Column(name = "bucket_name", length = 100, nullable = false)
  private String bucketName;

  @Column(name = "uploaded_by", nullable = false)
  private UUID uploadedBy;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @PrePersist
  public void prePersistSlideTemplate() {
    if (isActive == null) {
      isActive = true;
    }
  }
}
