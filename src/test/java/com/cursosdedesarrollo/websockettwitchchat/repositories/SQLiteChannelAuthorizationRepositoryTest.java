package com.cursosdedesarrollo.websockettwitchchat.repositories;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.security.TokenCipher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SQLiteChannelAuthorizationRepository.class, SQLiteChannelAuthorizationRepositoryTest.CredentialTestConfig.class})
class SQLiteChannelAuthorizationRepositoryTest {
    private static final Path DATABASE_PATH = createDatabasePath();

    @Autowired
    private SQLiteChannelAuthorizationRepository repository;

    @Autowired
    private ChannelAuthorizationJpaRepository jpaRepository;

    @Autowired
    private TokenCipher tokenCipher;

    @DynamicPropertySource
    static void configureSqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH);
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
    }

    @Test
    void persistsIndependentChannelsPerProviderAndUpdatesByStableIdentity() {
        Instant firstUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(authorization("twitch", "1001", "first_login", "first-access", "first-refresh", firstUpdatedAt));
        repository.save(authorization("youtube", "1001", "video_login", "video-access", "video-refresh", firstUpdatedAt));
        repository.save(authorization("twitch", "2002", "second_login", "second-access", "second-refresh", firstUpdatedAt));

        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        repository.save(authorization("twitch", "1001", "renamed_login", "rotated-access", "rotated-refresh", updatedAt));
        jpaRepository.flush();

        ChannelAuthorization updated = repository.findByProviderUserId("twitch", "1001").orElseThrow();
        assertEquals("renamed_login", updated.login());
        assertEquals("rotated-access", updated.accessToken());
        assertEquals("rotated-refresh", updated.refreshToken());
        assertEquals(updatedAt, updated.updatedAt());
        assertEquals("video_login", repository.findByProviderUserId("youtube", "1001").orElseThrow().login());
        assertEquals("second_login", repository.findByProviderUserId("twitch", "2002").orElseThrow().login());
        assertTrue(repository.findByProviderUserId("twitch", "3003").isEmpty());
        assertEquals(3, repository.findAll().size());
    }

    @Test
    void persistsOptionalDatesScopesAndStatus() {
        ChannelAuthorization value = new ChannelAuthorization(
                "twitch", "1001", "streamer", "access", "refresh", List.of("chat:read", "chat:edit"),
                null, ChannelAuthorization.Status.NEEDS_REAUTHORIZATION, null,
                Instant.parse("2026-01-03T00:00:00Z"));

        repository.save(value);
        jpaRepository.flush();

        assertEquals(value, repository.findByProviderUserId("twitch", "1001").orElseThrow());
    }

    @Test
    void storesEncryptedTokensInJpaRowsAndSqliteFile() throws Exception {
        repository.save(authorization("twitch", "1001", "streamer", "disk-access-secret", "disk-refresh-secret",
                Instant.parse("2026-01-04T00:00:00Z")));
        jpaRepository.flush();

        ChannelAuthorizationEntity stored = jpaRepository.findById(new ChannelAuthorizationKey("twitch", "1001"))
                .orElseThrow();
        assertFalse(stored.getEncryptedAccessToken().contains("disk-access-secret"));
        assertFalse(stored.getEncryptedRefreshToken().contains("disk-refresh-secret"));
        String fileContents = new String(Files.readAllBytes(DATABASE_PATH), StandardCharsets.ISO_8859_1);
        assertFalse(fileContents.contains("disk-access-secret"));
        assertFalse(fileContents.contains("disk-refresh-secret"));
        assertEquals("disk-access-secret", repository.findByProviderUserId("twitch", "1001").orElseThrow().accessToken());
    }

    @Test
    void failsBeforePersistingCredentialsWhenEncryptionKeyIsMissing() {
        SQLiteChannelAuthorizationRepository unconfigured =
                new SQLiteChannelAuthorizationRepository(jpaRepository, new TokenCipher(""));

        assertThrows(IllegalStateException.class,
                () -> unconfigured.save(authorization("twitch", "1001", "streamer", "access", "refresh",
                        Instant.parse("2026-01-06T00:00:00Z"))));
        assertTrue(jpaRepository.findById(new ChannelAuthorizationKey("twitch", "1001")).isEmpty());
    }

    private static ChannelAuthorization authorization(
            String provider, String providerUserId, String login, String accessToken, String refreshToken, Instant updatedAt) {
        return new ChannelAuthorization(
                provider, providerUserId, login, accessToken, refreshToken, List.of("chat:read", "chat:edit"),
                Instant.parse("2026-02-01T00:00:00Z"), ChannelAuthorization.Status.AUTHORIZED,
                Instant.parse("2026-01-01T00:00:00Z"), updatedAt);
    }

    private static Path createDatabasePath() {
        try {
            return Files.createTempDirectory("twitch-authorization-jpa-").resolve("credentials.sqlite");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @TestConfiguration
    static class CredentialTestConfig {
        @Bean
        TokenCipher tokenCipher() {
            return new TokenCipher(TokenCipher.generateKeyset());
        }
    }
}
