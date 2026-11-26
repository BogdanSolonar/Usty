package com.planning.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.planning.backend.repository.UserRepository;

// Остання активність оновлюється лише після успішних змін даних (не після переглядів і не після помилок)
class ActivityTrackerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final ActivityTracker tracker = new ActivityTracker(users);

    private void loginAs(long userId) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject(String.valueOf(userId))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void request(String method, int status) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        tracker.afterCompletion(new MockHttpServletRequest(method, "/api/dates"), response, null, null);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void successfulChangeUpdatesActivity() {
        loginAs(7);
        request("POST", 201);
        verify(users).touchActivity(eq(7L), any());
    }

    @Test
    void viewsAndErrorsDoNotCount() {
        loginAs(7);
        request("GET", 200);
        request("DELETE", 404);
        request("PUT", 403);
        verify(users, never()).touchActivity(any(), any());
    }

    @Test
    void anonymousRequestsAreIgnored() {
        request("POST", 200);
        verify(users, never()).touchActivity(any(), any());
    }
}
