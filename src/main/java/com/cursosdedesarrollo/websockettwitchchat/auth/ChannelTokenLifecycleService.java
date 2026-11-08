package com.cursosdedesarrollo.websockettwitchchat.auth;

import com.cursosdedesarrollo.websockettwitchchat.channels.ChannelConnector;
import com.cursosdedesarrollo.websockettwitchchat.channels.ChannelConnectorRegistry;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.repositories.SQLiteChannelAuthorizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ChannelTokenLifecycleService {
    private static final long VALIDATION_INTERVAL_MILLIS = 3_600_000L;
    private static final Logger logger = LoggerFactory.getLogger(ChannelTokenLifecycleService.class);

    private final SQLiteChannelAuthorizationRepository authorizationRepository;
    private final OAuthProviderRegistry providerRegistry;
    private final ChannelConnectorRegistry connectorRegistry;
    private final ChannelAuthorizationService authorizationService;

    public ChannelTokenLifecycleService(
            SQLiteChannelAuthorizationRepository authorizationRepository,
            OAuthProviderRegistry providerRegistry,
            ChannelConnectorRegistry connectorRegistry,
            ChannelAuthorizationService authorizationService) {
        this.authorizationRepository = authorizationRepository;
        this.providerRegistry = providerRegistry;
        this.connectorRegistry = connectorRegistry;
        this.authorizationService = authorizationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateAtStartup() {
        validateStoredAuthorizations();
    }

    @Scheduled(fixedDelay = VALIDATION_INTERVAL_MILLIS, initialDelay = VALIDATION_INTERVAL_MILLIS)
    public void validateHourly() {
        validateStoredAuthorizations();
    }

    public synchronized void validateStoredAuthorizations() {
        for (ChannelAuthorization authorization : authorizationRepository.findAll()) {
            if (authorization.status() != ChannelAuthorization.Status.AUTHORIZED) {
                continue;
            }
            validateOne(authorization);
        }
    }

    private void validateOne(ChannelAuthorization authorization) {
        OAuthProvider provider;
        try {
            provider = providerRegistry.require(authorization.providerId());
        } catch (IllegalArgumentException exception) {
            logger.warn("No OAuth adapter registered for provider {}", authorization.providerId());
            return;
        }

        ChannelConnector connector = connectorRegistry.find(authorization.providerId()).orElse(null);
        try {
            boolean wasConnected = connector != null && connector.isConnected(authorization);
            ChannelAuthorization validated = authorizationService.validateOrRefresh(provider, authorization);
            if (validated.status() == ChannelAuthorization.Status.NEEDS_REAUTHORIZATION) {
                if (wasConnected) {
                    connector.disconnect(authorization);
                }
                logger.warn("OAuth authorization for provider {} channel {} requires reauthorization",
                        authorization.providerId(), authorization.login());
                return;
            }

            boolean tokensChanged = !authorization.accessToken().equals(validated.accessToken())
                    || !authorization.refreshToken().equals(validated.refreshToken());
            if (connector == null) {
                return;
            }
            if (!wasConnected) {
                connector.connect(validated);
            } else if (tokensChanged) {
                connector.disconnect(authorization);
                connector.connect(validated);
            }
        } catch (Exception exception) {
            logger.error("OAuth validation for provider {} channel {} failed ({})",
                    authorization.providerId(), authorization.login(), exception.getClass().getSimpleName());
        }
    }
}
