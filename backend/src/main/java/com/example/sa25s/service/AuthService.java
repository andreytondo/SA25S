package com.example.sa25s.service;

import com.example.sa25s.api.dto.*;
import com.example.sa25s.domain.User;
import com.example.sa25s.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;
    @Inject
    PasswordService passwordService;
    @Inject
    EncryptionService encryptionService;
    @Inject
    TotpService totpService;
    @Inject
    QrCodeService qrCodeService;
    @Inject
    JwtService jwtService;
    @Inject
    RateLimiterService rateLimiter;

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Transactional
    public StatusResponse register(RegisterRequest request) {
        Optional<User> existing = userRepository.findByEmail(request.email);
        if (existing.isPresent()) {
            return StatusResponse.of("error", "User already exists");
        }
        User user = new User();
        user.email = request.email.toLowerCase();
        user.passwordHash = passwordService.hash(request.password);
        userRepository.persist(user);
        LOG.infov("User registered: {0}", user.email);
        return StatusResponse.of("ok", "User registered");
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email);
        if (userOpt.isEmpty()) {
            return StatusResponse.of("error", "Invalid credentials").toLoginResponse();
        }
        User user = userOpt.get();
        if (!passwordService.matches(request.password, user.passwordHash)) {
            LOG.warnv("Invalid password for {0}", user.email);
            return StatusResponse.of("error", "Invalid credentials").toLoginResponse();
        }
        if (user.otpLockedUntil != null && user.otpLockedUntil.isAfter(Instant.now())) {
            return StatusResponse.of("locked", "Account locked after repeated OTP failures").toLoginResponse();
        }
        boolean has2faSecret = user.encryptedSecretKey != null;
        if (!has2faSecret) {
            String token = jwtService.generateAccessToken(user);
            return LoginResponse.immediateToken(token);
        }
        // 2FA secret present -> require OTP to either activate or login
        String nonce = UUID.randomUUID().toString();
        user.tempTokenNonce = nonce;
        user.tempTokenExpiresAt = Instant.now().plus(Duration.ofMinutes(5));
        String temp = jwtService.generateTemporaryToken(user, nonce);
        return LoginResponse.pending2fa(temp);
    }

    @Transactional
    public TwoFaSetupResponse setup2fa(String accessToken) {
        User user = getUserFromAccessToken(accessToken);
        if (user == null) {
            throw new IllegalArgumentException("Invalid token");
        }
        String secret = totpService.generateBase32Secret();
        String otpauth = totpService.buildOtpAuthUrl("SA25S", user.email, secret);
        String encrypted = encryptionService.encrypt(secret);
        user.encryptedSecretKey = encrypted;

        TwoFaSetupResponse response = new TwoFaSetupResponse();
        response.otpauthUrl = otpauth;
        response.secretBase32 = secret;
        response.qrCodeDataUri = qrCodeService.generateDataUri(otpauth);
        response.message = "Scan the QR in your Authenticator app";
        LOG.infov("2FA secret generated for {0}", user.email);
        return response;
    }

    @Transactional
    public LoginResponse verifyOtp(OtpVerifyRequest request) {
        JwtService.ParsedToken parsed = jwtService.parse(request.temporaryToken);
        if (parsed == null || !"temp".equals(parsed.tokenType())) {
            return StatusResponse.of("error", "Invalid temporary token").toLoginResponse();
        }
        User user = userRepository.findById(Long.valueOf(parsed.subject()));
        if (user == null || user.tempTokenExpiresAt == null || Instant.now().isAfter(user.tempTokenExpiresAt)) {
            return StatusResponse.of("error", "Temporary token expired").toLoginResponse();
        }
        if (parsed.nonce() == null || user.tempTokenNonce == null || !user.tempTokenNonce.equals(String.valueOf(parsed.nonce()))) {
            return StatusResponse.of("error", "Nonce mismatch").toLoginResponse();
        }
        if (!rateLimiter.isAllowed("otp-" + user.email)) {
            return StatusResponse.of("locked", "Too many attempts").toLoginResponse();
        }
        if (user.encryptedSecretKey == null) {
            return StatusResponse.of("error", "2FA not configured").toLoginResponse();
        }
        if (user.otpLockedUntil != null && user.otpLockedUntil.isAfter(Instant.now())) {
            return StatusResponse.of("locked", "Account locked for OTP").toLoginResponse();
        }
        String secret = encryptionService.decrypt(user.encryptedSecretKey);
        boolean valid = totpService.verifyCode(secret, request.otp);
        if (!valid) {
            user.failedOtpAttempts = user.failedOtpAttempts == null ? 1 : user.failedOtpAttempts + 1;
            if (user.failedOtpAttempts >= 5) {
                user.otpLockedUntil = Instant.now().plus(Duration.ofMinutes(5));
            }
            rateLimiter.recordFailure("otp-" + user.email);
            return StatusResponse.of("error", "Invalid OTP").toLoginResponse();
        }
        if (user.otpLockedUntil != null && user.otpLockedUntil.isAfter(Instant.now())) {
            return StatusResponse.of("locked", "Account locked for OTP").toLoginResponse();
        }
        user.failedOtpAttempts = 0;
        rateLimiter.reset("otp-" + user.email);
        user.twoFactorEnabled = true;
        user.tempTokenNonce = null;
        user.tempTokenExpiresAt = null;
        String token = jwtService.generateAccessToken(user);
        return LoginResponse.immediateToken(token);
    }

    @Transactional
    public StatusResponse disable2fa(String accessToken, Disable2faRequest request) {
        User user = getUserFromAccessToken(accessToken);
        if (user == null) {
            return StatusResponse.of("error", "Invalid token");
        }
        if (user.encryptedSecretKey == null) {
            return StatusResponse.of("ok", "2FA already disabled");
        }
        String secret = encryptionService.decrypt(user.encryptedSecretKey);
        boolean valid = totpService.verifyCode(secret, request.otp);
        if (!valid) {
            return StatusResponse.of("error", "OTP invalid for disable");
        }
        user.twoFactorEnabled = false;
        user.encryptedSecretKey = null;
        user.failedOtpAttempts = 0;
        LOG.infov("2FA disabled for {0}", user.email);
        return StatusResponse.of("ok", "2FA disabled");
    }

    private User getUserFromAccessToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring("Bearer ".length());
        JwtService.ParsedToken parsed = jwtService.parse(token);
        if (parsed == null || !"access".equals(parsed.tokenType())) return null;
        return userRepository.findById(Long.valueOf(parsed.subject()));
    }
}
