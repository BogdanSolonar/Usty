package com.planning.backend.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.planning.backend.entity.User;

// Видає JWT-токен після успішного входу.
// Токен = {хто (id користувача), коли виданий, коли спливає} + підпис сервера.
// Підробити чи продовжити його без секретного ключа неможливо — підпис не зійдеться.
@Component
public class JwtService {

    public record IssuedToken(String value, Instant expiresAt) {
    }

    private final JwtEncoder encoder;
    private final long lifetimeDays;

    public JwtService(JwtEncoder encoder, @Value("${jwt.lifetime-days}") long lifetimeDays) {
        this.encoder = encoder;
        this.lifetimeDays = lifetimeDays;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetimeDays, ChronoUnit.DAYS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("usty")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString()) // за цим id CurrentUser знайде користувача
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
