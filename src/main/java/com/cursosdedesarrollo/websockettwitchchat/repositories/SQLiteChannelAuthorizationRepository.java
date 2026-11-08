package com.cursosdedesarrollo.websockettwitchchat.repositories;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.cursosdedesarrollo.websockettwitchchat.security.TokenCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SQLiteChannelAuthorizationRepository {
    private final ChannelAuthorizationJpaRepository jpaRepository;
    private final TokenCipher tokenCipher;

    @Autowired
    public SQLiteChannelAuthorizationRepository(
            ChannelAuthorizationJpaRepository jpaRepository,
            TokenCipher tokenCipher) {
        this.jpaRepository = jpaRepository;
        this.tokenCipher = tokenCipher;
    }

    public void save(ChannelAuthorization authorization) {
        jpaRepository.save(new ChannelAuthorizationEntity(
                new ChannelAuthorizationKey(authorization.providerId(), authorization.providerUserId()),
                authorization.login(),
                tokenCipher.encrypt(authorization.accessToken()),
                tokenCipher.encrypt(authorization.refreshToken()),
                String.join(" ", authorization.scopes()),
                toEpochMillis(authorization.expiresAt()),
                authorization.status().name(),
                toEpochMillis(authorization.validatedAt()),
                authorization.updatedAt().toEpochMilli()));
    }

    public Optional<ChannelAuthorization> findByProviderUserId(String providerId, String providerUserId) {
        return jpaRepository.findById(new ChannelAuthorizationKey(providerId, providerUserId))
                .map(this::toDomain);
    }

    public List<ChannelAuthorization> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private ChannelAuthorization toDomain(ChannelAuthorizationEntity entity) {
        ChannelAuthorizationKey id = entity.getId();
        return new ChannelAuthorization(
                id.getProviderId(),
                id.getProviderUserId(),
                entity.getLogin(),
                tokenCipher.decrypt(entity.getEncryptedAccessToken()),
                tokenCipher.decrypt(entity.getEncryptedRefreshToken()),
                parseScopes(entity.getScopes()),
                fromEpochMillis(entity.getExpiresAtMillis()),
                ChannelAuthorization.Status.valueOf(entity.getStatus()),
                fromEpochMillis(entity.getValidatedAtMillis()),
                Instant.ofEpochMilli(entity.getUpdatedAtMillis()));
    }

    private static Long toEpochMillis(Instant value) {
        return value == null ? null : value.toEpochMilli();
    }

    private static Instant fromEpochMillis(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }

    private static List<String> parseScopes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(" "))
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toList());
    }
}
