package com.planning.backend.dto.admin;

import java.time.Instant;

import com.planning.backend.entity.PartnerRole;

// Користувач в адмінці.
//   telegramLinked  — людина натиснула /start у боті (є chat_id)
//   inviteLink      — посилання на бота з її кодом (null — код уже використано або бот не налаштований)
//   pendingPassword — пароль, ще не доставлений власнику (null — вже доставлено або людина вже входила)
//   everLoggedIn    — чи входила людина хоча б раз (або щось створювала до появи цієї статистики)
//   lastLoginAt     — останній вхід; lastActivityAt — остання дія (вхід або будь-яка зміна даних)
//   created         — скільки побачень, бажань, мрій і фото створила саме ця людина
public record MemberView(
        Long id,
        String name,
        String nameGenitive,
        PartnerRole role,
        String color,
        String avatarUrl,
        boolean admin,
        boolean telegramLinked,
        String inviteLink,
        String pendingPassword,
        Instant createdAt,
        boolean everLoggedIn,
        Instant lastLoginAt,
        Instant lastActivityAt,
        MemberCounts created) {
}
