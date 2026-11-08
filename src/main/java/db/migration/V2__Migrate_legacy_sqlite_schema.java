package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Converts pre-Flyway credential tables without decrypting or rewriting token values. */
public class V2__Migrate_legacy_sqlite_schema extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            int schemaVersion = readSchemaVersion(statement);
            if (schemaVersion > 3) {
                throw new SQLException("SQLite user_version is newer than this application");
            }

            boolean authorizationTableExists = tableExists(statement, "channel_authorizations");
            if (schemaVersion == 1 || (schemaVersion == 0 && authorizationTableExists
                    && hasColumn(statement, "broadcaster_id"))) {
                migrateVersionOne(statement);
            } else if (schemaVersion == 2 || (schemaVersion == 0 && authorizationTableExists
                    && hasColumn(statement, "provider_id"))) {
                migrateVersionTwo(statement);
            } else if (schemaVersion == 0 && !authorizationTableExists) {
                createAuthorizationTable(statement);
            } else if (schemaVersion != 3) {
                throw new SQLException("Unrecognized SQLite credential schema; refusing to alter unknown data");
            }

            statement.execute("PRAGMA user_version=3");
        }
    }

    private static void migrateVersionOne(Statement statement) throws SQLException {
        statement.execute("ALTER TABLE channel_authorizations RENAME TO channel_authorizations_legacy");
        createAuthorizationTable(statement);
        statement.execute("""
                INSERT INTO channel_authorizations (
                    provider_id, provider_user_id, login, access_token, refresh_token, scopes,
                    expires_at, status, validated_at, updated_at
                )
                SELECT 'twitch', broadcaster_id, login, access_token, refresh_token, scopes,
                       expires_at, status, validated_at, updated_at
                FROM channel_authorizations_legacy
                """);
        statement.execute("DROP TABLE channel_authorizations_legacy");
    }

    private static void migrateVersionTwo(Statement statement) throws SQLException {
        statement.execute("ALTER TABLE channel_authorizations RENAME TO channel_authorizations_legacy");
        createAuthorizationTable(statement);
        statement.execute("""
                INSERT INTO channel_authorizations (
                    provider_id, provider_user_id, login, access_token, refresh_token, scopes,
                    expires_at, status, validated_at, updated_at
                )
                SELECT provider_id, provider_user_id, login, access_token, refresh_token, scopes,
                       expires_at, status, validated_at, updated_at
                FROM channel_authorizations_legacy
                """);
        statement.execute("DROP TABLE channel_authorizations_legacy");
    }

    private static void createAuthorizationTable(Statement statement) throws SQLException {
        statement.execute("CREATE TABLE channel_authorizations ("
                + "provider_id TEXT NOT NULL,"
                + "provider_user_id TEXT NOT NULL,"
                + "login TEXT NOT NULL,"
                + "access_token TEXT NOT NULL,"
                + "refresh_token TEXT NOT NULL,"
                + "scopes TEXT NOT NULL,"
                + "expires_at BIGINT NULL,"
                + "status TEXT NOT NULL,"
                + "validated_at BIGINT NULL,"
                + "updated_at BIGINT NOT NULL,"
                + "PRIMARY KEY(provider_id, provider_user_id))");
    }

    private static int readSchemaVersion(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static boolean tableExists(Statement statement, String tableName) throws SQLException {
        try (ResultSet result = statement.executeQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='" + tableName + "'")) {
            return result.next();
        }
    }

    private static boolean hasColumn(Statement statement, String columnName) throws SQLException {
        try (ResultSet result = statement.executeQuery("PRAGMA table_info(channel_authorizations)")) {
            while (result.next()) {
                if (columnName.equals(result.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
