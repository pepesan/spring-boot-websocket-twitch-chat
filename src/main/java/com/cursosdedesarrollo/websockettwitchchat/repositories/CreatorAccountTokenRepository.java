package com.cursosdedesarrollo.websockettwitchchat.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface CreatorAccountTokenRepository extends JpaRepository<CreatorAccountTokenEntity, Integer> {
    Optional<CreatorAccountTokenEntity> findByTokenHashAndPurpose(String tokenHash, String purpose);
    @Modifying @Query("delete from CreatorAccountTokenEntity t where t.accountId = :accountId and t.purpose = :purpose")
    void deleteForAccountAndPurpose(Integer accountId, String purpose);
    @Modifying @Query("delete from CreatorAccountTokenEntity t where t.id = :id")
    void deleteToken(Integer id);
}
