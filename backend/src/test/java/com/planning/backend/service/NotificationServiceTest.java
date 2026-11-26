package com.planning.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.planning.backend.service.NotificationService.PlannedDate;

class NotificationServiceTest {

    @Test
    void messageContainsTitleDateNoteAndInitiator() {
        String text = NotificationService.messageText(
                new PlannedDate("Пікнік у парку", LocalDate.of(2026, 12, 5), "Взяти плед", "Марічка"));

        assertThat(text)
                .contains("Пікнік у парку")
                .contains("5 грудня 2026")
                .contains("Взяти плед")
                .contains("Марічка");
    }

    @Test
    void emptyNoteIsShownAsDash() {
        String text = NotificationService.messageText(new PlannedDate("Кіно", LocalDate.of(2026, 12, 5), null, "Бодя"));
        assertThat(text).contains("Примітка: —");
    }
}
