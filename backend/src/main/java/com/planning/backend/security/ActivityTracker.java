package com.planning.backend.security;

import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.planning.backend.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// "Коли людина востаннє щось робила" — для статистики в адмінці.
//
// Після КОЖНОГО успішного запиту, що змінює дані (POST / PUT / DELETE на /api/**:
// побачення, план, "реалізовано", фото, бажання, мрії, аватарка...), записуємо users.last_activity_at = зараз.
// Одне місце замість правок у кожному сервісі; перегляд (GET) активністю не вважається.
// Вхід оновлює last_login_at і last_activity_at окремо — в AuthService.
@Component
public class ActivityTracker implements HandlerInterceptor {

    private final UserRepository users;

    public ActivityTracker(UserRepository users) {
        this.users = users;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        if ("GET".equals(request.getMethod()) || ex != null || response.getStatus() >= 400) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            users.touchActivity(Long.parseLong(jwt.getSubject()), Instant.now());
        }
    }
}
