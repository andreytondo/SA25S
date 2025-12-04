package com.example.sa25s.service;

import com.example.sa25s.domain.User;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class JwtService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration TEMP_TOKEN_TTL = Duration.ofMinutes(5);
    private static final String ISSUER = "https://example.com/issuer";

    @Inject
    JWTParser jwtParser;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwt.issuer(ISSUER)
                .subject(String.valueOf(user.id))
                .upn(user.email)
                .groups(Set.of("User"))
                .claim("tokenType", "access")
                .claim("email", user.email)
                .claim("twoFactor", user.twoFactorEnabled)
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_TTL))
                .sign();
    }

    public String generateTemporaryToken(User user, String nonce) {
        Instant now = Instant.now();
        return Jwt.issuer(ISSUER)
                .subject(String.valueOf(user.id))
                .upn(user.email)
                .claim("tokenType", "temp")
                .claim("nonce", nonce)
                .claim("email", user.email)
                .issuedAt(now)
                .expiresAt(now.plus(TEMP_TOKEN_TTL))
                .sign();
    }

    public ParsedToken parse(String token) {
        try {
            var jwt = jwtParser.parse(token);
            return new ParsedToken(jwt.getSubject(), jwt.getClaim("tokenType"), jwt.getClaim("nonce"));
        } catch (Exception e) {
            return null;
        }
    }

    public record ParsedToken(String subject, String tokenType, Object nonce) {
        public UUID nonceAsUuid() {
            if (nonce == null) return null;
            return UUID.fromString(nonce.toString());
        }
    }
}
