package com.planning.backend.dto.date;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

// "Реалізовано": коли побачення відбулося
public record CompleteRequest(@NotNull LocalDate completedAt) {
}
