package com.fptu.math_master.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

  private String baseUrl;
  private String model;
  private Integer timeoutSeconds;
}
