package com.planning.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.planning.backend.entity.User;
import com.planning.backend.repository.UserRepository;

// "Хто зараз робить запит?" — дістає id з перевіреного токена і повертає користувача з бази.
// Сервіси беруть звідси автора (created_by, owner, uploaded_by) і пару (couple_id),
// тож кожен запит автоматично обмежений даними своєї пари.
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        long id = Long.parseLong(jwt.getSubject());
        // користувача могли видалити разом із парою — тоді старий токен більше не діє
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public Long coupleId() {
        return get().getCoupleId();
    }

    // Адмінка: прапорець is_admin перевіряється в базі при КОЖНОМУ запиті (а не береться з токена)
    public User requireAdmin() {
        User me = get();
        if (!me.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins only");
        }
        return me;
    }
}
