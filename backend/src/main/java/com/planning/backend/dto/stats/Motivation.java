package com.planning.backend.dto.stats;

// Тепла підказка у статистиці: кожному своя, і бачить її лише сам власник.
//   level    — START / TIE / LEAD / SMALL / MEDIUM / BIG (наскільки великий відрив)
//   headline — загальна мотивуюча фраза
//   action   — конкретний крок з реальними цифрами ("Додай ще 2 ідеї — і ти попереду"), може бути null
public record Motivation(Long userId, String level, String headline, String action) {
}
