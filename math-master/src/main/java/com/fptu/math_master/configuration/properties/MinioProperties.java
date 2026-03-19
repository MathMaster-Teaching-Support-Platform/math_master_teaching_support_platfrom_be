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

  @NotBlank private String accessKey;

  @NotBlank private String secretKey;

  @NotBlank private String templateBucket = "slide-templates";

  @NotBlank private String verificationBucket = "teacher-verifications";
}
