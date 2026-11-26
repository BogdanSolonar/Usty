package com.planning.backend.dto.stats;

import com.planning.backend.dto.user.UserResponse;

// Показники й бали одного з пари (як рахуються бали — service/stats/StatsPoints)
public record PlayerStats(
        UserResponse user,
        long ideas,             // скільки ідей побачень додав
        long initiated,         // скільки побачень запланував / реалізував
        long photos,            // скільки фото завантажив
        int avgPhotoScore,      // середня оцінка якості фото (1..100)
        long ideaPoints,        // ідеї × 10
        long initiativePoints,  // побачення × 20
        long photoPoints,       // кожне фото 5..15 залежно від якості
        long totalPoints) {     // сума
}
