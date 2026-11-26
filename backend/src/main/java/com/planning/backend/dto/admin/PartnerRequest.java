package com.planning.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Один з пари у формі створення.
//   name          — ім'я для входу в інтерфейсі ("Андрій")
//   nameGenitive  — "чий?" для підписів "Вішліст Андрія" (порожньо — як name)
//   color         — кольоровий маркер "#3b82f6" (порожньо — синій для хлопця / рожевий для дівчини)
//   password      — свій пароль (порожньо — згенерується автоматично)
public record PartnerRequest(
        @NotBlank @Size(max = 40) String name,
        @Size(max = 40) String nameGenitive,
        @Pattern(regexp = "^(#[0-9a-fA-F]{6})?$") String color,
        @Size(max = 64) String password) {
}
