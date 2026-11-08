package com.cursosdedesarrollo.websockettwitchchat.repositories;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.services.TokenCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class SQLiteChannelAuthorizationRepository {
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = PosixFilePermissions.fromString("rw-------");

    private final Path databasePath;
    private final TokenCipher tokenCipher;

    @Autowired
    public SQLiteChannelAuthorizationRepository(
            @Value("${oauth.credentials.database:./data/oauth-credentials.sqlite}") String databasePath,
            TokenCipher tokenCipher) {
        this(Path.of(databasePath), tokenCipher);
    }

    public SQLiteChannelAuthorizationRepository(Path databasePath, TokenCipher tokenCipher) {
        this.databasePath = databasePath.toAbsolutePath().normalize();
        this.tokenCipher = tokenCipher;
        initializeDatabase();
    }

    public void save(ChannelAuthorization authorization) {
        String sql = """
                INSERT INTO channel_authorizations (
                    provider_id, provider_user_id, login, access_token, refresh_token, scopes,
                    expires_at, status, validated_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(provider_id, provider_user_id) DO UPDATE SET
                    login = excluded.login,
                    access_token = excluded.access_token,
                    refresh_token = excluded.refresh_token,
                    scopes = excluded.scopes,
                    expires_at = excluded.expires_at,
                    status = excluded.status,
                    validated_at = excluded.validated_at,
                    updated_at = excluded.updated_at
                """;
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, authorization.providerId());
            statement.setString(2, authorization.providerUserId());
            statement.setString(3, authorization.login());
            statement.setString(4, tokenCipher.encrypt(authorization.accessToken()));
            statement.setString(5, tokenCipher.encrypt(authorization.refreshToken()));
            statement.setString(6, String.join(" ", authorization.scopes()));
            setInstant(statement, 7, authorization.expiresAt());
            statement.setString(8, authorization.status().name());
            setInstant(statement, 9, authorization.validatedAt());
            setInstant(statement, 10, authorization.updatedAt());
            statement.executeUpdate();
            restrictFilePermissions();
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Could not persist provider channel authorization", exception);
        }
    }

    public Optional<ChannelAuthorization> findByProviderUserId(String providerId, String providerUserId) {
        String sql = """
                SELECT provider_id, provider_user_id, login, access_token, refresh_token, scopes,
                       expires_at, status, validated_at, updated_at
                FROM channel_authorizations WHERE provider_id = ? AND provider_user_id = ?
                """;
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, providerId);
            statement.setString(2, providerUserId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ChannelAuthorization(
                        result.getString("provider_id"),
                        result.getString("provider_user_id"),
                        result.getString("login"),
                        tokenCipher.decrypt(result.getString("access_token")),
                        tokenCipher.decrypt(result.getString("refresh_token")),
                        parseScopes(result.getString("scopes")),
                        readInstant(result, "expires_at"),
                        ChannelAuthorization.Status.valueOf(result.getString("status")),
                        readInstant(result, "validated_at"),
                        readInstant(result, "updated_at")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read provider channel authorization", exception);
        }
    }

    private void initializeDatabase() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
                restrictPermissions(parent, DIRECTORY_PERMISSIONS);
            }
            if (Files.notExists(databasePath)) {
                try {
                    Files.createFile(databasePath, PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS));
                } catch (UnsupportedOperationException exception) {
                    Files.createFile(databasePath);
                }
            }
            try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=DELETE");
                int schemaVersion;
                try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
                    schemaVersion = result.next() ? result.getInt(1) : 0;
                }
                if (schemaVersion > 2) {
                    throw new SQLException("Credential database schema is newer than this application");
                }
                if (schemaVersion == 0) {
                    createGenericSchema(statement);
                    statement.execute("PRAGMA user_version=2");
                } else if (schemaVersion == 1) {
                    statement.execute("ALTER TABLE channel_authorizations RENAME TO channel_authorizations_v1");
                    createGenericSchema(statement);
                    statement.execute("""
                            INSERT INTO channel_authorizations (
                                provider_id, provider_user_id, login, access_token, refresh_token, scopes,
                                expires_at, status, validated_at, updated_at
                            )
                            SELECT 'twitch', broadcaster_id, login, access_token, refresh_token, scopes,
                                   expires_at, status, validated_at, updated_at
                            FROM channel_authorizations_v1
                            """);
                    statement.execute("DROP TABLE channel_authorizations_v1");
                    statement.execute("PRAGMA user_version=2");
                }
            }
            restrictFilePermissions();
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Could not initialize provider credential database", exception);
        }
    }

    private void createGenericSchema(Statement statement) throws SQLException {
        statement.execute("CREATE TABLE channel_authorizations ("
                + "provider_id TEXT NOT NULL,"
                + "provider_user_id TEXT NOT NULL,"
                + "login TEXT NOT NULL,"
                + "access_token TEXT NOT NULL,"
                + "refresh_token TEXT NOT NULL,"
                + "scopes TEXT NOT NULL,"
                + "expires_at INTEGER NULL,"
                + "status TEXT NOT NULL,"
                + "validated_at INTEGER NULL,"
                + "updated_at INTEGER NOT NULL,"
                + "PRIMARY KEY(provider_id, provider_user_id))");
    }
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    private void restrictFilePermissions() throws IOException {
        restrictPermissions(databasePath, FILE_PERMISSIONS);
    }

    private static void restrictPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // POSIX permissions are unavailable on this platform; token encryption remains enabled.
        }
    }

    private static void setInstant(PreparedStatement statement, int index, Instant instant) throws SQLException {
        if (instant == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, instant.toEpochMilli());
        }
    }

    private static Instant readInstant(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : Instant.ofEpochMilli(value);
    }

    private static List<String> parseScopes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(" ")).filter(scope -> !scope.isBlank()).collect(Collectors.toList());
    }
}
