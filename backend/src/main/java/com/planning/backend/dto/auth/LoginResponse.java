package com.planning.backend.dto.auth;

import java.time.Instant;

import com.planning.backend.dto.user.UserResponse;

// Відповідь після входу: токен (фронтенд надсилає його в заголовку Authorization),
// коли він спливає, і хто саме увійшов
public record LoginResponse(String token, Instant expiresAt, UserResponse user) {
}
