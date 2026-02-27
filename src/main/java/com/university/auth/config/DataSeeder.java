package com.university.auth.config;

import com.university.auth.entity.Permission;
import com.university.auth.entity.Role;
import com.university.auth.enums.PermissionName;
import com.university.auth.enums.RoleName;
import com.university.auth.repository.PermissionRepository;
import com.university.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) {

        // ✅ create permissions
        Permission userView = createPermission(
                PermissionName.USER_VIEW.name(),
                "Can view user resources"
        );

        Permission adminDashboard = createPermission(
                PermissionName.ADMIN_DASHBOARD.name(),
                "Can access admin dashboard"
        );

        // ✅ create roles
        Role userRole = createRole(
                RoleName.USER.name(),
                "Default public user role"
        );

        Role adminRole = createRole(
                RoleName.ADMIN.name(),
                "System administrator role"
        );

        createRole(RoleName.STUDENT.name(), "Student role placeholder");
        createRole(RoleName.FACULTY.name(), "Faculty role placeholder");

        // 🔥 ENTERPRISE WAY — mutate existing collection
        userRole.getPermissions().clear();
        userRole.getPermissions().add(userView);

        adminRole.getPermissions().clear();
        adminRole.getPermissions().add(userView);
        adminRole.getPermissions().add(adminDashboard);

        roleRepository.save(userRole);
        roleRepository.save(adminRole);

        log.info("RBAC seed complete");
    }

    private Permission createPermission(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder()
                                .name(name)
                                .description(description)
                                .build()
                ));
    }

    private Role createRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(name)
                                .description(description)
                                .build()
                ));
    }
}