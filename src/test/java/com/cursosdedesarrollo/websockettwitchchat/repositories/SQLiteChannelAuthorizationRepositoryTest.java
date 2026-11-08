package com.cursosdedesarrollo.websockettwitchchat.repositories;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.services.TokenCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteChannelAuthorizationRepositoryTest {
    @TempDir
    Path tempDir;

    private Path databasePath;
    private TokenCipher tokenCipher;
    private SQLiteChannelAuthorizationRepository repository;

    @BeforeEach
    void setUp() {
        databasePath = tempDir.resolve("data/twitch-chat.sqlite");
        tokenCipher = new TokenCipher(TokenCipher.generateKeyset());
        repository = new SQLiteChannelAuthorizationRepository(databasePath, tokenCipher);
    }

    @Test
    void persistsSeparateChannelsAndUpdatesByBroadcasterId() {
        Instant firstUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(authorization("1001", "first_login", "first-access", "first-refresh", firstUpdatedAt));
        repository.save(authorization("2002", "second_login", "second-access", "second-refresh", firstUpdatedAt));

        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        repository.save(authorization("1001", "renamed_login", "rotated-access", "rotated-refresh", updatedAt));

        assertEquals("renamed_login", repository.findByBroadcasterId("1001").orElseThrow().login());
        assertEquals("rotated-access", repository.findByBroadcasterId("1001").orElseThrow().accessToken());
        assertEquals("rotated-refresh", repository.findByBroadcasterId("1001").orElseThrow().refreshToken());
        assertEquals(updatedAt, repository.findByBroadcasterId("1001").orElseThrow().updatedAt());
        assertEquals("second_login", repository.findByBroadcasterId("2002").orElseThrow().login());
        assertTrue(repository.findByBroadcasterId("3003").isEmpty());
    }

    @Test
    void persistsOptionalDatesScopesAndStatus() {
        ChannelAuthorization value = new ChannelAuthorization(
                "1001", "streamer", "access", "refresh", List.of("chat:read", "chat:edit"),
                null, ChannelAuthorization.Status.NEEDS_REAUTHORIZATION, null,
                Instant.parse("2026-01-03T00:00:00Z"));

        repository.save(value);

        assertEquals(value, repository.findByBroadcasterId("1001").orElseThrow());
    }

    @Test
    void storesTokensEncryptedInTheDatabaseFile() throws Exception {
        repository.save(authorization("1001", "streamer", "disk-access-secret", "disk-refresh-secret", Instant.parse("2026-01-04T00:00:00Z")));

        String fileContents = new String(Files.readAllBytes(databasePath), StandardCharsets.ISO_8859_1);
        assertFalse(fileContents.contains("disk-access-secret"));
        assertFalse(fileContents.contains("disk-refresh-secret"));
    }

    @Test
    void schemaInitializationIsRepeatableAndDataSurvivesRepositoryRecreation() {
        ChannelAuthorization expected = authorization("1001", "streamer", "access", "refresh", Instant.parse("2026-01-05T00:00:00Z"));
        repository.save(expected);

        SQLiteChannelAuthorizationRepository reopened = new SQLiteChannelAuthorizationRepository(databasePath, tokenCipher);

        assertEquals(expected, reopened.findByBroadcasterId("1001").orElseThrow());
    }

    @Test
    void restrictsDatabaseAndDirectoryPermissionsOnPosix() throws Exception {
        Assumptions.assumeTrue(databasePath.getFileSystem().supportedFileAttributeViews().contains("posix"));

        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(databasePath));
        assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(databasePath.getParent()));
    }

    @Test
    void missingKeysetFailsBeforePersistingCredentials() {
        SQLiteChannelAuthorizationRepository unconfigured = new SQLiteChannelAuthorizationRepository(
                tempDir.resolve("unconfigured.sqlite"), new TokenCipher(""));

        assertThrows(IllegalStateException.class,
                () -> unconfigured.save(authorization("1001", "streamer", "access", "refresh", Instant.parse("2026-01-06T00:00:00Z"))));
        assertTrue(unconfigured.findByBroadcasterId("1001").isEmpty());
    }

    private static ChannelAuthorization authorization(
            String broadcasterId, String login, String accessToken, String refreshToken, Instant updatedAt) {
        return new ChannelAuthorization(
                broadcasterId, login, accessToken, refreshToken, List.of("chat:read", "chat:edit"),
                Instant.parse("2026-02-01T00:00:00Z"), ChannelAuthorization.Status.AUTHORIZED,
                Instant.parse("2026-01-01T00:00:00Z"), updatedAt);
    }
}
