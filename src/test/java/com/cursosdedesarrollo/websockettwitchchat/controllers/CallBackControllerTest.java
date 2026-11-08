package com.cursosdedesarrollo.websockettwitchchat.controllers;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.auth.ChannelAuthorizationService;
import com.cursosdedesarrollo.websockettwitchchat.channels.ChannelConnector;
import com.cursosdedesarrollo.websockettwitchchat.channels.ChannelConnectorRegistry;
import com.cursosdedesarrollo.websockettwitchchat.auth.OAuthProvider;
import com.cursosdedesarrollo.websockettwitchchat.auth.OAuthProviderRegistry;
import com.cursosdedesarrollo.websockettwitchchat.auth.OAuthTokenResponse;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallBackControllerTest {
    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");

    private OAuthProvider provider;
    private ChannelAuthorizationService authorizationService;
    private ChannelConnector connector;
    private CallBackController controller;

    @BeforeEach
    void setUp() {
        provider = mock(OAuthProvider.class);
        when(provider.providerId()).thenReturn("twitch");
        when(provider.authorizationUri(anyString())).thenAnswer(invocation -> URI.create(
                "https://provider.example/authorize?client_id=test-client&state=" + invocation.getArgument(0)));
        authorizationService = mock(ChannelAuthorizationService.class);
        connector = mock(ChannelConnector.class);
        when(connector.providerId()).thenReturn("twitch");
        controller = new CallBackController(
                new OAuthProviderRegistry(List.of(provider)),
                authorizationService,
                new ChannelConnectorRegistry(List.of(connector)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void authorizationStartDelegatesUrlAndStoresFreshProviderScopedSessionState() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.startAuthorization("twitch", request, response);

        String firstState = (String) request.getSession().getAttribute(CallBackController.stateSessionAttribute("twitch"));
        assertEquals("test-client", queryParameter(response.getRedirectedUrl(), "client_id"));
        assertEquals(firstState, queryParameter(response.getRedirectedUrl(), "state"));
        assertNotNull(firstState);
        assertEquals(43, firstState.length());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        controller.startAuthorization("twitch", request, secondResponse);
        String secondState = (String) request.getSession().getAttribute(CallBackController.stateSessionAttribute("twitch"));
        assertNotEquals(firstState, secondState);
        assertEquals(secondState, queryParameter(secondResponse.getRedirectedUrl(), "state"));
    }

    @Test
    void authorizationStatesAreIsolatedPerProvider() throws IOException {
        OAuthProvider secondProvider = mock(OAuthProvider.class);
        when(secondProvider.providerId()).thenReturn("youtube");
        when(secondProvider.authorizationUri(anyString())).thenAnswer(invocation -> URI.create(
                "https://youtube.example/authorize?state=" + invocation.getArgument(0)));
        ChannelConnector secondConnector = mock(ChannelConnector.class);
        when(secondConnector.providerId()).thenReturn("youtube");
        CallBackController multiProviderController = new CallBackController(
                new OAuthProviderRegistry(List.of(provider, secondProvider)), authorizationService,
                new ChannelConnectorRegistry(List.of(connector, secondConnector)), Clock.fixed(NOW, ZoneOffset.UTC));
        MockHttpServletRequest request = new MockHttpServletRequest();

        multiProviderController.startAuthorization("twitch", request, new MockHttpServletResponse());
        multiProviderController.startAuthorization("youtube", request, new MockHttpServletResponse());

        assertNotNull(request.getSession().getAttribute(CallBackController.stateSessionAttribute("twitch")));
        assertNotNull(request.getSession().getAttribute(CallBackController.stateSessionAttribute("youtube")));
        assertNotEquals(CallBackController.stateSessionAttribute("twitch"),
                CallBackController.stateSessionAttribute("youtube"));
    }

    @Test
    void callbackValidatesAndConsumesStateThenConnectsPersistedProviderAuthorization() {
        MockHttpServletRequest request = requestWithState("valid-state", NOW.toEpochMilli());
        OAuthTokenResponse token = tokenResponse();
        ChannelAuthorization authorization = channelAuthorization();
        when(provider.exchangeAuthorizationCode("single-use-code")).thenReturn(token);
        when(authorizationService.authorize(provider, token)).thenReturn(authorization);

        ResponseEntity<String> response = controller.handleCallback(
                "twitch", "single-use-code", "valid-state", null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(provider).exchangeAuthorizationCode("single-use-code");
        verify(authorizationService).authorize(provider, token);
        verify(connector).connect(authorization);
        assertNull(request.getSession().getAttribute(CallBackController.stateSessionAttribute("twitch")));

        ResponseEntity<String> replay = controller.handleCallback(
                "twitch", "single-use-code", "valid-state", null, request);
        assertEquals(HttpStatus.BAD_REQUEST, replay.getStatusCode());
        verify(provider, org.mockito.Mockito.times(1)).exchangeAuthorizationCode("single-use-code");
    }

    @Test
    void callbackRejectsIncorrectOrExpiredStateBeforeExchangingCode() {
        MockHttpServletRequest request = requestWithState("valid-state", NOW.toEpochMilli());
        ResponseEntity<String> wrongState = controller.handleCallback("twitch", "code", "wrong-state", null, request);
        assertEquals(HttpStatus.BAD_REQUEST, wrongState.getStatusCode());

        MockHttpServletRequest expiredRequest = requestWithState("expired-state", NOW.minusSeconds(601).toEpochMilli());
        ResponseEntity<String> expiredState = controller.handleCallback(
                "twitch", "code", "expired-state", null, expiredRequest);
        assertEquals(HttpStatus.BAD_REQUEST, expiredState.getStatusCode());

        verify(provider, never()).exchangeAuthorizationCode(anyString());
        verify(authorizationService, never()).authorize(provider, tokenResponse());
        verify(connector, never()).connect(any());
    }

    @Test
    void callbackHandlesConsentDenialWithoutExchangingCode() {
        MockHttpServletRequest request = requestWithState("valid-state", NOW.toEpochMilli());

        ResponseEntity<String> response = controller.handleCallback(
                "twitch", null, "valid-state", "access_denied", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().contains("access_denied"));
        verify(provider, never()).exchangeAuthorizationCode(anyString());
        verify(authorizationService, never()).authorize(provider, tokenResponse());
        verify(connector, never()).connect(any());
    }

    @Test
    void callbackRejectsUnknownProviderWithoutConsumingAnotherProvidersState() {
        MockHttpServletRequest request = requestWithState("valid-state", NOW.toEpochMilli());

        ResponseEntity<String> response = controller.handleCallback(
                "unknown", "code", "valid-state", null, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(request.getSession().getAttribute(CallBackController.stateSessionAttribute("twitch")));
        verify(provider, never()).exchangeAuthorizationCode(anyString());
        verify(authorizationService, never()).authorize(provider, tokenResponse());
        verify(connector, never()).connect(any());
    }

    private MockHttpServletRequest requestWithState(String state, long createdAt) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = new MockHttpSession();
        session.setAttribute(CallBackController.stateSessionAttribute("twitch"), state);
        session.setAttribute(CallBackController.stateCreatedAtSessionAttribute("twitch"), createdAt);
        request.setSession((MockHttpSession) session);
        return request;
    }

    private String queryParameter(String url, String name) {
        return UriComponentsBuilder.fromUri(URI.create(url)).build().getQueryParams().getFirst(name);
    }

    private OAuthTokenResponse tokenResponse() {
        return new OAuthTokenResponse(
                "synthetic-access-token",
                "synthetic-refresh-token",
                3600,
                List.of("chat.read", "chat.write"),
                "bearer");
    }

    private ChannelAuthorization channelAuthorization() {
        return new ChannelAuthorization(
                "twitch",
                "12345",
                "test-channel",
                "synthetic-access-token",
                "synthetic-refresh-token",
                List.of("chat.read", "chat.write"),
                NOW.plusSeconds(3600),
                ChannelAuthorization.Status.AUTHORIZED,
                NOW,
                NOW);
    }
}
