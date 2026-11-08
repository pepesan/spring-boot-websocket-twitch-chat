package com.cursosdedesarrollo.websockettwitchchat.services;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TwitchAuthServiceTest {
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String AUTHORIZATION_CODE = "code with + and &";
    private static final String ACCESS_TOKEN = "synthetic-access-token";
    private static final String REFRESH_TOKEN = "synthetic-refresh-token";

    private MockRestServiceServer server;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger serviceLogger;
    private TwitchAuthService service;

    @BeforeEach
    void setUp() {
        TwitchConfig config = mock(TwitchConfig.class);
        when(config.getClientId()).thenReturn("test-client-id");
        when(config.getClientSecret()).thenReturn(CLIENT_SECRET);
        when(config.getRedirectUri()).thenReturn("http://localhost:8080/callback");

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new TwitchAuthService(config, restTemplate);

        serviceLogger = (Logger) LoggerFactory.getLogger(TwitchAuthService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void exchangesAuthorizationCodeAsEncodedFormAndReturnsAllTokenFields() {
        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("client_id", "test-client-id");
        expectedForm.add("client_secret", CLIENT_SECRET);
        expectedForm.add("code", AUTHORIZATION_CODE);
        expectedForm.add("grant_type", "authorization_code");
        expectedForm.add("redirect_uri", "http://localhost:8080/callback");
        server.expect(requestTo("https://id.twitch.tv/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("""
                        {
                          "access_token":"%s",
                          "refresh_token":"%s",
                          "expires_in":3600,
                          "scope":["chat:read","chat:edit"],
                          "token_type":"bearer"
                        }
                        """.formatted(ACCESS_TOKEN, REFRESH_TOKEN), MediaType.APPLICATION_JSON));

        TwitchAuthService.TokenResponse response = service.exchangeAuthorizationCode(AUTHORIZATION_CODE);

        assertEquals(ACCESS_TOKEN, response.accessToken());
        assertEquals(REFRESH_TOKEN, response.refreshToken());
        assertEquals(3600, response.expiresIn());
        assertEquals(List.of("chat:read", "chat:edit"), response.scopes());
        assertEquals("bearer", response.tokenType());
        server.verify();
        assertNoSensitiveValuesInLogs();
    }

    @Test
    void failedExchangeDoesNotLogRequestOrResponseSecrets() {
        server.expect(requestTo("https://id.twitch.tv/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"access_token\":\"response-secret\",\"message\":\"invalid code\"}"));

        assertThrows(IllegalStateException.class, () -> service.exchangeAuthorizationCode(AUTHORIZATION_CODE));

        server.verify();
        assertNoSensitiveValuesInLogs();
        assertTrue(logAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("401")));
    }

    private void assertNoSensitiveValuesInLogs() {
        String logs = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertTrue(!logs.contains(CLIENT_SECRET));
        assertTrue(!logs.contains(AUTHORIZATION_CODE));
        assertTrue(!logs.contains(ACCESS_TOKEN));
        assertTrue(!logs.contains(REFRESH_TOKEN));
        assertTrue(!logs.contains("response-secret"));
    }
}
