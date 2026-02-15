package com.fptu.math_master.service;

import com.fptu.math_master.configuration.properties.InitProperties;

/**
 * Logic interface for application initialization.
 *
 * <p>Handles the creation of default permissions, roles, and users required for the application to
 * function.
 *
 * @author Math Master Team
 * @version 2.0
 * @since 2024-02-07
 */
public interface ApplicationInitLogic {

  /**
   * Initializes the application with default data.
   *
   * <p>Creates:
   *
   * <ul>
   *   <li>Default permissions (user, role, permission, quiz management)
   *   <li>Default roles (ADMIN, TEACHER, STUDENT)
   *   <li>Default users based on provided configuration
   * </ul>
   *
   * @param properties configuration properties containing user credentials
   * @throws IllegalStateException if required permissions are not found
   */
  void initialize(InitProperties properties);
}
