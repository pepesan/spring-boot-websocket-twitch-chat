package com.cursosdedesarrollo.websockettwitchchat.controllers;

import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.cursosdedesarrollo.websockettwitchchat.repositories.TwitchConfigRepository;
import com.cursosdedesarrollo.websockettwitchchat.services.TwitchAuthService;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchChatClient;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchHelixClient;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CallBackControllerTest {
    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
    private static final String CLIENT_ID = "test-client-id";
    private static final String REDIRECT_URI = "http://localhost:8080/callback";

    private TwitchConfig twitchConfig;
    private TwitchAuthService authService;
    private TwitchConfigRepository configRepository;
    private TwitchChatClient chatClient;
    private TwitchHelixClient helixClient;
    private CallBackController controller;

    @BeforeEach
    void setUp() {
        twitchConfig = mock(TwitchConfig.class);
        when(twitchConfig.getClientId()).thenReturn(CLIENT_ID);
        when(twitchConfig.getRedirectUri()).thenReturn(REDIRECT_URI);
        authService = mock(TwitchAuthService.class);
        configRepository = mock(TwitchConfigRepository.class);
        chatClient = mock(TwitchChatClient.class);
        helixClient = mock(TwitchHelixClient.class);
        controller = new CallBackController(
                twitchConfig,
                authService,
                configRepository,
                chatClient,
                helixClient,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void authorizationStartRedirectsWithConfiguredClientAndFreshSessionState() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        controller.startAuthorization(request, firstResponse);

        String firstState = queryParameter(firstResponse.getRedirectedUrl(), "state");
        assertNotNull(firstState);
        assertEquals(43, firstState.length());
        assertEquals(CLIENT_ID, queryParameter(firstResponse.getRedirectedUrl(), "client_id"));
        assertEquals(REDIRECT_URI, queryParameter(firstResponse.getRedirectedUrl(), "redirect_uri"));
        assertEquals("code", queryParameter(firstResponse.getRedirectedUrl(), "response_type"));
        assertEquals("chat:read%20chat:edit", queryParameter(firstResponse.getRedirectedUrl(), "scope"));
        assertEquals(firstState, request.getSession().getAttribute(CallBackController.STATE_SESSION_ATTRIBUTE));

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        controller.startAuthorization(request, secondResponse);
        String secondState = queryParameter(secondResponse.getRedirectedUrl(), "state");
        assertNotEquals(firstState, secondState);
    }

    @Test
    void callbackExchangesCodeOnlyAfterValidStateAndConsumesState() {
        MockHttpServletRequest request = requestWithState("valid-state", NOW.toEpochMilli());
        when(authService.exchangeAuthorizationCode("single-use-code")).thenReturn(tokenResponse());

        ResponseEntity<String> response = controller.handleCallback("single-use-code", "valid-state", null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).exchangeAuthorizationCode("single-use-code");
        verify(twitchConfig).setOauthToken("synthetic-access-token");
        verify(twitchConfig).setRefreshToken("synthetic-refresh-token");
        verify(configRepository).save(twitchConfig);
        verify(helixClient).connect();
        verify(chatClient).connect();
        verify(chatClient).connectAndJoinChannel();
        verify(chatClient, never()).sendMessage("Bot activado!");
        assertNull(request.getSession().getAttribute(CallBackController.STATE_SESSION_ATTRIBUTE));

        ResponseEntity<String> replay = controller.handleCallback("single-use-code", "valid-state", null, request);
        assertEquals(HttpStatus.BAD_REQUEST, replay.getStatusCode());
        verify(authService, org.mockito.Mockito.times(1)).exchangeAuthorizationCode("single-use-code");
    }

    @Test
    void callbackRejectsIncorrectOrExpiredStateBeforeExchangingCode() {
        MockHttpServletRequest wrongStateRequest = requestWithState("expected", NOW.toEpochMilli());
        ResponseEntity<String> wrongState = controller.handleCallback("code", "attacker-state", null, wrongStateRequest);
        assertEquals(HttpStatus.BAD_REQUEST, wrongState.getStatusCode());

        MockHttpServletRequest expiredStateRequest = requestWithState("expired", NOW.minusSeconds(601).toEpochMilli());
        ResponseEntity<String> expiredState = controller.handleCallback("code", "expired", null, expiredStateRequest);
        assertEquals(HttpStatus.BAD_REQUEST, expiredState.getStatusCode());

        verifyNoInteractions(authService, configRepository, chatClient, helixClient);
    }

    @Test
    void callbackHandlesConsentDenialWithoutExchangingCode() {
        MockHttpServletRequest request = requestWithState("valid-state", NOW.toEpochMilli());

        ResponseEntity<String> response = controller.handleCallback(null, "valid-state", "access_denied", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().contains("access_denied"));
        verifyNoInteractions(authService, configRepository, chatClient, helixClient);
    }

    private MockHttpServletRequest requestWithState(String state, long createdAt) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = new MockHttpSession();
        session.setAttribute(CallBackController.STATE_SESSION_ATTRIBUTE, state);
        session.setAttribute(CallBackController.STATE_CREATED_AT_SESSION_ATTRIBUTE, createdAt);
        request.setSession((MockHttpSession) session);
        return request;
    }

    private String queryParameter(String url, String name) {
        return UriComponentsBuilder.fromUri(URI.create(url)).build().getQueryParams().getFirst(name);
    }

    private TwitchAuthService.TokenResponse tokenResponse() {
        return new TwitchAuthService.TokenResponse(
                "synthetic-access-token",
                "synthetic-refresh-token",
                3600,
                List.of("chat:read", "chat:edit"),
                "bearer");
    }
}
