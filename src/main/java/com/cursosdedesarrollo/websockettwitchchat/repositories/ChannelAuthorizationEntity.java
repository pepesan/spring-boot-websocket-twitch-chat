package com.cursosdedesarrollo.websockettwitchchat.repositories;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "channel_authorizations")
public class ChannelAuthorizationEntity {
    @EmbeddedId
    private ChannelAuthorizationKey id;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "access_token", nullable = false)
    private String encryptedAccessToken;

    @Column(name = "refresh_token", nullable = false)
    private String encryptedRefreshToken;

    @Column(name = "scopes", nullable = false)
    private String scopes;

    @Column(name = "expires_at")
    private Long expiresAtMillis;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "validated_at")
    private Long validatedAtMillis;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAtMillis;

    protected ChannelAuthorizationEntity() {
    }

    public ChannelAuthorizationEntity(
            ChannelAuthorizationKey id,
            String login,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String scopes,
            Long expiresAtMillis,
            String status,
            Long validatedAtMillis,
            Long updatedAtMillis) {
        this.id = id;
        this.login = login;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.scopes = scopes;
        this.expiresAtMillis = expiresAtMillis;
        this.status = status;
        this.validatedAtMillis = validatedAtMillis;
        this.updatedAtMillis = updatedAtMillis;
    }

    public ChannelAuthorizationKey getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public String getScopes() {
        return scopes;
    }

    public Long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public String getStatus() {
        return status;
    }

    public Long getValidatedAtMillis() {
        return validatedAtMillis;
    }

    public Long getUpdatedAtMillis() {
        return updatedAtMillis;
    }
}
