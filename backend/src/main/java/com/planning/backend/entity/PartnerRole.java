package com.planning.backend.entity;

// Роль у парі: хлопець чи дівчина. Від неї залежать:
//   - колір-маркер за замовчуванням (синій / рожевий)
//   - рід дієслів в інтерфейсі ("запланував" / "запланувала")
public enum PartnerRole {
    HE("#3b82f6"),
    SHE("#ec4899");

    private final String defaultColor;

    PartnerRole(String defaultColor) {
        this.defaultColor = defaultColor;
    }

    public String getDefaultColor() {
        return defaultColor;
    }
}
