package com.planning.backend.dto.date;

import java.time.Instant;

import com.planning.backend.dto.user.UserResponse;

// Одне фото з побачення разом з оцінкою якості
public record DatePhotoResponse(
        Long id,
        String photoUrl,
        UserResponse uploadedBy,
        Instant uploadedAt,
        int qualityScore,
        String qualityVerdict,
        QualityBreakdown qualityBreakdown) {
}
