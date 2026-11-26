package com.planning.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QueryCheckerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "пікнік у парку", "Вечеря при свічках", "сходим на квадроцикли", "кіно", "Мальдіви",
            "новий iPhone 16", "PlayStation 5", "квітковий магазин", "подорож в Японію", "собака",
            "з'їздити в Карпати", "ресторан", "beach sunset", "strawberry", "контрпропозиція",
            "пролісок", "морозиво", "кава", "п'ятниця"})
    void acceptsRealPhrases(String query) {
        assertTrue(QueryChecker.isMeaningful(query), query);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "  ", "ab", "ааааа", "фвфвфв", "ываываыва", "фывапролдж", "йцукен", "asdfgh",
            "qwerty", "zxcvbn", "dsfjkl", "вплрвплр", "ккккот", "!!!???", "123 456", "кoт",
            "мнгвпрст", "хзхзхз", "jjjjj", "@#$%"})
    void rejectsGibberish(String query) {
        assertFalse(QueryChecker.isMeaningful(query), query);
    }
}
