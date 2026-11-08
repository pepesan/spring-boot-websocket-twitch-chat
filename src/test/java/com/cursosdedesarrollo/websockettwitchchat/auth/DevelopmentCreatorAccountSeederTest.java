package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevelopmentCreatorAccountSeederTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void seedsOneVerifiedAccountWithNormalizedEmailAndEncodedPassword() {
        CreatorAccountRepository accounts = mock(CreatorAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(accounts.findByEmail("streamer@example.test")).thenReturn(Optional.empty());
        when(encoder.encode("a-long-development-password")).thenReturn("argon2-hash");
        when(accounts.saveAndFlush(any(CreatorAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DevelopmentCreatorAccountSeeder seeder = new DevelopmentCreatorAccountSeeder(
                accounts, encoder, " Streamer@Example.Test ", "a-long-development-password", CLOCK);

        seeder.run(null);

        var accountCaptor = org.mockito.ArgumentCaptor.forClass(CreatorAccountEntity.class);
        verify(accounts).saveAndFlush(accountCaptor.capture());
        CreatorAccountEntity seeded = accountCaptor.getValue();
        assertEquals("streamer@example.test", seeded.getEmail());
        assertEquals("argon2-hash", seeded.getPasswordHash());
        assertTrue(seeded.isEmailVerified());
        assertEquals(CLOCK.millis(), seeded.getCreatedAt());
    }

    @Test
    void doesNotOverwriteAnExistingDevelopmentAccount() {
        CreatorAccountRepository accounts = mock(CreatorAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(accounts.findByEmail("streamer@example.test"))
                .thenReturn(Optional.of(new CreatorAccountEntity("streamer@example.test", "existing-hash", 1L)));
        DevelopmentCreatorAccountSeeder seeder = new DevelopmentCreatorAccountSeeder(
                accounts, encoder, "streamer@example.test", "a-long-development-password", CLOCK);

        seeder.run(null);

        verify(accounts, never()).saveAndFlush(any(CreatorAccountEntity.class));
        verify(encoder, never()).encode(any());
    }

    @Test
    void refusesToSeedWhenPasswordIsNotConfiguredStrongEnough() {
        CreatorAccountRepository accounts = mock(CreatorAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        DevelopmentCreatorAccountSeeder seeder = new DevelopmentCreatorAccountSeeder(
                accounts, encoder, "streamer@example.test", "short", CLOCK);

        seeder.run(null);

        verify(accounts, never()).findByEmail(any());
        verify(accounts, never()).saveAndFlush(any(CreatorAccountEntity.class));
        verify(encoder, never()).encode(any());
    }
}
