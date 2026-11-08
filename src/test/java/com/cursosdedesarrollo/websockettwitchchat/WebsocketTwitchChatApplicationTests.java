package com.cursosdedesarrollo.websockettwitchchat;

import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.CreatorAccountRepository;
import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.security.TokenCipher;
import com.cursosdedesarrollo.websockettwitchchat.repositories.ChannelAuthorizationEntity;
import com.cursosdedesarrollo.websockettwitchchat.repositories.ChannelAuthorizationJpaRepository;
import com.cursosdedesarrollo.websockettwitchchat.repositories.ChannelAuthorizationKey;
import com.cursosdedesarrollo.websockettwitchchat.repositories.SQLiteChannelAuthorizationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=0",
        "spring.mail.host=localhost",
        "spring.mail.port=1025",
        "spring.flyway.clean-disabled=true",
        "frontend.public-url=http://localhost:5173"
})

class WebsocketTwitchChatApplicationTests {
    @Autowired
    private CreatorAccountRepository accounts;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SQLiteChannelAuthorizationRepository channelAuthorizations;
    @Autowired
    private ChannelAuthorizationJpaRepository channelAuthorizationRows;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void twitchAuthorizationRequiresAnApplicationAccount() throws Exception {
        mockMvc.perform(get("/callback/twitch/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedInCreatorCanStartTwitchAuthorizationAndGetsSessionBoundState() throws Exception {
        MockHttpSession session = creatorSession();

        mockMvc.perform(get("/account/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("creator@example.test"));
        mockMvc.perform(get("/callback/twitch/start").session(session))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("state=")));

        assertTrue(session.getAttribute("oauthState.twitch") instanceof String);
    }

    @Test
    void csrfTokenIsAvailableAndRegistrationRejectsMissingCsrf() throws Exception {
        mockMvc.perform(get("/account/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/account/register")
                        .contentType("application/json")
                        .content("{\"email\":\"creator@example.test\",\"password\":\"long secure password\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifiedAccountCanLogInAndReturnToFrontendAfterEmailVerification() throws Exception {
        String email = "verified-" + UUID.randomUUID() + "@example.test";
        String password = "local test password";
        CreatorAccountEntity account = new CreatorAccountEntity(
                email, passwordEncoder.encode(password), System.currentTimeMillis());
        account.setEmailVerified(true);
        accounts.saveAndFlush(account);

        MvcResult csrfResult = mockMvc.perform(get("/account/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, String> csrf = objectMapper.readValue(
                csrfResult.getResponse().getContentAsString(),
                new TypeReference<>() {});
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        assertNotNull(session);

        MvcResult loginResult = mockMvc.perform(post("/account/login")
                        .session(session)
                        .param("username", email)
                        .param("password", password)
                        .header(csrf.get("headerName"), csrf.get("token")))
                .andExpect(status().isFound())
                .andReturn();
        MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(authenticatedSession);
        mockMvc.perform(get("/account/session").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/account/verify-email").param("token", "not-a-valid-token"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location", "http://localhost:5173/?account=verification-failed"));
    }

    @Test
    void unverifiedCreatorCannotLogIn() throws Exception {
        String email = "unverified-" + UUID.randomUUID() + "@example.test";
        String password = "local test password";
        accounts.saveAndFlush(new CreatorAccountEntity(
                email, passwordEncoder.encode(password), System.currentTimeMillis()));

        MvcResult csrfResult = mockMvc.perform(get("/account/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, String> csrf = objectMapper.readValue(
                csrfResult.getResponse().getContentAsString(),
                new TypeReference<>() {});
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        assertNotNull(session);
        MvcResult loginResult = mockMvc.perform(post("/account/login")
                        .session(session)
                        .param("username", email)
                        .param("password", password)
                        .header(csrf.get("headerName"), csrf.get("token")))
                .andExpect(status().isFound())
                .andReturn();

        mockMvc.perform(get("/account/session")
                        .session((MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void persistsProviderAuthorizationUnderItsVerifiedCreatorAccount() {
        CreatorAccountEntity account = new CreatorAccountEntity(
                "owner-" + UUID.randomUUID() + "@example.test", "argon2-test-hash", System.currentTimeMillis());
        account.setEmailVerified(true);
        accounts.saveAndFlush(account);
        Instant now = Instant.parse("2026-11-08T12:00:00Z");
        String providerUserId = UUID.randomUUID().toString();
        ChannelAuthorization authorization = new ChannelAuthorization(
                "twitch", providerUserId, "test-channel", "synthetic-access-token", "synthetic-refresh-token",
                List.of("chat:read", "chat:edit"), now.plusSeconds(3600),
                ChannelAuthorization.Status.AUTHORIZED, now, now);

        channelAuthorizations.save(authorization, account.getId());
        channelAuthorizationRows.flush();

        ChannelAuthorizationEntity stored = channelAuthorizationRows
                .findById(new ChannelAuthorizationKey("twitch", providerUserId))
                .orElseThrow();
        assertEquals(account.getId(), stored.getCreatorAccountId());
    }

    private MockHttpSession creatorSession() {
        MockHttpSession session = new MockHttpSession();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                "creator@example.test",
                "not-a-password",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_CREATOR"))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    private static Path createDatabasePath() {
        try {
            return Files.createTempDirectory("twitch-app-sqlite-").resolve("application.sqlite");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
