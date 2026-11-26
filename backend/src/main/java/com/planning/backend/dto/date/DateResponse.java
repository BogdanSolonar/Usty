package com.planning.backend.dto.date;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.DateStatus;

// Побачення у відповідях API разом з авторами та фото
public record DateResponse(
        Long id,
        String title,
        String description,
        String coverImageUrl,
        DateStatus status,
        Instant createdAt,
        UserResponse createdBy,
        LocalDate plannedDate,
        String plannedNote,
        UserResponse initiatedBy,
        LocalDate completedAt,
        List<DatePhotoResponse> photos) {
}
