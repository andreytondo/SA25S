package com.example.sa25s.service;

import com.example.sa25s.domain.User;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JwtService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration TEMP_TOKEN_TTL = Duration.ofMinutes(5);

    @Inject
    JWTParser jwtParser;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwt
                .claims(Map.of(
                        "tokenType", "access",
                        "email", user.email,
                        "twoFactor", user.twoFactorEnabled))
                .issuer("https://example.com/issuer")
                .groups("User")
                .subject(String.valueOf(user.id))
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_TTL))
                .signWithSecret(secret());
    }

    public String generateTemporaryToken(User user, String nonce) {
        Instant now = Instant.now();
        return Jwt.claims(Map.of(
                        "tokenType", "temp",
                        "nonce", nonce,
                        "email", user.email))
                .subject(String.valueOf(user.id))
                .issuedAt(now)
                .expiresAt(now.plus(TEMP_TOKEN_TTL))
                .signWithSecret(secret());
    }

    public ParsedToken parse(String token) {
        try {
            var jwt = jwtParser.parse(token);
            return new ParsedToken(jwt.getSubject(), jwt.getClaim("tokenType"), jwt.getClaim("nonce"));
        } catch (Exception e) {
            return null;
        }
    }

    private String secret() {
        String fromEnv = System.getenv("JWT_SECRET");
        // HS256 requires >= 256-bit key; fallback dev key is 32+ chars.
        return (fromEnv == null || fromEnv.isBlank())
                ? "dev-super-secret-key-32bytes-min!"
                : fromEnv;
    }

    public record ParsedToken(String subject, String tokenType, Object nonce) {
        public UUID nonceAsUuid() {
            if (nonce == null) return null;
            return UUID.fromString(nonce.toString());
        }
    }
}
