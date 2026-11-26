package com.planning.backend.security;

import java.security.SecureRandom;
import java.util.Base64;

// Випадкові паролі та коди запрошень (криптографічно стійкий генератор)
public final class RandomSecrets {

    // без схожих символів (0/o, 1/l/i), щоб пароль легко було переписати вручну
    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomSecrets() {
    }

    // "k7mq-x4tr-9pwa": 12 випадкових символів (~59 біт) у трьох групах
    public static String password() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    // Код для посилання t.me/<бот>?start=<код>: 32 байти -> 43 символи [A-Za-z0-9_-]
    // (Telegram дозволяє до 64 таких символів у параметрі start)
    public static String inviteToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
