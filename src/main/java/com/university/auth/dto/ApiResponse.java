package com.university.auth.dto;

import lombok.Builder;

@Builder
public record ApiResponse<T>(boolean success, String message, T data) {
}
