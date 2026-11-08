package com.cursosdedesarrollo.websockettwitchchat.repositories;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlywaySchemaMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyVersionOneCredentialsWithoutDecryptingTokens() throws Exception {
        Path database = tempDir.resolve("legacy-v1.sqlite");
        String jdbcUrl = "jdbc:sqlite:" + database;
        createLegacyVersionOneDatabase(jdbcUrl);

        migrate(jdbcUrl);

        try (Connection connection = DriverManager.getConnection(jdbcUrl); Statement statement = connection.createStatement()) {
            assertEquals(3, readSchemaVersion(statement));
            try (ResultSet result = statement.executeQuery(
                    "SELECT provider_id, provider_user_id, access_token, refresh_token FROM channel_authorizations")) {
                assertTrue(result.next());
                assertEquals("twitch", result.getString("provider_id"));
                assertEquals("legacy-user", result.getString("provider_user_id"));
                assertEquals("opaque-access-ciphertext", result.getString("access_token"));
                assertEquals("opaque-refresh-ciphertext", result.getString("refresh_token"));
            }
            assertTrue(tableExists(statement, "command"));
            assertTrue(tableExists(statement, "twitch_user"));
        }
    }

    @Test
    void migratesVersionTwoTimestampTypesWithoutChangingEncryptedTokens() throws Exception {
        Path database = tempDir.resolve("legacy-v2.sqlite");
        String jdbcUrl = "jdbc:sqlite:" + database;
        createLegacyVersionTwoDatabase(jdbcUrl);

        migrate(jdbcUrl);

        try (Connection connection = DriverManager.getConnection(jdbcUrl); Statement statement = connection.createStatement()) {
            assertEquals(3, readSchemaVersion(statement));
            boolean hasBigintExpiresAt = false;
            boolean hasCreatorAccountId = false;
            try (ResultSet columns = statement.executeQuery("PRAGMA table_info(channel_authorizations)")) {
                while (columns.next()) {
                    if ("expires_at".equals(columns.getString("name"))) {
                        hasBigintExpiresAt = "BIGINT".equalsIgnoreCase(columns.getString("type"));
                    }
                    if ("creator_account_id".equals(columns.getString("name"))) {
                        hasCreatorAccountId = true;
                    }
                }
            }
            assertTrue(hasBigintExpiresAt);
            assertTrue(hasCreatorAccountId);
            try (ResultSet result = statement.executeQuery(
                    "SELECT access_token, refresh_token, creator_account_id FROM channel_authorizations WHERE provider_user_id='existing-user'")) {
                assertTrue(result.next());
                assertEquals("still-encrypted-access", result.getString("access_token"));
                assertEquals("still-encrypted-refresh", result.getString("refresh_token"));
                assertNull(result.getObject("creator_account_id"));
            }
        }
    }

    @Test
    void createsJpaTablesForACompletelyNewDatabase() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("fresh.sqlite");

        migrate(jdbcUrl);

        try (Connection connection = DriverManager.getConnection(jdbcUrl); Statement statement = connection.createStatement()) {
            assertEquals(3, readSchemaVersion(statement));
            assertTrue(tableExists(statement, "channel_authorizations"));
            assertTrue(tableExists(statement, "command"));
            assertTrue(tableExists(statement, "twitch_user"));
            assertTrue(tableExists(statement, "creator_accounts"));
            assertTrue(tableExists(statement, "creator_account_tokens"));
        }
    }

    private static void migrate(String jdbcUrl) {
        Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .cleanDisabled(true)
                .load()
                .migrate();
    }

    private static void createLegacyVersionOneDatabase(String jdbcUrl) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE channel_authorizations ("
                    + "broadcaster_id TEXT PRIMARY KEY NOT NULL, login TEXT NOT NULL, access_token TEXT NOT NULL,"
                    + "refresh_token TEXT NOT NULL, scopes TEXT NOT NULL, expires_at INTEGER NULL, status TEXT NOT NULL,"
                    + "validated_at INTEGER NULL, updated_at INTEGER NOT NULL)");
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO channel_authorizations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, "legacy-user");
                insert.setString(2, "legacy_login");
                insert.setString(3, "opaque-access-ciphertext");
                insert.setString(4, "opaque-refresh-ciphertext");
                insert.setString(5, "chat:read chat:edit");
                insert.setLong(6, 1769904000000L);
                insert.setString(7, "AUTHORIZED");
                insert.setLong(8, 1767225600000L);
                insert.setLong(9, 1769904000000L);
                insert.executeUpdate();
            }
            statement.execute("PRAGMA user_version=1");
        }
    }

    private static void createLegacyVersionTwoDatabase(String jdbcUrl) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE channel_authorizations ("
                    + "provider_id TEXT NOT NULL, provider_user_id TEXT NOT NULL, login TEXT NOT NULL,"
                    + "access_token TEXT NOT NULL, refresh_token TEXT NOT NULL, scopes TEXT NOT NULL,"
                    + "expires_at INTEGER NULL, status TEXT NOT NULL, validated_at INTEGER NULL,"
                    + "updated_at INTEGER NOT NULL, PRIMARY KEY(provider_id, provider_user_id))");
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO channel_authorizations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, "twitch");
                insert.setString(2, "existing-user");
                insert.setString(3, "existing_login");
                insert.setString(4, "still-encrypted-access");
                insert.setString(5, "still-encrypted-refresh");
                insert.setString(6, "chat:read");
                insert.setLong(7, 1769904000000L);
                insert.setString(8, "AUTHORIZED");
                insert.setLong(9, 1767225600000L);
                insert.setLong(10, 1769904000000L);
                insert.executeUpdate();
            }
            statement.execute("PRAGMA user_version=2");
        }
    }

    private static int readSchemaVersion(Statement statement) throws Exception {
        try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet result = statement.executeQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='" + tableName + "'")) {
            return result.next();
        }
    }
}
