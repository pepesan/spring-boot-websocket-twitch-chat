package com.cursosdedesarrollo.websockettwitchchat.repositories;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.security.TokenCipher;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

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
        databasePath = tempDir.resolve("data/oauth-credentials.sqlite");
        tokenCipher = new TokenCipher(TokenCipher.generateKeyset());
        repository = new SQLiteChannelAuthorizationRepository(databasePath, tokenCipher);
    }

    @Test
    void persistsIndependentChannelsPerProviderAndUpdatesByStableIdentity() {
        Instant firstUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(authorization("twitch", "1001", "first_login", "first-access", "first-refresh", firstUpdatedAt));
        repository.save(authorization("youtube", "1001", "video_login", "video-access", "video-refresh", firstUpdatedAt));
        repository.save(authorization("twitch", "2002", "second_login", "second-access", "second-refresh", firstUpdatedAt));

        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        repository.save(authorization("twitch", "1001", "renamed_login", "rotated-access", "rotated-refresh", updatedAt));

        assertEquals("renamed_login", repository.findByProviderUserId("twitch", "1001").orElseThrow().login());
        assertEquals("rotated-access", repository.findByProviderUserId("twitch", "1001").orElseThrow().accessToken());
        assertEquals("rotated-refresh", repository.findByProviderUserId("twitch", "1001").orElseThrow().refreshToken());
        assertEquals(updatedAt, repository.findByProviderUserId("twitch", "1001").orElseThrow().updatedAt());
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

        assertEquals(value, repository.findByProviderUserId("twitch", "1001").orElseThrow());
    }

    @Test
    void storesTokensEncryptedInTheDatabaseFile() throws Exception {
        repository.save(authorization("twitch", "1001", "streamer", "disk-access-secret", "disk-refresh-secret",
                Instant.parse("2026-01-04T00:00:00Z")));

        String fileContents = new String(Files.readAllBytes(databasePath), StandardCharsets.ISO_8859_1);
        assertFalse(fileContents.contains("disk-access-secret"));
        assertFalse(fileContents.contains("disk-refresh-secret"));
    }

    @Test
    void schemaInitializationIsRepeatableAndDataSurvivesRepositoryRecreation() {
        ChannelAuthorization expected = authorization(
                "twitch", "1001", "streamer", "access", "refresh", Instant.parse("2026-01-05T00:00:00Z"));
        repository.save(expected);

        SQLiteChannelAuthorizationRepository reopened = new SQLiteChannelAuthorizationRepository(databasePath, tokenCipher);

        assertEquals(expected, reopened.findByProviderUserId("twitch", "1001").orElseThrow());
    }

    @Test
    void migratesVersionOneTwitchCredentialsWithoutDecryptingOrLosingTokens() throws Exception {
        Path legacyDatabase = tempDir.resolve("legacy.sqlite");
        ChannelAuthorization expected = authorization(
                "twitch", "legacy-user", "legacy_login", "legacy-access", "legacy-refresh",
                Instant.parse("2026-01-05T00:00:00Z"));
        String createLegacyTable = "CREATE TABLE channel_authorizations ("
                + "broadcaster_id TEXT PRIMARY KEY NOT NULL, login TEXT NOT NULL, access_token TEXT NOT NULL,"
                + "refresh_token TEXT NOT NULL, scopes TEXT NOT NULL, expires_at INTEGER NULL, status TEXT NOT NULL,"
                + "validated_at INTEGER NULL, updated_at INTEGER NOT NULL)";
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + legacyDatabase);
             Statement statement = connection.createStatement()) {
            statement.execute(createLegacyTable);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO channel_authorizations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, expected.providerUserId());
                insert.setString(2, expected.login());
                insert.setString(3, tokenCipher.encrypt(expected.accessToken()));
                insert.setString(4, tokenCipher.encrypt(expected.refreshToken()));
                insert.setString(5, String.join(" ", expected.scopes()));
                insert.setLong(6, expected.expiresAt().toEpochMilli());
                insert.setString(7, expected.status().name());
                insert.setLong(8, expected.validatedAt().toEpochMilli());
                insert.setLong(9, expected.updatedAt().toEpochMilli());
                insert.executeUpdate();
            }
            statement.execute("PRAGMA user_version=1");
        }

        SQLiteChannelAuthorizationRepository migrated =
                new SQLiteChannelAuthorizationRepository(legacyDatabase, tokenCipher);

        assertEquals(expected, migrated.findByProviderUserId("twitch", "legacy-user").orElseThrow());
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
                () -> unconfigured.save(authorization("twitch", "1001", "streamer", "access", "refresh",
                        Instant.parse("2026-01-06T00:00:00Z"))));
        assertTrue(unconfigured.findByProviderUserId("twitch", "1001").isEmpty());
    }

    private static ChannelAuthorization authorization(
            String provider, String providerUserId, String login, String accessToken, String refreshToken, Instant updatedAt) {
        return new ChannelAuthorization(
                provider, providerUserId, login, accessToken, refreshToken, List.of("chat:read", "chat:edit"),
                Instant.parse("2026-02-01T00:00:00Z"), ChannelAuthorization.Status.AUTHORIZED,
                Instant.parse("2026-01-01T00:00:00Z"), updatedAt);
    }
}
