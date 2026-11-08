package com.cursosdedesarrollo.websockettwitchchat.repositories;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/** Restricts the SQLite file before Flyway or JPA opens it. */
@Component
public class SQLiteDatabaseFileSecurity implements FlywayConfigurationCustomizer {
    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = PosixFilePermissions.fromString("rw-------");

    private final String jdbcUrl;

    public SQLiteDatabaseFileSecurity(
            @Value("${spring.datasource.url:jdbc:sqlite:./data/oauth-credentials.sqlite}") String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Override
    public void customize(FluentConfiguration configuration) {
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) {
            throw new IllegalArgumentException("The application datasource must use SQLite");
        }
        String databaseName = jdbcUrl.substring("jdbc:sqlite:".length()).split("\\?", 2)[0];
        if (databaseName.isBlank() || databaseName.equals(":memory:") || databaseName.startsWith("file:")) {
            return;
        }

        Path databaseFile = Path.of(databaseName).toAbsolutePath().normalize();
        try {
            Path parent = databaseFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
                restrictPermissions(parent, DIRECTORY_PERMISSIONS);
            }
            if (Files.notExists(databaseFile)) {
                try {
                    Files.createFile(databaseFile, PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS));
                } catch (UnsupportedOperationException exception) {
                    Files.createFile(databaseFile);
                }
            }
            restrictPermissions(databaseFile, FILE_PERMISSIONS);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not secure SQLite database file", exception);
        }
    }

    private static void restrictPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // POSIX permissions are unavailable; tokens remain encrypted before persistence.
        }
    }
}
