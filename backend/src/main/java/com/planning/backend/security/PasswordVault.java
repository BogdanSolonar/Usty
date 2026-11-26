package com.planning.backend.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

// Тимчасове зашифроване сховище пароля, який ще НЕ доставлено власнику.
//
// Для входу в базі завжди лише BCrypt-хеш. Але бот має надіслати людині сам пароль,
// а адмін — бачити його як запасний варіант. Тому до моменту доставки пароль лежить
// у users.pending_password, зашифрований AES-256-GCM. Ключ виводиться з jwt.secret
// (secrets.properties) і в базі не зберігається: сама база без ключа пароль не розкриє.
// Щойно бот надіслав пароль або людина вперше увійшла — запис стирається.
@Component
public class PasswordVault {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public PasswordVault(SecretKey jwtSigningKey) {
        try {
            // окремий ключ для шифрування, похідний від ключа підпису токенів
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update("usty-password-vault".getBytes(StandardCharsets.UTF_8));
            sha.update(jwtSigningKey.getEncoded());
            this.key = new SecretKeySpec(sha.digest(), "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String encrypt(String password) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    // null — якщо розшифрувати не вдалося (наприклад, jwt.secret змінили)
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            return new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return null;
        }
    }
}
