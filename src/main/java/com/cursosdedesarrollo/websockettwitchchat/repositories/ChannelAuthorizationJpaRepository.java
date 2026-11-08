package com.cursosdedesarrollo.websockettwitchchat.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ChannelAuthorizationJpaRepository
        extends JpaRepository<ChannelAuthorizationEntity, ChannelAuthorizationKey> {
}
