package com.planning.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class PasswordVaultTest {

    private static PasswordVault vault(int seed) {
        byte[] key = new byte[32];
        key[0] = (byte) seed;
        return new PasswordVault(new SecretKeySpec(key, "HmacSHA256"));
    }

    @Test
    void roundTrip() {
        PasswordVault vault = vault(1);
        assertThat(vault.decrypt(vault.encrypt("k7mq-x4tr-9pwa"))).isEqualTo("k7mq-x4tr-9pwa");
    }

    @Test
    void ciphertextIsRandomAndDoesNotContainPassword() {
        PasswordVault vault = vault(1);
        String a = vault.encrypt("secret-pass");
        String b = vault.encrypt("secret-pass");
        assertThat(a).isNotEqualTo(b).doesNotContain("secret-pass");
    }

    @Test
    void otherKeyCannotDecrypt() {
        assertThat(vault(2).decrypt(vault(1).encrypt("secret-pass"))).isNull();
        assertThat(vault(1).decrypt(null)).isNull();
        assertThat(vault(1).decrypt("not base64 !!")).isNull();
    }

    @Test
    void generatedSecretsHaveExpectedShape() {
        assertThat(RandomSecrets.password()).matches("[a-z2-9]{4}-[a-z2-9]{4}-[a-z2-9]{4}");
        assertThat(RandomSecrets.inviteToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(RandomSecrets.password()).isNotEqualTo(RandomSecrets.password());
    }
}
