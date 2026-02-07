package com.fptu.math_master.configuration;

import com.fptu.math_master.configuration.properties.InitProperties;
import com.fptu.math_master.service.ApplicationInitLogic;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class that orchestrates application initialization.
 *
 * <p>This is a thin configuration layer that delegates actual initialization
 * logic to {@link ApplicationInitLogic}. Can be disabled via
 * {@code app.init.enabled=false} property.
 *
 * @author Math Master Team
 * @version 2.0
 * @since 2024-02-07
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitConfig {

  private static final String LOG_INIT_START = "Starting application initialization...";
  private static final String LOG_INIT_SUCCESS = "Application initialization completed successfully";
  private static final String LOG_INIT_FAILED = "Failed to initialize application data";

  private final InitProperties initProperties;
  private final ApplicationInitLogic applicationInitLogic;

  /**
   * Creates an ApplicationRunner bean that initializes data when the application starts.
   *
   * <p>Only runs when {@code app.init.enabled=true} (default).
   * Works with any database (PostgreSQL, MySQL, H2, etc.)
   *
   * @return ApplicationRunner that executes data initialization
   */
  @Bean
  @ConditionalOnProperty(
    prefix = "app.init",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
  ApplicationRunner applicationRunner() {
    return args -> {
      log.info(LOG_INIT_START);
      try {
        applicationInitLogic.initialize(initProperties);
        log.info(LOG_INIT_SUCCESS);
      } catch (Exception e) {
        log.error(LOG_INIT_FAILED, e);
        throw e;
      }
    };
  }
}
