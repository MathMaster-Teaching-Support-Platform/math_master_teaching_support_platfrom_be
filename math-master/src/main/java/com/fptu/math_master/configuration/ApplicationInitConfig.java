package com.fptu.math_master.configuration;

import java.util.HashSet;
import java.util.Set;

import com.fptu.math_master.constant.PredefinedPermission;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.entity.Permission;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.Status;
import com.fptu.math_master.repository.PermissionRepository;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;
    UserRepository userRepository;
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;

    @NonFinal
    static final String ADMIN_USER_NAME = "admin";

    @NonFinal
    static final String ADMIN_PASSWORD = "admin";

    @NonFinal
    static final String REGULAR_USER_NAME = "user";

    @NonFinal
    static final String REGULAR_USER_PASSWORD = "user";

    @Bean
    @ConditionalOnProperty(
            prefix = "spring.datasource",
            value = "driver-class-name",
            havingValue = "org.postgresql.Driver")
    ApplicationRunner applicationRunner(ApplicationInitConfig self) {
        return args -> {
            log.info("Initializing application.....");
            self.initializeData();
            log.info("Application initialization completed .....");
        };
    }

    @Transactional
    public void initializeData() {
        // Step 1: Create Permissions
        Set<Permission> adminPermissions = createPermissions(permissionRepository);
        Set<Permission> userPermissions = createUserPermissions(permissionRepository);

        // Step 2: Create Roles with Permissions
        Role userRole = createUserRole(roleRepository, userPermissions);
        Role adminRole = createAdminRole(roleRepository, adminPermissions);

            // Step 3: Create Admin User
            if (userRepository.findByUserName(ADMIN_USER_NAME).isEmpty()) {
                Set<Role> adminRoles = new HashSet<>();
                adminRoles.add(adminRole);

                User adminUser = User.builder()
                        .userName(ADMIN_USER_NAME)
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .fullName("System Administrator")
                        .email("admin@mathmaster.com")
                        .status(Status.ACTIVE)
                        .roles(adminRoles)
                        .build();

                userRepository.save(adminUser);
                log.warn("Admin user has been created with username: {} and default password: {}, please change it",
                        ADMIN_USER_NAME, ADMIN_PASSWORD);
            }

            // Step 4: Create Regular User
            if (userRepository.findByUserName(REGULAR_USER_NAME).isEmpty()) {
                Set<Role> userRoles = new HashSet<>();
                userRoles.add(userRole);

                User regularUser = User.builder()
                        .userName(REGULAR_USER_NAME)
                        .password(passwordEncoder.encode(REGULAR_USER_PASSWORD))
                        .fullName("Regular User")
                        .email("user@mathmaster.com")
                        .status(Status.ACTIVE)
                        .roles(userRoles)
                        .build();

                userRepository.save(regularUser);
                log.warn("Regular user has been created with username: {} and default password: {}, please change it",
                        REGULAR_USER_NAME, REGULAR_USER_PASSWORD);
            }
    }

    private Set<Permission> createPermissions(PermissionRepository permissionRepository) {
        Set<Permission> permissions = new HashSet<>();

        // User permissions
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.USER_READ, "Read User", "Permission to view user information"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.USER_CREATE, "Create User", "Permission to create new users"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.USER_UPDATE, "Update User", "Permission to update user information"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.USER_DELETE, "Delete User", "Permission to delete users"));

        // Role permissions
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.ROLE_READ, "Read Role", "Permission to view roles"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.ROLE_CREATE, "Create Role", "Permission to create new roles"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.ROLE_UPDATE, "Update Role", "Permission to update roles"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.ROLE_DELETE, "Delete Role", "Permission to delete roles"));

        // Permission permissions
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.PERMISSION_READ, "Read Permission", "Permission to view permissions"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.PERMISSION_CREATE, "Create Permission", "Permission to create new permissions"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.PERMISSION_UPDATE, "Update Permission", "Permission to update permissions"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.PERMISSION_DELETE, "Delete Permission", "Permission to delete permissions"));

        // Quiz permissions
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.QUIZ_READ, "Read Quiz", "Permission to view quizzes"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.QUIZ_CREATE, "Create Quiz", "Permission to create new quizzes"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.QUIZ_UPDATE, "Update Quiz", "Permission to update quizzes"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.QUIZ_DELETE, "Delete Quiz", "Permission to delete quizzes"));
        permissions.add(createPermissionIfNotExists(permissionRepository,
                PredefinedPermission.QUIZ_SUBMIT, "Submit Quiz", "Permission to submit quiz answers"));

        return permissions;
    }

    private Set<Permission> createUserPermissions(PermissionRepository permissionRepository) {
        Set<Permission> permissions = new HashSet<>();

        // Regular users can only read their own info and submit quizzes
        permissions.add(permissionRepository.findByCode(PredefinedPermission.USER_READ).orElseThrow());
        permissions.add(permissionRepository.findByCode(PredefinedPermission.QUIZ_READ).orElseThrow());
        permissions.add(permissionRepository.findByCode(PredefinedPermission.QUIZ_SUBMIT).orElseThrow());

        return permissions;
    }

    private Permission createPermissionIfNotExists(PermissionRepository permissionRepository,
                                                   String code, String name, String description) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code(code)
                        .name(name)
                        .description(description)
                        .build()));
    }

    private Role createUserRole(RoleRepository roleRepository,
                                Set<Permission> permissions) {
        return roleRepository.findByName(PredefinedRole.USER_ROLE)
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name(PredefinedRole.USER_ROLE)
                            .permissions(permissions)
                            .build();
                    return roleRepository.save(role);
                });
    }

    private Role createAdminRole(RoleRepository roleRepository,
                                 Set<Permission> permissions) {
        return roleRepository.findByName(PredefinedRole.ADMIN_ROLE)
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name(PredefinedRole.ADMIN_ROLE)
                            .permissions(permissions)
                            .build();
                    return roleRepository.save(role);
                });
    }
}