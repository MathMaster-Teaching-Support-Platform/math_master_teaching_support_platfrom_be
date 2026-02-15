package com.fptu.math_master.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payos")
public class PayOSProperties {

  private String clientId;

  private String apiKey;

  private String checksumKey;

  private String returnUrl;

  private String cancelUrl;
}
