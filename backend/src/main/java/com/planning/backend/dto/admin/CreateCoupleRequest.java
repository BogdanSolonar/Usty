package com.planning.backend.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// POST /api/admin/couples: хлопець і дівчина нової пари
public record CreateCoupleRequest(@NotNull @Valid PartnerRequest he, @NotNull @Valid PartnerRequest she) {
}
