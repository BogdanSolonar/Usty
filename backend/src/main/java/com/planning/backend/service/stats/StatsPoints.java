package com.planning.backend.service.stats;

import java.util.List;

// Як нараховуються бали у статистиці. Усі три активності співмірні:
//   ідея побачення                       +10
//   ініційоване побачення                +20  (натиснув "Реалізувати": заплановане або вже реалізоване)
//   фото з побачення                     +5..+15 = 5 базових + бонус за якість (оцінка 1..100 / 10)
// Тобто одне фото — від половини до півтори ідеї, але ніколи не "переважує" побачення в рази.
public final class StatsPoints {

    public static final int IDEA = 10;
    public static final int INITIATIVE = 20;
    public static final int PHOTO_BASE = 5;
    public static final int PHOTO_MAX = 15;

    private StatsPoints() {
    }

    // 1..4 -> 5, 50 -> 10, 74 -> 12, 95..100 -> 15
    public static int photo(int qualityScore) {
        int score = Math.max(0, Math.min(100, qualityScore));
        return PHOTO_BASE + (int) Math.round(score / 10.0);
    }

    public static long photos(List<Integer> qualityScores) {
        return qualityScores.stream().mapToLong(StatsPoints::photo).sum();
    }
}
