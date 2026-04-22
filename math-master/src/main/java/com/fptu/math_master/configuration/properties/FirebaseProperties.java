package com.fptu.math_master.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

  private boolean enabled;

  private String serviceAccountPath;

  private String serviceAccountJson;

  private String projectId;
}
