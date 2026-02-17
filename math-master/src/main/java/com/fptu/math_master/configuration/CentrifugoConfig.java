package com.fptu.math_master.configuration;

import com.fptu.math_master.configuration.properties.CentrifugoProperties;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CentrifugoConfig {

  private final CentrifugoProperties centrifugoProperties;

  @Bean
  public OkHttpClient centrifugoHttpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();
  }

  @Bean
  public CentrifugoProperties getCentrifugoProperties() {
    log.info(
        "Initializing Centrifugo configuration with API URL: {}", centrifugoProperties.getApiUrl());
    return centrifugoProperties;
  }
}
