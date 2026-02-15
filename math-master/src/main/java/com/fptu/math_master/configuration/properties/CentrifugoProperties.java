package com.fptu.math_master.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "centrifugo")
public class CentrifugoProperties {

  private String apiUrl;

  private String apiKey;

  private String tokenHmacSecret;

  private String wsUrl;
}
