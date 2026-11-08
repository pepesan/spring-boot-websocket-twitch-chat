package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountRepository;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountTokenEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreatorAccountServiceTest {
    private static final long NOW = 1_800_000_000_000L;
    private CreatorAccountRepository accounts;
    private CreatorAccountTokenRepository tokens;
    private JavaMailSender mail;
    private CreatorAccountService service;

    @BeforeEach void setup() {
        accounts=mock(CreatorAccountRepository.class); tokens=mock(CreatorAccountTokenRepository.class);
        mail=mock(JavaMailSender.class);
        PasswordEncoder encoder=mock(PasswordEncoder.class); when(encoder.encode(any())).thenReturn("argon2-hash");
        service=new CreatorAccountService(accounts,tokens,encoder,mail,"https://example.test",Duration.ofHours(24),
                Duration.ofHours(1),Clock.fixed(Instant.ofEpochMilli(NOW),ZoneOffset.UTC));
    }

    @Test void registrationNormalizesEmailAndPersistsOnlyTokenHash() throws Exception {
        CreatorAccountEntity account=account(1);
        when(accounts.findByEmail("person@example.test")).thenReturn(Optional.empty());
        when(accounts.saveAndFlush(any())).thenReturn(account);
        service.register(" Person@Example.Test ","long secure password");
        verify(accounts).saveAndFlush(argThat(a -> a.getEmail().equals("person@example.test")
                && a.getPasswordHash().equals("argon2-hash") && !a.isEmailVerified()));
        var captor=org.mockito.ArgumentCaptor.forClass(CreatorAccountTokenEntity.class);
        verify(tokens).save(captor.capture());
        CreatorAccountTokenEntity stored=captor.getValue();
        assertEquals("VERIFY_EMAIL",stored.getPurpose());
        assertEquals(NOW+Duration.ofHours(24).toMillis(),stored.getExpiresAt());
        var mailCaptor=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(mailCaptor.capture());
        SimpleMailMessage sent=mailCaptor.getValue();
        assertNotNull(sent);
        assertFalse(sent.getText().contains(stored.getTokenHash()));
        String raw=sent.getText().substring(sent.getText().indexOf("token=")+6);
        assertEquals(hash(raw),stored.getTokenHash());
        assertNotEquals(raw,stored.getTokenHash());
    }

    @Test void duplicateNormalizedEmailDoesNotCreateAnotherAccountOrSendMail() {
        when(accounts.findByEmail("person@example.test")).thenReturn(Optional.of(account(1)));
        service.register("PERSON@EXAMPLE.TEST","long secure password");
        verify(accounts,never()).saveAndFlush(any());
        verify(tokens,never()).save(any());
        verify(mail,never()).send(any(SimpleMailMessage.class));
    }

    @Test void expiredTokenIsRejectedAndDeleted() {
        String raw="raw-verification-token";
        CreatorAccountTokenEntity token=new CreatorAccountTokenEntity(1,hash(raw),"VERIFY_EMAIL",NOW,NOW-1);
        when(tokens.findByTokenHashAndPurpose(hash(raw),"VERIFY_EMAIL")).thenReturn(Optional.of(token));
        assertFalse(service.verifyEmail(raw));
        verify(tokens).deleteToken(token.getId());
        verify(accounts,never()).findById(any());
    }

    @Test void successfulVerificationConsumesTokenAndReuseFails() {
        String raw="raw-verification-token";
        CreatorAccountTokenEntity token=new CreatorAccountTokenEntity(1,hash(raw),"VERIFY_EMAIL",NOW+1,NOW);
        when(tokens.findByTokenHashAndPurpose(hash(raw),"VERIFY_EMAIL")).thenReturn(Optional.of(token),Optional.empty());
        CreatorAccountEntity account=account(1);
        when(accounts.findById(1)).thenReturn(Optional.of(account));
        assertTrue(service.verifyEmail(raw));
        assertFalse(service.verifyEmail(raw));
        verify(accounts).findById(1);
        verify(tokens).deleteToken(token.getId());
    }

    @Test void passwordResetChangesPasswordAndConsumesResetToken() {
        String raw="raw-reset-token";
        CreatorAccountTokenEntity token=new CreatorAccountTokenEntity(1,hash(raw),"RESET_PASSWORD",NOW+1,NOW);
        when(tokens.findByTokenHashAndPurpose(hash(raw),"RESET_PASSWORD")).thenReturn(Optional.of(token));
        CreatorAccountEntity account=account(1);
        when(accounts.findById(1)).thenReturn(Optional.of(account));
        assertTrue(service.resetPassword(raw,"a different long password"));
        assertEquals("argon2-hash",account.getPasswordHash());
        verify(tokens).deleteForAccountAndPurpose(1,"RESET_PASSWORD");
    }
    private static CreatorAccountEntity account(int id) {
        CreatorAccountEntity entity=new CreatorAccountEntity("person@example.test","argon2-hash",NOW);
        try { Field idField=CreatorAccountEntity.class.getDeclaredField("id"); idField.setAccessible(true); idField.set(entity,id); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        return entity;
    }
    private static String hash(String token) {
        try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new AssertionError(e); }
    }
}
