package com.fptu.math_master.configuration.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for application initialization.
 *
 * <p>Binds to properties with prefix {@code app.init} from application.yml/properties. Supports
 * loading passwords from environment variables for security.
 *
 * <p>Example configuration:
 *
 * <pre>
 * app:
 *   init:
 *     enabled: true
 *     admin:
 *       username: admin
 *       password: ${APP_INIT_ADMIN_PASSWORD}
 *       email: admin@example.com
 *       fullname: System Administrator
 * </pre>
 *
 * @author Math Master Team
 * @version 2.0
 * @since 2024-02-07
 */
@Component
@ConfigurationProperties(prefix = "app.init")
@Validated
@Data
public class InitProperties {

  /** Whether to enable application initialization. Default: true */
  private boolean enabled = true;

  /** Admin user configuration. */
  @Valid private UserConfig admin;

  /** Student user configuration. */
  @Valid private UserConfig student;

  /** Teacher user configuration. */
  @Valid private UserConfig teacher;

  /** Configuration for a single user account. */
  @Data
  public static class UserConfig {

    /** Username for login. */
    @NotBlank(message = "Username cannot be blank")
    private String username;

    /** Password (should be loaded from environment variable). */
    @NotBlank(message = "Password cannot be blank")
    private String password;

    /** Email address. */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;

    /** Full name of the user. */
    @NotBlank(message = "Full name cannot be blank")
    private String fullname;
  }
}
