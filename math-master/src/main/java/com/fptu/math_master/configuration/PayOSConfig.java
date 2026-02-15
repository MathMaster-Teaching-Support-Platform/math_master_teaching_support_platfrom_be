package com.fptu.math_master.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class PayOSConfig {

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
