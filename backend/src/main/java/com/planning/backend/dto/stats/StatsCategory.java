package com.planning.backend.dto.stats;

import java.util.List;

// Одна активність (ідеї / ініціатива / фото): бали кожного і хто попереду (null = порівну)
public record StatsCategory(String key, String title, List<Score> scores, Long leaderUserId) {

    // points — бали за цю активність, label — що за ними стоїть ("3 ідеї")
    public record Score(Long userId, long points, String label) {
    }
}
