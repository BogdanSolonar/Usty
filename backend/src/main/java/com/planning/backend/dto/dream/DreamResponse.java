package com.planning.backend.dto.dream;

import java.time.Instant;

import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.Dream;

public record DreamResponse(
        Long id,
        String title,
        String description,
        String imageUrl,
        UserResponse createdBy,
        Instant createdAt,
        boolean achieved,
        Instant achievedAt) {

    public static DreamResponse from(Dream dream) {
        return new DreamResponse(dream.getId(), dream.getTitle(), dream.getDescription(), dream.getImageUrl(),
                UserResponse.from(dream.getCreatedBy()), dream.getCreatedAt(), dream.isAchieved(),
                dream.getAchievedAt());
    }
}
