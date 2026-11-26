package com.planning.backend.dto.admin;

import java.time.Instant;
import java.util.List;

// Пара в адмінці: двоє користувачів і чи можна її видалити (пару адміна — ні)
// botConfigured = false -> посилань немає, паролі треба передати вручну
public record CoupleView(Long id, Instant createdAt, List<MemberView> members, boolean deletable,
        boolean botConfigured) {
}
