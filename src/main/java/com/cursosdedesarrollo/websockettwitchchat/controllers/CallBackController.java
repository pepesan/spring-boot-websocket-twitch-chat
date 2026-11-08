package com.cursosdedesarrollo.websockettwitchchat.controllers;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.services.ChannelAuthorizationService;
import com.cursosdedesarrollo.websockettwitchchat.services.ChannelConnector;
import com.cursosdedesarrollo.websockettwitchchat.services.ChannelConnectorRegistry;
import com.cursosdedesarrollo.websockettwitchchat.services.OAuthProvider;
import com.cursosdedesarrollo.websockettwitchchat.services.OAuthProviderRegistry;
import com.cursosdedesarrollo.websockettwitchchat.services.OAuthTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

@Controller
@RequestMapping("/callback")
public class CallBackController {
    private static final String STATE_SESSION_ATTRIBUTE_PREFIX = "oauthState.";
    private static final String STATE_CREATED_AT_SESSION_ATTRIBUTE_PREFIX = "oauthStateCreatedAt.";
    private static final Duration STATE_LIFETIME = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger logger = LoggerFactory.getLogger(CallBackController.class);

    private final OAuthProviderRegistry providerRegistry;
    private final ChannelAuthorizationService authorizationService;
    private final ChannelConnectorRegistry connectorRegistry;
    private final Clock clock;

    @Autowired
    public CallBackController(
            OAuthProviderRegistry providerRegistry,
            ChannelAuthorizationService authorizationService,
            ChannelConnectorRegistry connectorRegistry) {
        this(providerRegistry, authorizationService, connectorRegistry, Clock.systemUTC());
    }

    CallBackController(
            OAuthProviderRegistry providerRegistry,
            ChannelAuthorizationService authorizationService,
            ChannelConnectorRegistry connectorRegistry,
            Clock clock) {
        this.providerRegistry = providerRegistry;
        this.authorizationService = authorizationService;
        this.connectorRegistry = connectorRegistry;
        this.clock = clock;
    }

    @GetMapping("/{providerId}/start")
    public void startAuthorization(
            @PathVariable String providerId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        OAuthProvider provider = providerRegistry.require(providerId);
        connectorRegistry.require(providerId);

        byte[] stateBytes = new byte[32];
        SECURE_RANDOM.nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
        HttpSession session = request.getSession(true);
        session.setAttribute(stateSessionAttribute(providerId), state);
        session.setAttribute(stateCreatedAtSessionAttribute(providerId), clock.millis());
        response.sendRedirect(provider.authorizationUri(state).toString());
    }

    @GetMapping("/{providerId}")
    @ResponseBody
    public ResponseEntity<String> handleCallback(
            @PathVariable String providerId,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            HttpServletRequest request) {
        OAuthProvider provider;
        ChannelConnector connector;
        try {
            provider = providerRegistry.require(providerId);
            connector = connectorRegistry.require(providerId);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }

        HttpSession session = request.getSession(false);
        String stateAttribute = stateSessionAttribute(providerId);
        String createdAtAttribute = stateCreatedAtSessionAttribute(providerId);
        String expectedState = session == null ? null : (String) session.getAttribute(stateAttribute);
        Long stateCreatedAt = session == null ? null : (Long) session.getAttribute(createdAtAttribute);
        if (session != null) {
            session.removeAttribute(stateAttribute);
            session.removeAttribute(createdAtAttribute);
        }

        if (!isValidState(expectedState, state, stateCreatedAt)) {
            return ResponseEntity.badRequest().body("La solicitud de autorización no es válida o ha caducado.");
        }
        if (error != null && !error.isBlank()) {
            return ResponseEntity.badRequest().body("La autorización del proveedor fue cancelada.");
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("El proveedor no devolvió un código de autorización.");
        }

        try {
            OAuthTokenResponse token = provider.exchangeAuthorizationCode(code);
            ChannelAuthorization authorization = authorizationService.authorize(provider, token);
            connector.connect(authorization);
            return ResponseEntity.ok("Canal " + authorization.login() + " autorizado y conectado.");
        } catch (Exception exception) {
            logger.error("OAuth callback for provider {} failed ({})", providerId, exception.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("No se pudo completar la autorización. Reintenta el proceso.");
        }
    }

    static String stateSessionAttribute(String providerId) {
        return STATE_SESSION_ATTRIBUTE_PREFIX + providerId;
    }

    static String stateCreatedAtSessionAttribute(String providerId) {
        return STATE_CREATED_AT_SESSION_ATTRIBUTE_PREFIX + providerId;
    }

    private boolean isValidState(String expectedState, String receivedState, Long createdAt) {
        if (expectedState == null || receivedState == null || createdAt == null) {
            return false;
        }
        long age = clock.millis() - createdAt;
        if (age < 0 || age > STATE_LIFETIME.toMillis()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedState.getBytes(StandardCharsets.UTF_8),
                receivedState.getBytes(StandardCharsets.UTF_8));
    }
}
