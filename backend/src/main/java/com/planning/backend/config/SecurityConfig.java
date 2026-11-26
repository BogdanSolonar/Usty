package com.planning.backend.config;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

// Хто куди має доступ.
//   - POST /api/auth/login — відкритий (тут отримують токен)
//   - /uploads/** — відкриті (тег <img> не вміє надсилати токен; імена файлів випадкові й не вгадуються)
//   - решта /api/** — лише з заголовком "Authorization: Bearer <токен>"
// Сесій і cookie немає: кожен запит перевіряється за підписом токена (тому CSRF-захист не потрібен).
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/uploads/**", "/error").permitAll()
                        .anyRequest().authenticated())
                // Spring сам перевіряє підпис і термін дії токена за допомогою jwtDecoder() нижче
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // Хешування паролів: BCrypt (повільний за задумом, із "сіллю" — однакові паролі дають різні хеші)
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Секретний ключ, яким сервер підписує токени (HMAC-SHA256). Береться з secrets.properties.
    @Bean
    SecretKey jwtSigningKey(@Value("${jwt.secret}") String secret) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            // Без ключа все працює, але після кожного перезапуску бекенду доведеться входити знову
            log.warn("jwt.secret is not set in secrets.properties: using a random key (tokens reset on restart)");
            bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(secret.trim());
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes (base64)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
