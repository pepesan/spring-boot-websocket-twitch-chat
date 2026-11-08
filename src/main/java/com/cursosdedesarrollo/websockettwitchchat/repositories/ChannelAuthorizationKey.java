package com.cursosdedesarrollo.websockettwitchchat.repositories;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ChannelAuthorizationKey implements Serializable {
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    protected ChannelAuthorizationKey() {
    }

    public ChannelAuthorizationKey(String providerId, String providerUserId) {
        this.providerId = Objects.requireNonNull(providerId);
        this.providerUserId = Objects.requireNonNull(providerUserId);
    }

    public String getProviderId() {
        return providerId;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChannelAuthorizationKey that)) {
            return false;
        }
        return Objects.equals(providerId, that.providerId)
                && Objects.equals(providerUserId, that.providerUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, providerUserId);
    }
}
