package com.cursosdedesarrollo.websockettwitchchat.controllers;

import com.cursosdedesarrollo.websockettwitchchat.domain.TwitchConfig;
import com.cursosdedesarrollo.websockettwitchchat.repositories.TwitchConfigRepository;
import com.cursosdedesarrollo.websockettwitchchat.services.TwitchAuthService;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchChatClient;
import com.cursosdedesarrollo.websockettwitchchat.twitch.TwitchHelixClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/callback")
public class CallBackController {
    static final String STATE_SESSION_ATTRIBUTE = "twitchOAuthState";
    static final String STATE_CREATED_AT_SESSION_ATTRIBUTE = "twitchOAuthStateCreatedAt";
    private static final String AUTHORIZE_ENDPOINT = "https://id.twitch.tv/oauth2/authorize";
    private static final Duration STATE_LIFETIME = Duration.ofMinutes(10);
    private static final List<String> CHAT_SCOPES = List.of("chat:read", "chat:edit");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger logger = LoggerFactory.getLogger(CallBackController.class);

    private final TwitchConfig twitchConfig;
    private final TwitchAuthService twitchService;
    private final TwitchConfigRepository twitchConfigRepository;
    private final TwitchChatClient twitchChatClient;
    private final TwitchHelixClient twitchHelixClient;
    private final Clock clock;

    @Autowired
    public CallBackController(
            TwitchConfig twitchConfig,
            TwitchAuthService twitchService,
            TwitchConfigRepository twitchConfigRepository,
            TwitchChatClient twitchChatClient,
            TwitchHelixClient twitchHelixClient) {
        this(twitchConfig, twitchService, twitchConfigRepository, twitchChatClient, twitchHelixClient, Clock.systemUTC());
    }

    CallBackController(
            TwitchConfig twitchConfig,
            TwitchAuthService twitchService,
            TwitchConfigRepository twitchConfigRepository,
            TwitchChatClient twitchChatClient,
            TwitchHelixClient twitchHelixClient,
            Clock clock) {
        this.twitchConfig = twitchConfig;
        this.twitchService = twitchService;
        this.twitchConfigRepository = twitchConfigRepository;
        this.twitchChatClient = twitchChatClient;
        this.twitchHelixClient = twitchHelixClient;
        this.clock = clock;
    }

    @GetMapping("/start")
    public void startAuthorization(HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] stateBytes = new byte[32];
        SECURE_RANDOM.nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);

        HttpSession session = request.getSession(true);
        session.setAttribute(STATE_SESSION_ATTRIBUTE, state);
        session.setAttribute(STATE_CREATED_AT_SESSION_ATTRIBUTE, clock.millis());

        String authorizationUrl = UriComponentsBuilder.fromUriString(AUTHORIZE_ENDPOINT)
                .queryParam("client_id", twitchConfig.getClientId())
                .queryParam("redirect_uri", twitchConfig.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", CHAT_SCOPES))
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        response.sendRedirect(authorizationUrl);
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<String> handleCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expectedState = session == null ? null : (String) session.getAttribute(STATE_SESSION_ATTRIBUTE);
        Long stateCreatedAt = session == null ? null : (Long) session.getAttribute(STATE_CREATED_AT_SESSION_ATTRIBUTE);
        if (session != null) {
            session.removeAttribute(STATE_SESSION_ATTRIBUTE);
            session.removeAttribute(STATE_CREATED_AT_SESSION_ATTRIBUTE);
        }

        if (!isValidState(expectedState, state, stateCreatedAt)) {
            return ResponseEntity.badRequest().body("La solicitud de autorización no es válida o ha caducado.");
        }
        if (error != null && !error.isBlank()) {
            return ResponseEntity.badRequest().body("La autorización de Twitch fue cancelada.");
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("Twitch no devolvió un código de autorización.");
        }

        try {
            TwitchAuthService.TokenResponse token = twitchService.exchangeAuthorizationCode(code);
            twitchConfig.setOauthToken(token.accessToken());
            twitchConfig.setRefreshToken(token.refreshToken());
            twitchConfigRepository.save(twitchConfig);
            twitchHelixClient.connect();
            twitchChatClient.connect();
            twitchChatClient.connectAndJoinChannel();
            return ResponseEntity.ok("Autorización completada. El cliente de chat está conectado.");
        } catch (Exception exception) {
            logger.error("Twitch authorization callback failed ({})", exception.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("No se pudo completar la autorización con Twitch. Reintenta el proceso.");
        }
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
