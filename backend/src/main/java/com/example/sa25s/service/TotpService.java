package com.example.sa25s.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.KeyGenerator;

@ApplicationScoped
public class TotpService {

    private final TimeBasedOneTimePasswordGenerator generator;
    private final Base32 base32 = new Base32();

    public TotpService() {
        this.generator = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30));
    }

    public String generateBase32Secret() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(generator.getAlgorithm());
            keyGen.init(160); // RFC 4226 recommends 160-bit key
            Key key = keyGen.generateKey();
            return base32.encodeToString(key.getEncoded());
        } catch (Exception e) {
            byte[] random = new byte[20];
            new SecureRandom().nextBytes(random);
            return base32.encodeToString(random);
        }
    }

    public boolean verifyCode(String base32Secret, String code) {
        try {
            byte[] keyBytes = base32.decode(base32Secret);
            Key key = new javax.crypto.spec.SecretKeySpec(keyBytes, generator.getAlgorithm());

            // Accept small window to tolerate clock skew
            Instant now = Instant.now();
            for (int i = -1; i <= 1; i++) {
                Instant step = now.plusSeconds(generator.getTimeStep().getSeconds() * i);
                int expected = generator.generateOneTimePassword(key, step);
                if (String.format("%06d", expected).equals(code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String buildOtpAuthUrl(String issuer, String email, String secret) {
        return "otpauth://totp/" + issuer + ":" + email + "?secret=" + secret + "&issuer=" + issuer + "&digits=6&period=30";
    }
}
