package com.planning.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.planning.backend.dto.auth.LoginRequest;
import com.planning.backend.dto.auth.LoginResponse;
import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.service.AuthService;
import com.planning.backend.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

// Вхід за паролем і "хто я"
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    // POST /api/auth/login {"password": "..."} -> {"token": "...", "expiresAt": "...", "user": {...}}
    // Невірний пароль -> 401, забагато спроб -> 429
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request.password(), http.getRemoteAddr());
    }

    // GET /api/auth/me -> поточний користувач (потрібен токен)
    @GetMapping("/me")
    public UserResponse me() {
        return userService.me();
    }
}
