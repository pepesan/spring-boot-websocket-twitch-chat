package com.cursosdedesarrollo.websockettwitchchat.services;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ChannelConnectorRegistry {
    private final Map<String, ChannelConnector> connectors;

    public ChannelConnectorRegistry(List<ChannelConnector> connectors) {
        this.connectors = connectors.stream().collect(Collectors.toUnmodifiableMap(
                ChannelConnector::providerId,
                Function.identity(),
                (first, duplicate) -> {
                    throw new IllegalArgumentException("Duplicate channel connector ID: " + first.providerId());
                }));
    }

    public ChannelConnector require(String providerId) {
        ChannelConnector connector = connectors.get(providerId);
        if (connector == null) {
            throw new IllegalArgumentException("No channel connector for OAuth provider: " + providerId);
        }
        return connector;
    }
}
