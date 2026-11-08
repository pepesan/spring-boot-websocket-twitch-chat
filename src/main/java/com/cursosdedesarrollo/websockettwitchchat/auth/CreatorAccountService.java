package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountRepository;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountTokenEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
public class CreatorAccountService {
    private static final String VERIFY = "VERIFY_EMAIL", RESET = "RESET_PASSWORD";
    private final CreatorAccountRepository accounts;
    private final CreatorAccountTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final Clock clock;
    private final String publicUrl;
    private final Duration verificationTtl, resetTtl;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public CreatorAccountService(CreatorAccountRepository accounts, CreatorAccountTokenRepository tokens,
            PasswordEncoder passwordEncoder, JavaMailSender mailSender,
            @Value("${creator.accounts.public-url:http://localhost:8080}") String publicUrl,
            @Value("${creator.accounts.verification-token-ttl:PT24H}") Duration verificationTtl,
            @Value("${creator.accounts.password-reset-token-ttl:PT1H}") Duration resetTtl) {
        this(accounts, tokens, passwordEncoder, mailSender, publicUrl, verificationTtl, resetTtl, Clock.systemUTC());
    }
    CreatorAccountService(CreatorAccountRepository accounts, CreatorAccountTokenRepository tokens,
            PasswordEncoder passwordEncoder, JavaMailSender mailSender, String publicUrl,
            Duration verificationTtl, Duration resetTtl, Clock clock) {
        this.accounts=accounts; this.tokens=tokens; this.passwordEncoder=passwordEncoder; this.mailSender=mailSender;
        this.publicUrl=publicUrl.replaceAll("/+$", ""); this.verificationTtl=verificationTtl; this.resetTtl=resetTtl; this.clock=clock;
    }


    @Transactional(readOnly = true)
    public Integer requireVerifiedAccountId(String email) {
        return accounts.findByEmail(normalize(email))
                .filter(CreatorAccountEntity::isEmailVerified)
                .map(CreatorAccountEntity::getId)
                .orElseThrow(() -> new IllegalArgumentException("A verified creator account is required"));
    }
    @Transactional
    public void register(String email, String password) {
        String normalized = normalize(email);
        if (accounts.findByEmail(normalized).isPresent()) return;
        CreatorAccountEntity account;
        try { account = accounts.saveAndFlush(new CreatorAccountEntity(normalized, passwordEncoder.encode(password), clock.millis())); }
        catch (DataIntegrityViolationException race) { return; }
        sendToken(account, VERIFY, verificationTtl, "verify-email");
    }

    @Transactional
    public boolean verifyEmail(String rawToken) {
        Optional<CreatorAccountTokenEntity> found = findValid(rawToken, VERIFY);
        if (found.isEmpty()) return false;
        CreatorAccountTokenEntity token=found.get();
        accounts.findById(token.getAccountId()).ifPresent(account -> account.setEmailVerified(true));
        tokens.deleteToken(token.getId()); return true;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        accounts.findByEmail(normalize(email)).filter(CreatorAccountEntity::isEmailVerified)
                .ifPresent(account -> sendToken(account, RESET, resetTtl, "reset-password"));
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        Optional<CreatorAccountTokenEntity> found=findValid(rawToken, RESET);
        if (found.isEmpty()) return false;
        CreatorAccountTokenEntity token=found.get();
        Optional<CreatorAccountEntity> account=accounts.findById(token.getAccountId());
        if (account.isEmpty()) return false;
        account.get().setPasswordHash(passwordEncoder.encode(newPassword));
        tokens.deleteForAccountAndPurpose(account.get().getId(), RESET); return true;
    }

    private void sendToken(CreatorAccountEntity account, String purpose, Duration ttl, String route) {
        tokens.deleteForAccountAndPurpose(account.getId(), purpose);
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new CreatorAccountTokenEntity(account.getId(), hash(raw), purpose,
                clock.millis()+ttl.toMillis(), clock.millis()));
        SimpleMailMessage message=new SimpleMailMessage(); message.setTo(account.getEmail());
        message.setSubject(purpose.equals(VERIFY) ? "Verify your email" : "Reset your password");
        message.setText(publicUrl+"/account/"+route+"?token="+raw); mailSender.send(message);
    }
    private Optional<CreatorAccountTokenEntity> findValid(String raw, String purpose) {
        if (raw == null || raw.length() > 200) return Optional.empty();
        Optional<CreatorAccountTokenEntity> candidate=tokens.findByTokenHashAndPurpose(hash(raw), purpose);
        if (candidate.isPresent() && candidate.get().getExpiresAt() <= clock.millis()) {
            tokens.deleteToken(candidate.get().getId()); return Optional.empty();
        }
        return candidate;
    }
    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static String hash(String token) {
        try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
