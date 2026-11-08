package com.cursosdedesarrollo.websockettwitchchat.repositories;

import com.cursosdedesarrollo.websockettwitchchat.domain.Command;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SQLiteJpaPersistenceTest {
    private static final Path DATABASE_PATH = createDatabasePath();

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void configureSqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH);
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
    }

    @AfterAll
    static void removeTemporaryDatabase() throws IOException {
        Files.deleteIfExists(DATABASE_PATH);
        Files.deleteIfExists(DATABASE_PATH.getParent());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void persistsAndReloadsCommandThroughJpaOnFileBackedSqlite() {
        Command command = new Command();
        command.setId(null);
        command.setName("jpa-sqlite-probe");
        command.setOutputText("original");
        command.setOutputType("text");
        command.setOwnerOnly(false);
        command.setOnlyMods(false);

        Command saved = commandRepository.save(command);
        assertNotNull(saved.getId());

        try (EntityManager firstSession = entityManagerFactory.createEntityManager()) {
            firstSession.getTransaction().begin();
            Command loaded = firstSession.find(Command.class, saved.getId());
            assertEquals("original", loaded.getOutputText());
            loaded.setOutputText("updated");
            firstSession.getTransaction().commit();
        }

        try (EntityManager reopenedSession = entityManagerFactory.createEntityManager()) {
            reopenedSession.getTransaction().begin();
            assertEquals("updated", reopenedSession.find(Command.class, saved.getId()).getOutputText());
            reopenedSession.getTransaction().commit();
        }
    }

    private static Path createDatabasePath() {
        try {
            return Files.createTempDirectory("twitch-jpa-sqlite-").resolve("persistence.sqlite");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
