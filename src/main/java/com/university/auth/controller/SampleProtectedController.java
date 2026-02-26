package com.university.auth.controller;

import com.university.auth.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protected")
public class SampleProtectedController {
    
    @GetMapping("/user-endpoint")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<String> userEndpoint() {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Access granted by USER role")
                .data("USER_ENDPOINT_OK")
                .build();
    }

    @GetMapping("/user-browser-endpoint")
    @PreAuthorize("hasRole('USER') and !hasRole('ADMIN')")
    public String userBrowserEndpoint() {
        return "user point";
    }

    @GetMapping("/admin-endpoint")
    @PreAuthorize("hasRole('ADMIN')")
    // public ApiResponse<String> adminRoleOnly() {
     public ApiResponse<String> adminEndpoint() {
        return ApiResponse.<String>builder()
                .success(true)
                .message("Access granted by ADMIN role")
                .data("ADMIN_ROLE_OK")
                .data("ADMIN_ENDPOINT_OK")
                .build();
    }

    @GetMapping("/admin-browser-endpoint")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminBrowserEndpoint() {
        return "admin point";
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