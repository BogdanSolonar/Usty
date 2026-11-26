package com.planning.backend.service;

import java.util.regex.Pattern;

// Перший фільтр перед пошуком фото: відсіює очевидно беззмістовні запити
// ("ааааа", "фвфвфв", "zxqwrt", "!!!???"), щоб не витрачати запити до Pixabay
// і не показувати випадкові картинки.
public final class QueryChecker {

    // Голосні українською, російською та англійською
    private static final String VOWELS = "аеєиіїоуюяыэёaeiouy";

    // Дозволені символи: літери, цифри, пробіл, апостроф, дефіс
    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\d\\s'’ʼ-]+");
    private static final Pattern CYRILLIC = Pattern.compile("\\p{IsCyrillic}");
    private static final Pattern LATIN = Pattern.compile("\\p{IsLatin}");
    // Один і той самий символ 3+ рази поспіль: "ааа", "ммм"
    private static final Pattern TRIPLE_CHAR = Pattern.compile("(\\p{L})\\1\\1");
    // Той самий склад/шматок, повторений 3+ рази поспіль: "фвфвфв", "абвабвабв"
    private static final Pattern REPEATED_CHUNK = Pattern.compile("(\\p{L}{2,3})\\1\\1");

    // Ряди клавіатури (укр., рос., англ.). 5 клавіш поспіль з одного ряду = "удар по клавіатурі"
    private static final String[] KEYBOARD_ROWS = {
            "йцукенгшщзхї", "фівапролджє", "ячсмитьбю",
            "йцукенгшщзхъ", "фывапролджэ", "ячсмитьбю",
            "qwertyuiop", "asdfghjkl", "zxcvbnm"};
    private static final int KEYBOARD_RUN = 5;

    private QueryChecker() {
    }

    public static boolean isMeaningful(String query) {
        if (query == null) {
            return false;
        }
        String text = query.trim().toLowerCase();
        if (text.length() < 3 || !ALLOWED.matcher(text).matches()) {
            return false;
        }
        boolean hasRealWord = false;
        for (String word : text.split("[\\s-]+")) {
            String letters = word.replaceAll("[^\\p{L}]", "");
            if (letters.isEmpty()) {
                continue; // числа на кшталт "2026" не роблять запит беззмістовним
            }
            if (!isPlausibleWord(letters)) {
                return false;
            }
            if (letters.length() >= 3) {
                hasRealWord = true;
            }
        }
        return hasRealWord;
    }

    private static boolean isPlausibleWord(String word) {
        // Кирилиця й латиниця в одному слові: "кoт" з латинською "o", "фыdf"
        if (CYRILLIC.matcher(word).find() && LATIN.matcher(word).find()) {
            return false;
        }
        if (TRIPLE_CHAR.matcher(word).find() || REPEATED_CHUNK.matcher(word).find()) {
            return false;
        }
        if (hasKeyboardRun(word)) {
            return false;
        }
        int vowels = 0;
        int consonantsInRow = 0;
        for (char c : word.toCharArray()) {
            if (VOWELS.indexOf(c) >= 0) {
                vowels++;
                consonantsInRow = 0;
            } else if (++consonantsInRow >= 6) {
                return false; // 6 приголосних поспіль у живих словах не трапляється
                              // (а 5 буває: "коНТРПРопозиція")
            }
        }
        if (word.length() <= 2) {
            return true; // короткі слова: "у", "в", "на", "і"
        }
        // У звичайних словах голосні становлять приблизно 25–65% літер
        double vowelShare = (double) vowels / word.length();
        return vowelShare >= 0.2 && vowelShare <= 0.75;
    }

    // Чи є у слові 5 літер, що йдуть поспіль на клавіатурі (в будь-який бік)
    private static boolean hasKeyboardRun(String word) {
        for (int i = 0; i + KEYBOARD_RUN <= word.length(); i++) {
            String piece = word.substring(i, i + KEYBOARD_RUN);
            String reversed = new StringBuilder(piece).reverse().toString();
            for (String row : KEYBOARD_ROWS) {
                if (row.contains(piece) || row.contains(reversed)) {
                    return true;
                }
            }
        }
        return false;
    }
}
