package com.example.sa25s.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    public String email;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public boolean twoFactorEnabled = false;

    @Column(length = 512)
    public String encryptedSecretKey;

    public Integer failedOtpAttempts = 0;

    public Instant otpLockedUntil;

    @Column(length = 128)
    public String tempTokenNonce;

    public Instant tempTokenExpiresAt;

    public Instant createdAt = Instant.now();
}
