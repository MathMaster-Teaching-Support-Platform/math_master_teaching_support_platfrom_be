package com.fptu.math_master.service.impl;

import com.fptu.math_master.configuration.properties.InitProperties;
import com.fptu.math_master.constant.PredefinedPermission;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.entity.Permission;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.repository.PermissionRepository;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.ApplicationInitLogic;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link ApplicationInitLogic}.
 *
 * <p>Provides the business logic for initializing application data including permissions, roles,
 * and default users.
 *
 * @author Math Master Team
 * @version 2.0
 * @since 2024-02-07
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitLogicImpl implements ApplicationInitLogic {

  // Logging messages
  private static final String LOG_CREATING_PERMISSIONS = "Creating permissions...";
  private static final String LOG_CREATING_ROLE_PERMISSIONS =
      "Creating role-specific permissions...";
  private static final String LOG_CREATING_ROLES = "Creating roles...";
  private static final String LOG_CREATING_USERS = "Creating default users...";
  private static final String LOG_DATA_COMPLETED =
      "Data initialization completed - {} permissions, 3 roles created";

  private static final String LOG_PERMISSION_CREATED = "Creating permission: {}";
  private static final String LOG_ROLE_CREATED = "Creating role: {} with {} permissions";
  private static final String LOG_USER_CREATED =
      "Default user created - Username: {} - PLEASE CHANGE THE PASSWORD!";
  private static final String LOG_USER_EXISTS = "User already exists: {}";

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /** {@inheritDoc} */
  @Override
  @Transactional
  public void initialize(InitProperties properties) {
    log.debug(LOG_CREATING_PERMISSIONS);
    Set<Permission> allPermissions = createAllPermissions();

    log.debug(LOG_CREATING_ROLE_PERMISSIONS);
    Set<Permission> studentPermissions = getStudentPermissions();
    Set<Permission> teacherPermissions = getTeacherPermissions();

    log.debug(LOG_CREATING_ROLES);
    Role studentRole = createRoleIfNotExists(PredefinedRole.STUDENT_ROLE, studentPermissions);
    Role teacherRole = createRoleIfNotExists(PredefinedRole.TEACHER_ROLE, teacherPermissions);
    Role adminRole = createRoleIfNotExists(PredefinedRole.ADMIN_ROLE, allPermissions);

    log.debug(LOG_CREATING_USERS);
    createUserIfNotExists(properties.getAdmin(), adminRole);
    createUserIfNotExists(properties.getTeacher(), teacherRole);
    createUserIfNotExists(properties.getStudent(), studentRole);

    log.info(LOG_DATA_COMPLETED, allPermissions.size());
  }

  /**
   * Creates all system permissions including user, role, permission, and quiz management.
   *
   * @return Set of all created permissions
   */
  private Set<Permission> createAllPermissions() {
    Set<Permission> permissions = new HashSet<>();

    permissions.addAll(createUserPermissions());
    permissions.addAll(createRolePermissions());
    permissions.addAll(createPermissionPermissions());
    permissions.addAll(createQuizPermissions());

    return permissions;
  }

  /**
   * Creates user management permissions (CREATE, READ, UPDATE, DELETE).
   *
   * @return Set of user-related permissions
   */
  private Set<Permission> createUserPermissions() {
    return createPermissionGroup(
        new PermissionDefinition(
            PredefinedPermission.USER_CREATE, "Create User", "Permission to create new users"),
        new PermissionDefinition(
            PredefinedPermission.USER_READ, "Read User", "Permission to view user information"),
        new PermissionDefinition(
            PredefinedPermission.USER_UPDATE,
            "Update User",
            "Permission to update user information"),
        new PermissionDefinition(
            PredefinedPermission.USER_DELETE, "Delete User", "Permission to delete users"));
  }

  /**
   * Creates role management permissions (CREATE, READ, UPDATE, DELETE).
   *
   * @return Set of role-related permissions
   */
  private Set<Permission> createRolePermissions() {
    return createPermissionGroup(
        new PermissionDefinition(
            PredefinedPermission.ROLE_CREATE, "Create Role", "Permission to create new roles"),
        new PermissionDefinition(
            PredefinedPermission.ROLE_READ, "Read Role", "Permission to view roles"),
        new PermissionDefinition(
            PredefinedPermission.ROLE_UPDATE, "Update Role", "Permission to update roles"),
        new PermissionDefinition(
            PredefinedPermission.ROLE_DELETE, "Delete Role", "Permission to delete roles"));
  }

  /**
   * Creates permission management permissions (CREATE, READ, UPDATE, DELETE).
   *
   * @return Set of permission-related permissions
   */
  private Set<Permission> createPermissionPermissions() {
    return createPermissionGroup(
        new PermissionDefinition(
            PredefinedPermission.PERMISSION_CREATE,
            "Create Permission",
            "Permission to create new permissions"),
        new PermissionDefinition(
            PredefinedPermission.PERMISSION_READ,
            "Read Permission",
            "Permission to view permissions"),
        new PermissionDefinition(
            PredefinedPermission.PERMISSION_UPDATE,
            "Update Permission",
            "Permission to update permissions"),
        new PermissionDefinition(
            PredefinedPermission.PERMISSION_DELETE,
            "Delete Permission",
            "Permission to delete permissions"));
  }

  /**
   * Creates quiz management permissions (CREATE, READ, UPDATE, DELETE, SUBMIT).
   *
   * @return Set of quiz-related permissions
   */
  private Set<Permission> createQuizPermissions() {
    return createPermissionGroup(
        new PermissionDefinition(
            PredefinedPermission.QUIZ_CREATE, "Create Quiz", "Permission to create new quizzes"),
        new PermissionDefinition(
            PredefinedPermission.QUIZ_READ, "Read Quiz", "Permission to view quizzes"),
        new PermissionDefinition(
            PredefinedPermission.QUIZ_UPDATE, "Update Quiz", "Permission to update quizzes"),
        new PermissionDefinition(
            PredefinedPermission.QUIZ_DELETE, "Delete Quiz", "Permission to delete quizzes"),
        new PermissionDefinition(
            PredefinedPermission.QUIZ_SUBMIT, "Submit Quiz", "Permission to submit quiz answers"));
  }

  /**
   * Creates a group of permissions from their definitions.
   *
   * @param definitions varargs array of permission definitions
   * @return Set of created permissions
   */
  private Set<Permission> createPermissionGroup(PermissionDefinition... definitions) {
    Set<Permission> permissions = new HashSet<>();
    Arrays.stream(definitions)
        .forEach(
            def ->
                permissions.add(createPermissionIfNotExists(def.code, def.name, def.description)));
    return permissions;
  }

  /**
   * Retrieves student-specific permissions (READ user info, READ quizzes, SUBMIT quizzes).
   *
   * @return Set of student permissions
   * @throws IllegalStateException if required permissions don't exist
   */
  private Set<Permission> getStudentPermissions() {
    return getPermissionsByCode(
        PredefinedPermission.USER_READ,
        PredefinedPermission.QUIZ_READ,
        PredefinedPermission.QUIZ_SUBMIT);
  }

  /**
   * Retrieves teacher-specific permissions (student permissions + quiz management).
   *
   * @return Set of teacher permissions
   * @throws IllegalStateException if required permissions don't exist
   */
  private Set<Permission> getTeacherPermissions() {
    return getPermissionsByCode(
        PredefinedPermission.USER_READ,
        PredefinedPermission.QUIZ_READ,
        PredefinedPermission.QUIZ_CREATE,
        PredefinedPermission.QUIZ_UPDATE,
        PredefinedPermission.QUIZ_DELETE,
        PredefinedPermission.QUIZ_SUBMIT);
  }

  /**
   * Retrieves multiple permissions by their codes.
   *
   * @param codes permission codes to retrieve
   * @return Set of found permissions
   * @throws IllegalStateException if any permission is not found
   */
  private Set<Permission> getPermissionsByCode(String... codes) {
    Set<Permission> permissions = new HashSet<>();
    Arrays.stream(codes).forEach(code -> permissions.add(findPermissionByCode(code)));
    return permissions;
  }

  /**
   * Finds a permission by its code.
   *
   * @param code permission code
   * @return found Permission
   * @throws IllegalStateException if permission not found
   */
  private Permission findPermissionByCode(String code) {
    return permissionRepository
        .findByCode(code)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    String.format("Required permission not found: %s", code)));
  }

  /**
   * Creates a permission if it doesn't already exist.
   *
   * @param code permission code
   * @param name permission name
   * @param description permission description
   * @return existing or newly created Permission
   */
  private Permission createPermissionIfNotExists(String code, String name, String description) {
    return permissionRepository
        .findByCode(code)
        .orElseGet(
            () -> {
              log.debug(LOG_PERMISSION_CREATED, code);
              return permissionRepository.save(
                  Permission.builder().code(code).name(name).description(description).build());
            });
  }

  /**
   * Creates a role if it doesn't already exist.
   *
   * @param roleName name of the role
   * @param permissions set of permissions for the role
   * @return existing or newly created Role
   */
  private Role createRoleIfNotExists(String roleName, Set<Permission> permissions) {
    return roleRepository
        .findByName(roleName)
        .orElseGet(
            () -> {
              log.debug(LOG_ROLE_CREATED, roleName, permissions.size());
              return roleRepository.save(
                  Role.builder().name(roleName).permissions(permissions).build());
            });
  }

  /**
   * Creates a user if one with the given username doesn't already exist.
   *
   * @param userConfig user configuration from properties
   * @param role user's role
   */
  private void createUserIfNotExists(InitProperties.UserConfig userConfig, Role role) {
    String username = userConfig.getUsername();

    if (userRepository.findByUserName(username).isEmpty()) {
      User user = buildUser(userConfig, role);
      userRepository.save(user);

      log.warn(LOG_USER_CREATED, username);
    } else {
      log.debug(LOG_USER_EXISTS, username);
    }
  }

  /**
   * Builds a User entity from configuration.
   *
   * @param userConfig user configuration
   * @param role user's role
   * @return constructed User entity
   */
  private User buildUser(InitProperties.UserConfig userConfig, Role role) {
    return User.builder()
        .userName(userConfig.getUsername())
        .password(passwordEncoder.encode(userConfig.getPassword()))
        .fullName(userConfig.getFullname())
        .email(userConfig.getEmail())
        .status(Status.ACTIVE)
        .roles(Set.of(role))
        .build();
  }

  /**
   * Internal record class for holding permission definition data.
   *
   * @param code the unique permission code
   * @param name the display name of the permission
   * @param description detailed description of what the permission allows
   */
  private record PermissionDefinition(String code, String name, String description) {}
}
