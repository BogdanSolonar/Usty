package com.planning.backend.dto.admin;

// Короткі лічильники по всій системі для верхнього блоку адмінки
//   activeCouples — пари, де обоє вже входили
//   loggedInUsers — користувачі, які входили хоча б раз; telegramUsers — прив'язали бота
public record AdminOverview(
        long couples,
        long activeCouples,
        long users,
        long loggedInUsers,
        long telegramUsers,
        long dates,
        long wishes,
        long dreams,
        long photos) {
}
