package com.cursosdedesarrollo.websockettwitchchat;

import com.cursosdedesarrollo.websockettwitchchat.security.TokenCipher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Assumptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(properties = {
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=0",
        "spring.flyway.clean-disabled=true"
})
class WebsocketTwitchChatApplicationTests {
    private static final Path DATABASE_PATH = createDatabasePath();

    @DynamicPropertySource
    static void configureSqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH);
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("twitch.credentials.encryption-keyset", TokenCipher::generateKeyset);
    }

    @AfterAll
    static void removeTemporaryDatabase() throws IOException {
        Files.deleteIfExists(DATABASE_PATH);
        Files.deleteIfExists(DATABASE_PATH.getParent());
    }

    @Test
    void applicationStartsWithMigratedSqliteJpaSchemaAndProtectsItsDatabaseFile() throws IOException {
        assertTrue(Files.isRegularFile(DATABASE_PATH));
        Assumptions.assumeTrue(DATABASE_PATH.getFileSystem().supportedFileAttributeViews().contains("posix"));
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(DATABASE_PATH));
    }

    private static Path createDatabasePath() {
        try {
            return Files.createTempDirectory("twitch-app-sqlite-").resolve("application.sqlite");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
