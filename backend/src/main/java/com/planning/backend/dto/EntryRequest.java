package com.planning.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Спільна форма для створення/редагування побачення, бажання чи мрії:
// назва, опис і фото (у побачення фото = обкладинка).
// Автора не передаємо — сервер бере його з токена.
public record EntryRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10000) String description,
        @Size(max = 2000) String imageUrl) {

    public String cleanTitle() {
        return title.trim();
    }

    public String cleanDescription() {
        return blankToNull(description);
    }

    public String cleanImageUrl() {
        return blankToNull(imageUrl);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
