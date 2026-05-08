package com.fptu.math_master.configuration.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "minio")
@Validated
@Data
public class MinioProperties {

  @NotBlank private String endpoint;

  /** Browser-accessible MinIO URL. Defaults to endpoint if not set. */
  private String publicEndpoint;

  @NotBlank private String accessKey;

  @NotBlank private String secretKey;

  @NotBlank private String templateBucket = "slide-templates";

  @NotBlank private String verificationBucket = "teacher-verifications";

  @NotBlank private String courseVideosBucket = "course-videos";
  @NotBlank private String courseMaterialsBucket = "course-materials";
  /**
   * Bucket containing OCR image assets referenced by lesson page content blocks (imagePath/rawImageUrl).
   * Falls back to template bucket for backward compatibility.
   */
  @NotBlank private String ocrContentBucket = "slide-templates";
}
