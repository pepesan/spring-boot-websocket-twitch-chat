package com.cursosdedesarrollo.websockettwitchchat.auth;

public final class OAuthProviderException extends RuntimeException {
    private final String providerId;
    private final int statusCode;

    public OAuthProviderException(String providerId, int statusCode) {
        super("OAuth provider " + providerId + " returned HTTP status " + statusCode);
        this.providerId = providerId;
        this.statusCode = statusCode;
    }

    public String providerId() {
        return providerId;
    }

    public int statusCode() {
        return statusCode;
    }
}
