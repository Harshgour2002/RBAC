package com.university.auth.controller;

import com.university.auth.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protected")
public class SampleProtectedController {

    @GetMapping("/admin-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> adminRoleOnly() {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Access granted by ADMIN role")
                .data("ADMIN_ROLE_OK")
                .build();
    }

    @GetMapping("/admin-dashboard")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD')")
    public ApiResponse<String> adminPermissionOnly() {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Access granted by ADMIN_DASHBOARD permission")
                .data("ADMIN_PERMISSION_OK")
                .build();
    }
}
