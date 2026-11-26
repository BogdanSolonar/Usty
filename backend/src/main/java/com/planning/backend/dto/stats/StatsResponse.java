package com.planning.backend.dto.stats;

import java.util.List;

// Статистика пари: обоє з балами, три активності, хто попереду за сумою (null = порівну),
// різниця в балах і тепла підказка — ЛИШЕ для того, хто запитує (чужі підказки не віддаються)
public record StatsResponse(
        List<PlayerStats> players,
        List<StatsCategory> categories,
        Long leaderUserId,
        long gap,
        Motivation myMotivation) {
}
