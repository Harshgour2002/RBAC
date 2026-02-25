package com.university.auth.config;

import com.university.auth.entity.Permission;
import com.university.auth.entity.Role;
import com.university.auth.enums.PermissionName;
import com.university.auth.enums.RoleName;
import com.university.auth.repository.PermissionRepository;
import com.university.auth.repository.RoleRepository;
import java.util.Set;
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
        Permission userView = createPermission(PermissionName.USER_VIEW.name(), "Can view user resources");
        Permission adminDashboard = createPermission(PermissionName.ADMIN_DASHBOARD.name(), "Can access admin dashboard");

        Role userRole = createRole(RoleName.USER.name(), "Default public user role");
        Role adminRole = createRole(RoleName.ADMIN.name(), "System administrator role");
        createRole(RoleName.STUDENT.name(), "Student role placeholder for future expansion");
        createRole(RoleName.FACULTY.name(), "Faculty role placeholder for future expansion");

        userRole.setPermissions(Set.of(userView));
        adminRole.setPermissions(Set.of(userView, adminDashboard));

        roleRepository.save(userRole);
        roleRepository.save(adminRole);
        log.info("RBAC seed complete");
    }

    private Permission createPermission(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .name(name)
                        .description(description)
                        .build()));
    }

    private Role createRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(name)
                        .description(description)
                        .build()));
    }
}
