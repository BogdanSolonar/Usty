package com.planning.backend.dto.date;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// "Реалізувати": обрана в календарі дата + примітка
public record PlanRequest(@NotNull LocalDate plannedDate, @Size(max = 2000) String note) {
}
