package com.cursosdedesarrollo.websockettwitchchat.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CreatorAccountRepository extends JpaRepository<CreatorAccountEntity, Integer> {
    Optional<CreatorAccountEntity> findByEmail(String email);
}
