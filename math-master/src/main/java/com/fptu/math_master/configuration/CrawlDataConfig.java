package com.fptu.math_master.configuration;

import com.fptu.math_master.configuration.properties.CrawlDataProperties;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CrawlDataConfig {

  CrawlDataProperties properties;

  @Bean(name = "crawlDataRestClient")
  public RestClient crawlDataRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    int timeoutMs = (int) Duration.ofSeconds(properties.getTimeoutSeconds()).toMillis();
    factory.setConnectTimeout(timeoutMs);
    factory.setReadTimeout(timeoutMs);

    return RestClient.builder()
        .baseUrl(properties.getBaseUrl())
        .defaultHeader("X-Internal-API-Key", properties.getInternalApiKey())
        .requestFactory(factory)
        .build();
  }
}
