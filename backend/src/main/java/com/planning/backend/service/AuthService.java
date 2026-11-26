package com.planning.backend.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.dto.auth.LoginResponse;
import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.User;
import com.planning.backend.repository.UserRepository;
import com.planning.backend.security.JwtService;
import com.planning.backend.security.JwtService.IssuedToken;
import com.planning.backend.security.LoginAttemptLimiter;

// Вхід без імені — лише за паролем.
// Сервер по черзі звіряє пароль з BCrypt-хешем кожного користувача: чий хеш збігся — той і увійшов.
// Тому паролі в усіх різні (адмінка це перевіряє при створенні пари).
// Перший успішний вхід означає, що людина вже має пароль, — його зашифрована копія більше не потрібна.
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptLimiter limiter;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
            LoginAttemptLimiter limiter) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.limiter = limiter;
    }

    @Transactional
    public LoginResponse login(String password, String clientAddress) {
        if (limiter.isBlocked(clientAddress)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts, try again later");
        }
        for (User user : users.findAllByOrderByIdAsc()) {
            // matches() хешує введений пароль з "сіллю" з хешу в базі і порівнює результат
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                limiter.recordSuccess(clientAddress);
                user.setPendingPassword(null);
                user.recordLogin(Instant.now()); // для статистики в адмінці
                IssuedToken token = jwtService.issue(user);
                return new LoginResponse(token.value(), token.expiresAt(), UserResponse.from(user));
            }
        }
        limiter.recordFailure(clientAddress);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password");
    }
}
