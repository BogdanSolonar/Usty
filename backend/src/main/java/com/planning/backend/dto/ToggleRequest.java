package com.planning.backend.dto;

import jakarta.validation.constraints.NotNull;

// Позначка "виконано" для бажання / "здійснено" для мрії: {"value": true}
public record ToggleRequest(@NotNull Boolean value) {
}
