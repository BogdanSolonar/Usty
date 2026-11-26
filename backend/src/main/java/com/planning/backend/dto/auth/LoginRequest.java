package com.planning.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Вхід лише за паролем: {"password": "..."}
public record LoginRequest(@NotBlank @Size(max = 200) String password) {
}
